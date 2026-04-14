package engine.lwjgl3;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

import java.util.function.BooleanSupplier;

public class Lwjgl3Configuration {

    /*
     * Boot-time configuration for the LWJGL3 application. Consumed once during
     * Lwjgl3Application construction — never read again at runtime.
     */

    // Identity
    String title = "Window";

    // Dimensions
    int width = 1280;
    int height = 720;

    // Display
    private int glMajor = 3;
    private int glMinor = 3;
    private boolean fullscreen;
    private boolean vsync = true;

    // Position
    private int windowX = -1;
    private int windowY = -1;

    // Callback
    private BooleanSupplier closeCallback;

    // Accessible \\

    public static DisplayModeStruct getDisplayMode() {

        long monitor = GLFW.glfwGetPrimaryMonitor();
        GLFWVidMode mode = monitor != 0L ? GLFW.glfwGetVideoMode(monitor) : null;

        if (mode != null)
            return new DisplayModeStruct(mode.width(), mode.height(), mode.refreshRate());

        return new DisplayModeStruct(1920, 1080, 60);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setWindowedMode(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setOpenGLVersion(int major, int minor) {
        this.glMajor = major;
        this.glMinor = minor;
    }

    public void setFullscreenMode(DisplayModeStruct mode) {
        this.fullscreen = true;
        this.width = mode.getWidth();
        this.height = mode.getHeight();
    }

    public void useVsync(boolean vsync) {
        this.vsync = vsync;
    }

    public void setWindowPosition(int x, int y) {
        this.windowX = x;
        this.windowY = y;
    }

    public void setCloseCallback(BooleanSupplier closeCallback) {
        this.closeCallback = closeCallback;
    }

    public int getGlMajor() {
        return glMajor;
    }

    public int getGlMinor() {
        return glMinor;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public boolean isVsync() {
        return vsync;
    }

    public int getWindowX() {
        return windowX;
    }

    public int getWindowY() {
        return windowY;
    }

    public BooleanSupplier getCloseCallback() {
        return closeCallback;
    }
}