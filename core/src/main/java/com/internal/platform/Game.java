package com.internal.platform;

public abstract class Game implements ApplicationListener {
    protected Screen screen;

    public void setScreen(Screen screen) {
        if (this.screen != null) this.screen.hide();
        this.screen = screen;
        if (screen != null) {
            screen.show();
            if (Gdx.graphics != null) screen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
    }

    public Screen getScreen() { return screen; }

    @Override
    public void render() {
        if (screen != null && Gdx.graphics != null) screen.render(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void resize(int width, int height) { if (screen != null) screen.resize(width, height); }
    @Override
    public void pause() { if (screen != null) screen.pause(); }
    @Override
    public void resume() { if (screen != null) screen.resume(); }
    @Override
    public void dispose() { if (screen != null) screen.dispose(); }
}
