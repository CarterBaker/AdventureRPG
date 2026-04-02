package lwjgl3;

import program.core.app.CoreContext;
import program.core.backends.lwjgl3.Lwjgl3Application;
import program.core.backends.lwjgl3.Lwjgl3Graphics;
import program.core.backends.lwjgl3.Lwjgl3Window;
import program.core.backends.lwjgl3.Lwjgl3WindowConfiguration;
import program.bootstrap.renderpipeline.window.WindowInstance;
import program.core.engine.WindowPlatform;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

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

        GLFW.glfwMakeContextCurrent(graphics.getWindow().getHandle());
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

        if (width > 0 && height > 0) {
            window.getWindowData().setWidth(width);
            window.getWindowData().setHeight(height);
        }
    }
}