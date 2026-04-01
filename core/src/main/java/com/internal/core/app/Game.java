package com.internal.core.app;

public abstract class Game implements ApplicationListener {
    protected Screen screen;

    public void setScreen(Screen screen) {
        if (this.screen != null) this.screen.hide();
        this.screen = screen;
        if (screen != null) {
            screen.show();
            if (CoreContext.graphics != null) screen.resize(CoreContext.graphics.getWidth(), CoreContext.graphics.getHeight());
        }
    }

    public Screen getScreen() { return screen; }

    @Override
    public void render() {
        if (screen != null && CoreContext.graphics != null) screen.render(CoreContext.graphics.getDeltaTime());
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
