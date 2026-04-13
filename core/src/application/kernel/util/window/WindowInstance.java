package application.kernel.util.window;

import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.renderpipeline.rendermanager.RenderQueueHandle;
import application.kernel.windowmanager.WindowManager;
import engine.root.ContextPackage;
import engine.root.InstancePackage;
import engine.util.camera.CameraInstance;
import engine.util.camera.OrthographicCameraInstance;

public class WindowInstance extends InstancePackage {

    /*
     * Runtime window wrapper. Owns WindowData, RenderQueueHandle, and both
     * cameras. WindowData and cameras are assigned in constructor(), while
     * render queue setup occurs in awake().
     */

    // Data
    private WindowData windowData;
    private long nativeHandle;

    // Render Queue
    private RenderQueueHandle renderQueueHandle;

    // Context
    private ContextPackage context;

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

        this.activeCamera = internal.createCamera(
                internal.settings.FOV,
                windowData.getWidth(),
                windowData.getHeight());

        this.orthoCamera = internal.createOrthographicCamera(
                windowData.getWidth(),
                windowData.getHeight());
    }

    @Override
    protected void get() {
        this.renderManager = get(RenderManager.class);
        this.vaoManager = get(VAOManager.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void awake() {
        this.renderQueueHandle = create(RenderQueueHandle.class);
        this.renderQueueHandle.constructor();
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

    public OrthographicCameraInstance getOrthoCamera() {
        return orthoCamera;
    }

    public void setOrthoCamera(OrthographicCameraInstance orthoCamera) {
        this.orthoCamera = orthoCamera;
    }

    // Accessible \\

    public void resize(int width, int height) {

        windowData.setWidth(width);
        windowData.setHeight(height);

        if (activeCamera != null)
            activeCamera.updateViewport(width, height);

        if (orthoCamera != null)
            orthoCamera.updateViewport(width, height);
    }

    public void dispose() {

        vaoManager.removeWindowVAOs(getWindowID());
        renderManager.removeWindowResources(this);

        if (context != null)
            internal.destroyContext(context);

        windowManager.removeWindow(this);
    }

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