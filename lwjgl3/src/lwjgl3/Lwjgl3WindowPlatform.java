package lwjgl3;

import program.core.app.CoreContext;
import program.core.backends.lwjgl3.Lwjgl3Application;
import program.core.backends.lwjgl3.Lwjgl3Graphics;
import program.core.backends.lwjgl3.Lwjgl3Window;
import program.core.backends.lwjgl3.Lwjgl3WindowConfiguration;
import program.core.engine.WindowPlatform;
import program.core.kernel.window.WindowInstance;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
    private final Int2ObjectOpenHashMap<Lwjgl3Window> windowID2Native = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<GLCapabilities> windowID2Capabilities = new Int2ObjectOpenHashMap<>();

    // Internal \\

    @Override
    public void openWindow(WindowInstance window) {

        Lwjgl3Window nativeWindow = windowID2Native.get(window.getWindowID());

        if (nativeWindow == null && window.getWindowID() == 0
                && CoreContext.graphics instanceof Lwjgl3Graphics graphics)
            nativeWindow = graphics.getWindow();

        if (nativeWindow == null) {
            Lwjgl3WindowConfiguration config = new Lwjgl3WindowConfiguration();
            config.setTitle(window.getTitle());
            config.setWindowedMode(window.getWidth(), window.getHeight());
            nativeWindow = ((Lwjgl3Application) CoreContext.app).newWindow(config);
        }

        windowID2Native.put(window.getWindowID(), nativeWindow);
        window.setNativeHandle(nativeWindow.getHandle());

        // If this context is already current (true for main window during bootstrap),
        // capture capabilities now. Secondary windows will lazily initialize caps
        // in makeContextCurrent() on first bind.
        if (GLFW.glfwGetCurrentContext() == nativeWindow.getHandle()) {
            GLCapabilities caps = GL.getCapabilities();
            if (caps != null)
                windowID2Capabilities.put(window.getWindowID(), caps);
        }

        syncWindowSize(window);
    }

    @Override
    public void destroyWindow(WindowInstance window) {

        Lwjgl3Window nativeWindow = windowID2Native.remove(window.getWindowID());

        if (nativeWindow == null)
            return;

        long handle = nativeWindow.getHandle();
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
    public void makeContextCurrent(WindowInstance window) {
        if (!window.hasNativeHandle())
            return;

        GLFW.glfwMakeContextCurrent(window.getNativeHandle());

        GLCapabilities caps = windowID2Capabilities.get(window.getWindowID());

        if (caps == null) {
            caps = GL.createCapabilities();
            windowID2Capabilities.put(window.getWindowID(), caps);
        }

        GL.setCapabilities(caps);
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

        long mainHandle = graphics.getWindow().getHandle();
        GLFW.glfwMakeContextCurrent(mainHandle);

        GLCapabilities caps = windowID2Capabilities.get(0);

        if (caps == null) {
            caps = GL.getCapabilities();
            if (caps == null)
                caps = GL.createCapabilities();
            windowID2Capabilities.put(0, caps);
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