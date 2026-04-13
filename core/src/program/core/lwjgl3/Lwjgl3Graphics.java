package program.core.lwjgl3;

import program.core.graphics.Graphics;

public class Lwjgl3Graphics implements Graphics {

    /*
     * Holds the current display state for the main window. Size is updated each
     * frame via the framebuffer resize callback — never stale after the first
     * frame.
     */

    // State
    private int width;
    private int height;
    private float delta;
    private boolean fullscreen;

    // Window
    private Lwjgl3Window window;

    Lwjgl3Graphics(int width, int height, boolean fullscreen) {
        this.width = width;
        this.height = height;
        this.fullscreen = fullscreen;
    }

    // Internal \\

    void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    void setDelta(float delta) {
        this.delta = delta;
    }

    void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    void setWindow(Lwjgl3Window window) {
        this.window = window;
    }

    // Accessible \\

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public float getDeltaTime() {
        return delta;
    }

    @Override
    public boolean isFullscreen() {
        return fullscreen;
    }

    public Lwjgl3Window getWindow() {
        return window;
    }
}