package program.core.app;

public interface ApplicationListener {
    void create();
    void resize(int width, int height);
    void render();
    void pause();
    void resume();
    void dispose();
}
