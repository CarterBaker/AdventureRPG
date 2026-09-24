package engine.lwjgl3;

import engine.graphics.display.Display;

public class Lwjgl3Display implements Display {

    /*
     * Holds the current display state for the main window. Width and height track
     * the framebuffer; position, window size, and maximized state track the last
     * restored (non-maximized, non-minimized) bounds in screen units, which is what
     * the launchers persist and restore.
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
    private int windowWidth;
    private int windowHeight;
    private boolean maximized;

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

    void setWindowBounds(int posX, int posY, int windowWidth, int windowHeight) {
        this.posX = posX;
        this.posY = posY;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
    }

    void setMaximized(boolean maximized) {
        this.maximized = maximized;
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

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public boolean isMaximized() {
        return maximized;
    }
}