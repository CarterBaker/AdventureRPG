package engine.root;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.input.Input;

public interface WindowPlatform {

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

    void exit();

    /*
     * Returns the Input object backing the given OS window, queried directly
     * from the platform. Pure lookup — never assigns any shared/global input
     * state. This is the only correct way to obtain "the" input for a
     * specific window; EngineContext.input is scoped to "whatever currently
     * owns focus" and is not guaranteed to be this window.
     *
     * Callers must pass a window with a native handle. Results for logical
     * windows are undefined — resolve via WindowInstance.getGLWindow() first.
     */
    Input getInputForWindow(WindowInstance window);

    float getCursorX(WindowInstance window);

    float getCursorY(WindowInstance window);

    void getCursorPos(WindowInstance window, float[] out);

    float getScreenX(WindowInstance window);

    float getScreenY(WindowInstance window);
}