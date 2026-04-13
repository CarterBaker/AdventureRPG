package application.core.engine;

import application.core.kernel.window.WindowInstance;

public interface WindowPlatform {

    /*
     * Backend contract for all GLFW window operations. Implemented once per
     * platform and injected at launch. The engine never calls GLFW directly —
     * all window management routes through here.
     */

    void openWindow(WindowInstance window);

    void destroyWindow(WindowInstance window);

    boolean shouldClose(WindowInstance window);

    boolean isWindowFocused(WindowInstance window);

    void makeContextCurrent(WindowInstance window);

    void swapBuffers(WindowInstance window);

    void restoreMainContext();

    void syncWindowSize(WindowInstance window);

    void exit();
}