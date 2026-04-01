package program.core.engine;

import program.bootstrap.renderpipeline.window.WindowInstance;

public interface WindowPlatform {

    // Detached lifecycle
    void openWindow(WindowInstance window);

    void destroyWindow(WindowInstance window);

    boolean shouldClose(WindowInstance window);

    // Context + presentation
    void makeContextCurrent(WindowInstance window);

    void swapBuffers(WindowInstance window);

    void restoreMainContext();

    // Sync
    void syncWindowSize(WindowInstance window);
}