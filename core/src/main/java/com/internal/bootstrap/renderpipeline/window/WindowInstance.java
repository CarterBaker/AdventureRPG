package com.internal.bootstrap.renderpipeline.window;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Screen;
import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.bootstrap.renderpipeline.camera.OrthographicCameraInstance;
import com.internal.bootstrap.renderpipeline.rendermanager.RenderManager;
import com.internal.bootstrap.renderpipeline.windowmanager.WindowManager;
import com.internal.core.engine.ContextPackage;
import com.internal.core.engine.InstancePackage;

public class WindowInstance extends InstancePackage implements Screen, ApplicationListener {

    /*
     * Runtime window wrapper. Owns its WindowData. Paired with a ContextPackage
     * at creation time via EnginePackage.createContext() — both sides hold a
     * reference to each other. Holds the active perspective camera and orthographic
     * camera for this window — set by whoever owns the camera (PlayerManager,
     * EditorCameraSystem, etc). The main window is driven as a Screen via
     * game.setScreen(); detached windows receive their own ApplicationListener
     * render() callback from LibGDX directly, which is the only safe moment
     * to flush that window's render queue since the GL context is current then.
     */

    // Data
    private WindowData windowData;

    // Context
    private ContextPackage context;

    // Cameras
    private CameraInstance activeCamera;
    private OrthographicCameraInstance orthoCamera;

    // Internal
    private RenderManager renderManager;
    private WindowManager windowManager;

    // Internal \\

    @Override
    protected void awake() {

        // Internal
        this.renderManager = internal.getUnchecked(RenderManager.class);
        this.windowManager = internal.getUnchecked(WindowManager.class);
    }

    public void constructor(WindowData windowData) {
        this.windowData = windowData;
    }

    // ApplicationListener — Detached Window Path \\

    @Override
    public void create() {
        // LibGDX calls this when the detached OS window is ready.
        // WindowInstance is already fully initialized by the engine before
        // internal.windowPlatform.openWindow() is called — nothing to do here.
    }

    @Override
    public void render() {

        // LibGDX makes this window's GL context current before calling this.
        // The engine has already run its full update and render phases this frame.
        // This is the only safe moment to flush render calls to this window's context.
        windowManager.setActiveWindow(this);
        renderManager.draw(this);
    }

    @Override
    public void resize(int width, int height) {
        windowData.setWidth(width);
        windowData.setHeight(height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {

        // LibGDX calls this when the user closes a detached OS window.
        // Destroy the paired context and remove this window from the registry.
        if (context != null)
            internal.destroyContext(context);

        windowManager.removeWindow(this);
    }

    // Screen — Main Window Path \\

    @Override
    public void render(float delta) {
        // Main window render is driven by EditorEngine.draw() / GameEngine.draw().
        // LibGDX fires this callback but the engine owns the actual draw call.
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    // Context \\

    public ContextPackage getContext() {
        return context;
    }

    public void setContext(ContextPackage context) {
        this.context = context;
    }

    public boolean hasContext() {
        return context != null;
    }

    // Cameras \\

    public CameraInstance getActiveCamera() {
        return activeCamera;
    }

    public void setActiveCamera(CameraInstance activeCamera) {
        this.activeCamera = activeCamera;
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

    // Accessible \\

    public WindowData getWindowData() {
        return windowData;
    }

    public int getWindowID() {
        return windowData.getWindowID();
    }

    public int getWidth() {
        return windowData.getWidth();
    }

    public int getHeight() {
        return windowData.getHeight();
    }

    public String getTitle() {
        return windowData.getTitle();
    }
}