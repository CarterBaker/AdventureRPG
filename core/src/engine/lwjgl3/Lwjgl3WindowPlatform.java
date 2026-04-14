package engine.lwjgl3;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineContext;
import engine.root.WindowPlatform;

import java.nio.IntBuffer;

public class Lwjgl3WindowPlatform implements WindowPlatform {

    /*
     * Bridges the engine WindowPlatform contract to raw GLFW. Maps engine window
     * IDs to native handles. All windows — main and secondary — share identical
     * open, draw, swap, and destroy paths with no special casing.
     */

    // Application
    private Lwjgl3Application application;

    // Window Registry
    private static final int MAIN_WINDOW_ID = 0;
    private static final int UNKNOWN_WINDOW_ID = -1;
    private final Int2LongOpenHashMap windowID2Handle = new Int2LongOpenHashMap();
    private final Int2ObjectOpenHashMap<GLCapabilities> windowID2Capabilities = new Int2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap handle2WindowID = new Long2IntOpenHashMap();

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
}
