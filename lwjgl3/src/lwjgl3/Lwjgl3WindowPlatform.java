package lwjgl3;

import program.core.app.CoreContext;
import program.core.backends.lwjgl3.Lwjgl3Application;
import program.core.backends.lwjgl3.Lwjgl3Graphics;
import program.core.backends.lwjgl3.Lwjgl3Window;
import program.core.backends.lwjgl3.Lwjgl3WindowConfiguration;
import program.core.engine.WindowPlatform;
import program.core.kernel.window.WindowInstance;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.IntBuffer;

public class Lwjgl3WindowPlatform implements WindowPlatform {

    /*
     * Bridges the engine WindowPlatform contract to raw GLFW. Maps engine window
     * IDs
     * to native Lwjgl3Window instances. All windows — main and secondary — share
     * identical open, draw, swap, and destroy paths with no special casing.
     */

    // Window Registry
    private static final int MAIN_WINDOW_ID = 0;
    private static final int UNKNOWN_WINDOW_ID = -1;
    private final Int2ObjectOpenHashMap<Lwjgl3Window> windowID2Native = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<GLCapabilities> windowID2Capabilities = new Int2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap handle2WindowID = new Long2IntOpenHashMap();

    public Lwjgl3WindowPlatform() {
        handle2WindowID.defaultReturnValue(UNKNOWN_WINDOW_ID);
    }

    // Internal \\

    @Override
    public void openWindow(WindowInstance window) {

        int windowID = window.getWindowID();
        Lwjgl3Window nativeWindow = resolveOrCreateNativeWindow(window);
        long handle = nativeWindow.getHandle();

        windowID2Native.put(windowID, nativeWindow);
        handle2WindowID.put(handle, windowID);
        window.setNativeHandle(handle);

        if (windowID != MAIN_WINDOW_ID)
            GLFW.glfwShowWindow(handle);

        primeWindowContext(windowID, handle);

        syncWindowSize(window);
    }

    @Override
    public void destroyWindow(WindowInstance window) {

        Lwjgl3Window nativeWindow = windowID2Native.remove(window.getWindowID());

        if (nativeWindow == null)
            return;

        long handle = nativeWindow.getHandle();
        handle2WindowID.remove(handle);
        ((Lwjgl3Application) CoreContext.app).removeSecondaryWindow(handle);
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
    public void swapBuffers(WindowInstance window) {
        if (!window.hasNativeHandle())
            return;

        GLFW.glfwSwapBuffers(window.getNativeHandle());
    }

    @Override
    public void restoreMainContext() {
        if (!(CoreContext.graphics instanceof Lwjgl3Graphics graphics))
            return;

        bindContext(MAIN_WINDOW_ID, graphics.getWindow().getHandle());
    }

    private Lwjgl3Window resolveOrCreateNativeWindow(WindowInstance window) {
        Lwjgl3Window nativeWindow = windowID2Native.get(window.getWindowID());

        if (nativeWindow != null)
            return nativeWindow;

        if (window.getWindowID() == MAIN_WINDOW_ID
                && CoreContext.graphics instanceof Lwjgl3Graphics graphics)
            return graphics.getWindow();

        Lwjgl3WindowConfiguration config = new Lwjgl3WindowConfiguration();
        config.setTitle(window.getTitle());
        config.setWindowedMode(window.getWidth(), window.getHeight());
        return ((Lwjgl3Application) CoreContext.app).newWindow(config);
    }

    private void primeWindowContext(int windowID, long windowHandle) {
        long previousContext = GLFW.glfwGetCurrentContext();
        bindContext(windowID, windowHandle);

        if (previousContext != windowHandle)
            restoreContext(previousContext);
    }

    private void restoreContext(long windowHandle) {

        if (windowHandle == 0L) {
            GLFW.glfwMakeContextCurrent(0L);
            GL.setCapabilities(null);
            return;
        }

        int windowID = handle2WindowID.get(windowHandle);

        if (windowID == UNKNOWN_WINDOW_ID) {
            GLFW.glfwMakeContextCurrent(windowHandle);
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
}