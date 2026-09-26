package engine.root;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.input.Input;

public interface WindowPlatform {

    /*
     * The platform contract the engine drives windows through: opening and
     * destroying windows, GL context switching, input lookup per OS window,
     * cursor, placement, display mode and vsync.
     */

    void openWindow(WindowInstance window);

    void destroyWindow(WindowInstance window);

    boolean shouldClose(WindowInstance window);

    boolean isWindowFocused(WindowInstance window);

    void makeContextCurrent(WindowInstance window);

    void syncInputForWindow(WindowInstance window);

    void swapBuffers(WindowInstance window);

    void restoreMainContext();

    void setCursorShape(long windowHandle, int shape);

    void syncWindowSize(WindowInstance window);

    void placeWindow(WindowInstance window, int screenX, int screenY, int width, int height);

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