package program.core.app;

public interface Screen {

    /*
     * Lifecycle contract for a renderable screen. Shown and hidden by Game
     * on transition. Render is driven by Game.render() each frame.
     */

    void show();

    void render(float delta);

    void resize(int width, int height);

    void pause();

    void resume();

    void hide();

    void dispose();
}