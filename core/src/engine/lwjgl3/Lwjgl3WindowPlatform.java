package engine.lwjgl3;

import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
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
     * Implements the WindowPlatform contract on GLFW for every window alike. GL
     * context binding is kept apart from input: only syncInputForWindow()
     * touches EngineContext.input. Display mode acts on the main window; vsync
     * is reapplied to every OS window.
     */

    // Application
    private Lwjgl3Application application;

    // Window Registry
    private static final int UNKNOWN_WINDOW_ID = -1;
    private final Int2LongOpenHashMap windowID2Handle = new Int2LongOpenHashMap();
    private final Int2ObjectOpenHashMap<GLCapabilities> windowID2Capabilities = new Int2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap handle2WindowID = new Long2IntOpenHashMap();

    // Scratch buffers — reused to avoid per-call allocation
    private final DoubleBuffer cursorScratchX = BufferUtils.createDoubleBuffer(1);
    private final DoubleBuffer cursorScratchY = BufferUtils.createDoubleBuffer(1);
    private final IntBuffer posScratchX = BufferUtils.createIntBuffer(1);
    private final IntBuffer posScratchY = BufferUtils.createIntBuffer(1);
    private final IntBuffer sizeScratchW = BufferUtils.createIntBuffer(1);
    private final IntBuffer sizeScratchH = BufferUtils.createIntBuffer(1);
    private final IntBuffer framebufferScratchW = BufferUtils.createIntBuffer(1);
    private final IntBuffer framebufferScratchH = BufferUtils.createIntBuffer(1);

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
            application.onWindowMoved(h);
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

        // Refreshes this window's own cursor without touching EngineContext.input
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

        bindContext(EngineSetting.MAIN_WINDOW_ID, display.getMainHandle());
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

        long mainHandle = windowID2Handle.get(EngineSetting.MAIN_WINDOW_ID);

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

    // Placement — OS-level window bounds \\

    @Override
    public void placeWindow(WindowInstance window, int screenX, int screenY, int width, int height) {

        if (!window.hasNativeHandle() || width <= 0 || height <= 0)
            return;

        long handle = window.getNativeHandle();

        sizeScratchW.clear();
        sizeScratchH.clear();
        framebufferScratchW.clear();
        framebufferScratchH.clear();
        GLFW.glfwGetWindowSize(handle, sizeScratchW, sizeScratchH);
        GLFW.glfwGetFramebufferSize(handle, framebufferScratchW, framebufferScratchH);

        int screenWidth = toScreenUnits(width, sizeScratchW.get(0), framebufferScratchW.get(0));
        int screenHeight = toScreenUnits(height, sizeScratchH.get(0), framebufferScratchH.get(0));

        GLFW.glfwSetWindowSize(handle, screenWidth, screenHeight);

        if (isOnAnyMonitor(screenX, screenY, screenWidth, screenHeight))
            GLFW.glfwSetWindowPos(handle, screenX, screenY);

        syncWindowSize(window);
        syncScreenPosition(window);
    }

    // Display Mode \\

    @Override
    public void setFullscreen(boolean fullscreen) {
        application.setFullscreen(fullscreen);
    }

    @Override
    public void setVsync(boolean vsync) {

        application.setSwapInterval(vsync ? 1 : 0);

        long previousContext = GLFW.glfwGetCurrentContext();
        GLCapabilities previousCapabilities = GL.getCapabilities();

        for (Int2LongMap.Entry entry : windowID2Handle.int2LongEntrySet()) {
            bindContext(entry.getIntKey(), entry.getLongValue());
            GLFW.glfwSwapInterval(application.getSwapInterval());
        }

        restoreContext(previousContext, previousCapabilities);
    }

    // Cursor position — window-local, no context switch \\

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

    private void syncScreenPosition(WindowInstance window) {

        if (!window.hasNativeHandle())
            return;

        posScratchX.clear();
        posScratchY.clear();
        GLFW.glfwGetWindowPos(window.getNativeHandle(), posScratchX, posScratchY);
        window.setScreenPosition(posScratchX.get(0), posScratchY.get(0));
    }

    private int toScreenUnits(int framebufferPixels, int windowSize, int framebufferSize) {

        if (windowSize <= 0 || framebufferSize <= 0)
            return framebufferPixels;

        return Math.max(1, Math.round(framebufferPixels * (float) windowSize / framebufferSize));
    }

    private boolean isOnAnyMonitor(int screenX, int screenY, int width, int height) {

        PointerBuffer monitors = GLFW.glfwGetMonitors();

        if (monitors == null)
            return false;

        for (int i = 0; i < monitors.limit(); i++) {

            posScratchX.clear();
            posScratchY.clear();
            sizeScratchW.clear();
            sizeScratchH.clear();
            GLFW.glfwGetMonitorWorkarea(monitors.get(i), posScratchX, posScratchY, sizeScratchW, sizeScratchH);

            int monitorX = posScratchX.get(0);
            int monitorY = posScratchY.get(0);

            if (screenX < monitorX + sizeScratchW.get(0)
                    && screenX + width > monitorX
                    && screenY < monitorY + sizeScratchH.get(0)
                    && screenY + height > monitorY)
                return true;
        }

        return false;
    }

    private long resolveOrCreateHandle(WindowInstance window) {

        long handle = windowID2Handle.get(window.getWindowID());

        if (handle != 0L)
            return handle;

        if (window.getWindowID() == EngineSetting.MAIN_WINDOW_ID
                && EngineContext.display instanceof Lwjgl3Display display)
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