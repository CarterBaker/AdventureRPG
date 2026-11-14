package engine.root;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.input.Input;

public interface WindowPlatform {

    /*
     * The platform contract the engine drives windows through: opening and
     * destroying windows, GL context switching, input lookup per OS window,
     * cursor, placement, display mode and vsync, whether a window is the one
     * uppermost under the cursor, and for a transparent window whether it lets
     * the cursor through to whatever lies behind it. A window that draws its
     * own frame minimizes, maximizes, closes and reads and sets its bounds in
     * screen units through the frame calls.
     */

    void openWindow(WindowInstance window);

    void destroyWindow(WindowInstance window);

    boolean shouldClose(WindowInstance window);

    boolean isWindowFocused(WindowInstance window);

    boolean isWindowHovered(WindowInstance window);

    void makeContextCurrent(WindowInstance window);

    void syncInputForWindow(WindowInstance window);

    void swapBuffers(WindowInstance window);

    void restoreMainContext();

    void setCursorShape(long windowHandle, int shape);

    void syncWindowSize(WindowInstance window);

    void placeWindow(WindowInstance window, int screenX, int screenY, int width, int height);

    void moveWindow(WindowInstance window, int screenX, int screenY);

    void getScreenBounds(WindowInstance window, float[] out);

    void setScreenBounds(WindowInstance window, int screenX, int screenY, int width, int height);

    boolean getWorkArea(float screenX, float screenY, float[] out);

    boolean isMaximized(WindowInstance window);

    void setMaximized(WindowInstance window, boolean maximized);

    void minimizeWindow(WindowInstance window);

    boolean isFullscreen(WindowInstance window);

    void requestClose(WindowInstance window);

    boolean isTransparent(WindowInstance window);

    void setMousePassthrough(WindowInstance window, boolean passthrough);

    void setFullscreen(boolean fullscreen);

    void setVsync(boolean vsync);

    void exit();

    Input getInputForWindow(WindowInstance window);

    float getCursorX(WindowInstance window);

    float getCursorY(WindowInstance window);

    void getCursorPos(WindowInstance window, float[] out);

    float getScreenX(WindowInstance window);

    float getScreenY(WindowInstance window);
}