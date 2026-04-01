package program.core.app;

public interface Screen {
    void show();
    void render(float delta);
    void resize(int width, int height);
    void pause();
    void resume();
    void hide();
    void dispose();
}
