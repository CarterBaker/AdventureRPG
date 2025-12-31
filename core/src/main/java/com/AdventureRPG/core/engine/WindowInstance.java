package com.AdventureRPG.core.engine;

import com.AdventureRPG.core.renderpipeline.camerasystem.CameraManager;
import com.badlogic.gdx.Screen;

public class WindowInstance extends InstancePackage implements Screen {

    // Internal
    private CameraManager cameraManager;

    private int width;
    private int height;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.cameraManager = get(CameraManager.class);

        // Internal
        this.width = 0;
        this.height = 0;
    }

    // Screen \\

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
    }

    @Override
    public void resize(int width, int height) {

        cameraManager.resize(width, height);

        this.width = width;
        this.height = height;
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
    }

    // Accessible \\

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
