package com.AdventureRPG.core.engine;

import com.AdventureRPG.core.renderpipeline.camerasystem.CameraManager;
import com.badlogic.gdx.Screen;

public class WindowSystem extends SystemPackage implements Screen {

    // Core
    private CameraManager cameraManager;

    // Internal
    private int width;
    private int height;

    // Core \\

    @Override
    protected void init() {

        // Core
        this.cameraManager = internal.get(CameraManager.class);

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
