package application.kernel.windowpipeline.window;

import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.renderpipeline.renderqueue.RenderQueueHandle;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.assets.camera.CameraInstance;
import engine.assets.camera.OrthographicCameraInstance;
import engine.root.ContextPackage;
import engine.root.InstancePackage;

public class WindowInstance extends InstancePackage {

    /*
     * Runtime window wrapper. Pairs with a context and owns the render queue.
     * Logical windows (tabs) have no native handle — they carry a composite
     * target and rect so FboRenderSystem can transparently redirect their
     * pushFbo calls to the correct OS window and screen region.
     * Depth controls draw order during the screen pass — OS windows are 0,
     * tab logical windows are 1. Higher depth always draws on top.
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

    // Composite routing — logical windows only
    private WindowInstance compositeTarget;
    private float compositeX;
    private float compositeY;
    private float compositeW;
    private float compositeH;
    private boolean compositeRect;

    // Draw order — 0 for OS windows, 1 for tab logical windows
    private int depth;

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

    // Composite Routing \\

    public WindowInstance getCompositeTarget() {
        return compositeTarget;
    }

    public void setCompositeTarget(WindowInstance compositeTarget) {
        this.compositeTarget = compositeTarget;
    }

    public boolean hasCompositeTarget() {
        return compositeTarget != null;
    }

    public void setCompositeRect(float x, float y, float w, float h) {
        this.compositeX = x;
        this.compositeY = y;
        this.compositeW = w;
        this.compositeH = h;
        this.compositeRect = true;
    }

    public void clearCompositeRect() {
        this.compositeRect = false;
    }

    public boolean hasCompositeRect() {
        return compositeRect;
    }

    public float getCompositeX() {
        return compositeX;
    }

    public float getCompositeY() {
        return compositeY;
    }

    public float getCompositeW() {
        return compositeW;
    }

    public float getCompositeH() {
        return compositeH;
    }

    public WindowInstance getGLWindow() {
        return hasNativeHandle() ? this : compositeTarget;
    }

    // Depth \\

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
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
        return hasCompositeTarget() ? compositeTarget.getRenderQueueHandle() : renderQueueHandle;
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