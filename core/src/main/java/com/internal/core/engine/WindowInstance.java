package com.internal.core.engine;

import com.badlogic.gdx.Screen;
import com.internal.bootstrap.menupipeline.menumanager.MenuManager;
import com.internal.bootstrap.renderpipeline.cameramanager.CameraManager;

public class WindowInstance extends InstancePackage implements Screen {

    // Internal
    private CameraManager cameraManager;
    private MenuManager menuManager;
    private int width;
    private int height;

    @Override
    protected void get() {
        this.cameraManager = get(CameraManager.class);
        this.menuManager = get(MenuManager.class);
        this.width = 0;
        this.height = 0;
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        cameraManager.resize(width, height);
        menuManager.resize(width, height);
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
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

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}