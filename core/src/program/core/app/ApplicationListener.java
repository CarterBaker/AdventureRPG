package program.core.app;

public interface ApplicationListener {

    /*
     * Lifecycle contract for the engine entry point.
     * Called by the platform application loop in order: create, then
     * render each frame, then dispose on shutdown.
     */

    void create();

    void resize(int width, int height);

    void render();

    void pause();

    void resume();

    void dispose();
}