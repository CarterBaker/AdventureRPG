package com.internal.bootstrap.renderpipeline.window;

import com.internal.core.app.ApplicationListener;
import com.internal.core.app.Screen;
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
     * Runtime window wrapper. Owns WindowData and RenderQueueHandle.
     * Engine now owns timing for all draws (main + detached). Detached
     * ApplicationListener callbacks are no-op for render timing.
     */

    // Data
    private WindowData windowData;
    private long nativeHandle;

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
        // Optional deferred context path (kept for compatibility).
        if (!hasContext() && pendingContextType != null) {
            internal.createContext(pendingContextType, this);
            pendingContextType = null;
        }
    }

    @Override
    public void render() {
        // Engine-owned draw timing for detached windows.
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

        // Safe idempotent cleanup path.
        vaoManager.removeWindowVAOs(getWindowID());
        renderManager.removeWindowResources(this);

        if (context != null)
            internal.destroyContext(context);

        windowManager.removeWindow(this);
    }

    // Screen — Main Window Path \\

    @Override
    public void render(float delta) {
        // Main window is flushed by engine draw() loop.
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

    // Native Handle \\

    public long getNativeHandle() {
        return nativeHandle;
    }

    public void setNativeHandle(long nativeHandle) {
        this.nativeHandle = nativeHandle;
    }

    public boolean hasNativeHandle() {
        return nativeHandle != 0L;
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