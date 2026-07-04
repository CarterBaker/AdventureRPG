package engine.lwjgl3;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.input.Input;
import engine.root.EngineContext;
import engine.root.WindowPlatform;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

public class Lwjgl3WindowPlatform implements WindowPlatform {

    /*
     * Bridges the engine WindowPlatform contract to raw GLFW. Maps engine window
     * IDs to native handles. All windows — main and secondary — share identical
     * open, draw, swap, and destroy paths with no special casing.
     *
     * GL context switching and input context switching are intentionally
     * decoupled. bindContext only makes a GL context current and ensures its
     * GLCapabilities are loaded — it never touches EngineContext.input.
     * syncInputForCurrentContext is called exclusively from syncInputForWindow,
     * which WindowManager drives once per frame after resolving the hovered OS
     * window. This ensures that render-loop context switches (makeContextCurrent
     * on secondary windows) cannot corrupt the input context that was established
     * by syncHoveredWindow earlier in the same frame.
     */

    // Application
    private Lwjgl3Application application;

    // Window Registry
    private static final int MAIN_WINDOW_ID = 0;
    private static final int UNKNOWN_WINDOW_ID = -1;
    private final Int2LongOpenHashMap windowID2Handle = new Int2LongOpenHashMap();
    private final Int2ObjectOpenHashMap<GLCapabilities> windowID2Capabilities = new Int2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap handle2WindowID = new Long2IntOpenHashMap();

    // Scratch buffers — reused to avoid per-call allocation
    private final DoubleBuffer cursorScratchX = BufferUtils.createDoubleBuffer(1);
    private final DoubleBuffer cursorScratchY = BufferUtils.createDoubleBuffer(1);
    private final IntBuffer posScratchX = BufferUtils.createIntBuffer(1);
    private final IntBuffer posScratchY = BufferUtils.createIntBuffer(1);

    public Lwjgl3WindowPlatform() {
        windowID2Handle.defaultReturnValue(0L);
        handle2WindowID.defaultReturnValue(UNKNOWN_WINDOW_ID);
    }

    public void setApplication(Lwjgl3Application application) {
        this.application = application;
    }

    // Internal \\

    @Override
    public void openWindow(WindowInstance window) {

        int windowID = window.getWindowID();
        long handle = resolveOrCreateHandle(window);

        windowID2Handle.put(windowID, handle);
        handle2WindowID.put(handle, windowID);
        window.setNativeHandle(handle);

        primeWindowContext(windowID, handle);
        GLFW.glfwShowWindow(handle);

        syncWindowSize(window);
        syncScreenPosition(window);

        GLFW.glfwSetWindowPosCallback(handle, (h, x, y) -> {
            window.setScreenPosition(x, y);
        });
    }

    @Override
    public void destroyWindow(WindowInstance window) {

        long handle = windowID2Handle.remove(window.getWindowID());

        if (handle == 0L)
            return;

        handle2WindowID.remove(handle);
        application.removeSecondaryWindow(handle);
        GLFW.glfwDestroyWindow(handle);
        windowID2Capabilities.remove(window.getWindowID());
        window.setNativeHandle(0L);
    }

    @Override
    public boolean shouldClose(WindowInstance window) {

        if (!window.hasNativeHandle())
            return false;

        return GLFW.glfwWindowShouldClose(window.getNativeHandle());
    }

