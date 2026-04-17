package engine.lwjgl3;

import engine.graphics.display.Display;

public class Lwjgl3Display implements Display {

    /*
     * Holds the current display state for the main window. Size and position are
     * updated via framebuffer and position callbacks — never stale after the first
     * frame.
     */

    // State
    private int width;
    private int height;
    private float delta;
    private boolean fullscreen;

    // Window
    private long mainHandle;
    private int posX;
    private int posY;

    Lwjgl3Display(int width, int height, boolean fullscreen) {
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

    void setMainHandle(long mainHandle) {
        this.mainHandle = mainHandle;
    }

    void setPosX(int posX) {
        this.posX = posX;
    }

    void setPosY(int posY) {
        this.posY = posY;
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

    public long getMainHandle() {
        return mainHandle;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }
}