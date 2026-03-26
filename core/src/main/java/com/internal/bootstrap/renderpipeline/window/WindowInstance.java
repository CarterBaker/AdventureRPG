package com.internal.bootstrap.renderpipeline.window;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Screen;
import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.bootstrap.renderpipeline.camera.OrthographicCameraInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.renderpipeline.rendermanager.RenderManager;
import com.internal.bootstrap.renderpipeline.rendermanager.RenderQueueHandle;
import com.internal.bootstrap.renderpipeline.windowmanager.WindowManager;
import com.internal.core.engine.ContextPackage;
import com.internal.core.engine.InstancePackage;

public class WindowInstance extends InstancePackage implements Screen, ApplicationListener {

    /*
     * Runtime window wrapper. Owns its WindowData and RenderQueueHandle.
     * The queue is created and owned entirely here — no manager involvement.
     * Render calls are pushed into this window's queue explicitly by systems
     * that declare their target window — no declared window defaults to main.
     * The main window's queue is flushed by the engine draw() loop. Each
     * detached window flushes its own queue in its ApplicationListener.render()
     * callback after the engine's full push phase is complete and the correct
     * GL context is current. activeWindow is set on focus for input and
     * raycast systems only — no relation to rendering.
     */

    // Data
    private WindowData windowData;

    // Render Queue
    private RenderQueueHandle renderQueueHandle;

    // Context
    private ContextPackage context;
    private Class<? extends ContextPackage> pendingContextType;

    // Cameras
    private CameraInstance activeCamera;
    private OrthographicCameraInstance orthoCamera;

    // Internal
    private RenderManager renderManager;
    private VAOManager vaoManager;
    private WindowManager windowManager;

    // Internal \\

    public void constructor(WindowData windowData) {
        this.windowData = windowData;
    }

    @Override
    protected void get() {

        // Internal
        this.renderManager = get(RenderManager.class);
        this.vaoManager = get(VAOManager.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void awake() {

        // Render Queue
        this.renderQueueHandle = create(RenderQueueHandle.class);
        this.renderQueueHandle.constructor();
    }

    // ApplicationListener — Detached Window Path \\

    @Override
    public void create() {
        // LibGDX calls this when the detached OS window is ready.
        // If this window was configured for deferred context creation,
        // create it now while this window's GL context is current.
        if (!hasContext() && pendingContextType != null) {
            internal.createContext(pendingContextType, this);
            pendingContextType = null;
        }
    }

    @Override
    public void render() {

        // LibGDX fires this after the engine's full update and push phase for
        // this frame. The queue is fully populated. GL context is current for
        // this window — safe to flush.
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
        vaoManager.removeWindowVAOs(getWindowID());
        renderManager.removeWindowResources(this);

        if (context != null)
            internal.destroyContext(context);

        windowManager.removeWindow(this);
    }

    // Screen — Main Window Path \\

    @Override
    public void render(float delta) {
        // Main window is flushed by the engine draw() loop — nothing to do here.
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

    public void setPendingContextType(Class<? extends ContextPackage> pendingContextType) {
        this.pendingContextType = pendingContextType;
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

    public RenderQueueHandle getRenderQueueHandle() {
        return renderQueueHandle;
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