    @Override
    public boolean isWindowFocused(WindowInstance window) {

        if (!window.hasNativeHandle())
            return false;

        return GLFW.glfwGetWindowAttrib(window.getNativeHandle(), GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
    }

    @Override
    public void makeContextCurrent(WindowInstance window) {

        if (!window.hasNativeHandle())
            return;

        bindContext(window.getWindowID(), window.getNativeHandle());
    }

    @Override
    public void syncInputForWindow(WindowInstance window) {

        if (!window.hasNativeHandle())
            return;

        // Refreshes this window's own Input object from a direct platform query
        // so getMouseX/Y() is current even on windows that never received a
        // cursor-move callback. Does NOT touch EngineContext.input — ownership
        // of that reference belongs exclusively to InputManager, decided once
        // per frame after focus is resolved, never as a side effect of syncing
        // every window in a loop.
        cursorScratchX.clear();
        cursorScratchY.clear();
        GLFW.glfwGetCursorPos(window.getNativeHandle(), cursorScratchX, cursorScratchY);
        Lwjgl3Input windowInput = application.getLwjglInputForHandle(window.getNativeHandle());
        if (windowInput != null)
            windowInput.refreshCursor(cursorScratchX.get(0), cursorScratchY.get(0));
    }

    @Override
    public Input getInputForWindow(WindowInstance window) {
        if (!window.hasNativeHandle())
            return null;
        return application.getInputForHandle(window.getNativeHandle());
    }

    @Override
    public void swapBuffers(WindowInstance window) {

        if (!window.hasNativeHandle())
            return;

        GLFW.glfwSwapBuffers(window.getNativeHandle());
    }

    @Override
    public void restoreMainContext() {

        if (!(EngineContext.display instanceof Lwjgl3Display display))
            return;

        bindContext(MAIN_WINDOW_ID, display.getMainHandle());
    }

    @Override
    public void setCursorShape(long windowHandle, int shape) {
        Lwjgl3Input windowInput = application.getLwjglInputForHandle(windowHandle);
        if (windowInput == null)
            return;
        windowInput.setCursorShape(windowHandle, shape);
    }

    @Override
    public void exit() {

        long mainHandle = windowID2Handle.get(MAIN_WINDOW_ID);

        if (mainHandle != 0L)
            GLFW.glfwSetWindowShouldClose(mainHandle, true);
    }

    @Override
    public void syncWindowSize(WindowInstance window) {

        if (!window.hasNativeHandle())
            return;

        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        GLFW.glfwGetFramebufferSize(window.getNativeHandle(), w, h);

        int width = w.get(0);
        int height = h.get(0);

        if (width > 0 && height > 0)
            window.resize(width, height);
    }

    // Cursor position — window-local, no context switch \\

    /*
     * Returns the cursor X position relative to the given OS window's client
     * area. glfwGetCursorPos does not require the window to be the current
     * GL context, so this is safe to call for any native window at any time
     * without disturbing the input-sync state. Used by InputManager to serve
     * TabDragManager during cross-window drag resolution.
     */
    @Override
    public float getCursorX(WindowInstance window) {

        if (!window.hasNativeHandle())
            return 0f;

        cursorScratchX.clear();
        cursorScratchY.clear();
        GLFW.glfwGetCursorPos(window.getNativeHandle(), cursorScratchX, cursorScratchY);
        return (float) cursorScratchX.get(0);
    }

    @Override
    public float getCursorY(WindowInstance window) {

        if (!window.hasNativeHandle())
            return 0f;

        cursorScratchX.clear();
        cursorScratchY.clear();
        GLFW.glfwGetCursorPos(window.getNativeHandle(), cursorScratchX, cursorScratchY);
        return (float) cursorScratchY.get(0);
    }

    /*
     * Fills out[0]/out[1] with cursor X/Y in a single glfwGetCursorPos call.
     * Use this instead of getCursorX + getCursorY when both values are needed
     * together — avoids a redundant platform round-trip per window per frame.
     * Called from resolveHoveredOsWindow which queries every registered OS
     * window every frame.
     */
    @Override
    public void getCursorPos(WindowInstance window, float[] out) {

        if (!window.hasNativeHandle()) {
            out[0] = 0f;
            out[1] = 0f;
            return;
        }

        cursorScratchX.clear();
        cursorScratchY.clear();
        GLFW.glfwGetCursorPos(window.getNativeHandle(), cursorScratchX, cursorScratchY);
        out[0] = (float) cursorScratchX.get(0);
        out[1] = (float) cursorScratchY.get(0);
    }

    // Screen position — OS-level window origin \\

    /*
     * Returns the screen X/Y of the window's top-left corner as reported by
     * the OS. Written into WindowInstance.screenX/Y so TabDragManager can test
     * whether the global cursor falls within a specific OS window's bounds
     * during drag resolution. Also called from openWindow and the
     * glfwSetWindowPosCallback installed there.
     */
    @Override
    public float getScreenX(WindowInstance window) {

        if (!window.hasNativeHandle())
            return 0f;

        posScratchX.clear();
        posScratchY.clear();
        GLFW.glfwGetWindowPos(window.getNativeHandle(), posScratchX, posScratchY);
        return posScratchX.get(0);
    }

    @Override
    public float getScreenY(WindowInstance window) {

        if (!window.hasNativeHandle())
            return 0f;

        posScratchX.clear();
        posScratchY.clear();
        GLFW.glfwGetWindowPos(window.getNativeHandle(), posScratchX, posScratchY);
        return posScratchY.get(0);
    }

    // Internal \\

    /*
     * Queries glfwGetWindowPos and writes the result into WindowInstance so
     * the engine-side screen position stays in sync with the OS. Called from
     * openWindow after the handle is live, and automatically kept current by
     * the glfwSetWindowPosCallback installed in openWindow.
     */
    private void syncScreenPosition(WindowInstance window) {

        if (!window.hasNativeHandle())
            return;

        posScratchX.clear();
        posScratchY.clear();
        GLFW.glfwGetWindowPos(window.getNativeHandle(), posScratchX, posScratchY);
        window.setScreenPosition(posScratchX.get(0), posScratchY.get(0));
    }

    private long resolveOrCreateHandle(WindowInstance window) {

        long handle = windowID2Handle.get(window.getWindowID());

        if (handle != 0L)
            return handle;

        if (window.getWindowID() == MAIN_WINDOW_ID && EngineContext.display instanceof Lwjgl3Display display)
            return display.getMainHandle();

        return application.newWindow(window.getTitle(), window.getWidth(), window.getHeight());
    }

    private void primeWindowContext(int windowID, long windowHandle) {

        long previousContext = GLFW.glfwGetCurrentContext();
        GLCapabilities previousCapabilities = GL.getCapabilities();
        bindContext(windowID, windowHandle);
        GLFW.glfwSwapInterval(application.getSwapInterval());

        if (previousContext != windowHandle)
            restoreContext(previousContext, previousCapabilities);
    }

    private void restoreContext(long windowHandle, GLCapabilities previousCapabilities) {

        if (windowHandle == 0L) {
            GLFW.glfwMakeContextCurrent(0L);
            GL.setCapabilities(null);
            return;
        }

        int windowID = handle2WindowID.get(windowHandle);

        if (windowID == UNKNOWN_WINDOW_ID) {
            GLFW.glfwMakeContextCurrent(windowHandle);
            GL.setCapabilities(previousCapabilities);
            return;
        }

        bindContext(windowID, windowHandle);
    }

    /*
     * Makes the given GL context current and ensures its GLCapabilities are
     * loaded. Does NOT touch EngineContext.input — input context is owned
     * exclusively by syncInputForWindow and must not be reassigned as a side
     * effect of GL context switches during rendering or window initialisation.
     */
    private void bindContext(int windowID, long windowHandle) {
        GLFW.glfwMakeContextCurrent(windowHandle);
        ensureCapabilitiesForCurrentContext(windowID);
    }

    private void ensureCapabilitiesForCurrentContext(int windowID) {

        GLCapabilities caps = windowID2Capabilities.get(windowID);

        if (caps == null) {
            caps = GL.createCapabilities();
            windowID2Capabilities.put(windowID, caps);
        }

        GL.setCapabilities(caps);
    }

    private void syncInputForCurrentContext(long windowHandle) {

        Input windowInput = application.getInputForHandle(windowHandle);

        if (windowInput != null)
            EngineContext.input = windowInput;
    }
}