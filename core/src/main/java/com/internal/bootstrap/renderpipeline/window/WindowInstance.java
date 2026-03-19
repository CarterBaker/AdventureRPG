package com.internal.bootstrap.renderpipeline.window;

import com.badlogic.gdx.Screen;
import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.bootstrap.renderpipeline.camera.OrthographicCameraInstance;
import com.internal.core.engine.ContextPackage;
import com.internal.core.engine.InstancePackage;

public class WindowInstance extends InstancePackage implements Screen {

    /*
     * Runtime window. Wraps WindowData and owns the active perspective and
     * orthographic cameras assigned to this window. Permanently paired with
     * a ContextPackage via EnginePackage.createContext() — both sides hold a
     * reference to each other and the pairing cannot change after creation.
     * resize() fires from LibGDX for the main window and from the detached
     * ApplicationListener for all others — both paths call this method directly.
     */

    // Internal
    private WindowData data;

    // Context
    private ContextPackage context;

    // Cameras
    private CameraInstance activeCamera;
    private OrthographicCameraInstance orthoCamera;

    // Constructor \\

    public void constructor(WindowData data) {
        this.data = data;
    }

    // Lifecycle \\

    @Override
    public void resize(int width, int height) {
        data.setWidth(width);
        data.setHeight(height);
        if (activeCamera != null)
            activeCamera.updateViewport(width, height);
        if (orthoCamera != null)
            orthoCamera.updateViewport(width, height);
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

    // Accessible \\

    public WindowData getWindowData() {
        return data;
    }

    public int getWindowID() {
        return data.getWindowID();
    }

    public int getWidth() {
        return data.getWidth();
    }

    public int getHeight() {
        return data.getHeight();
    }

    public ContextPackage getContext() {
        return context;
    }

    public void setContext(ContextPackage context) {
        this.context = context;
    }

    public boolean hasContext() {
        return context != null;
    }

    public CameraInstance getActiveCamera() {
        return activeCamera;
    }

    public void setActiveCamera(CameraInstance camera) {
        this.activeCamera = camera;
    }

    public boolean hasActiveCamera() {
        return activeCamera != null;
    }

    public OrthographicCameraInstance getOrthoCamera() {
        return orthoCamera;
    }

    public void setOrthoCamera(OrthographicCameraInstance orthoCamera) {
        this.orthoCamera = orthoCamera;
    }

    public boolean hasOrthoCamera() {
        return orthoCamera != null;
    }
}