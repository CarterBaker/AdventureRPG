package application.kernel.windowpipeline.window;

import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.menupipeline.menulist.MenuListHandle;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.renderpipeline.renderqueue.RenderQueueHandle;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.assets.camera.CameraInstance;
import engine.assets.camera.OrthographicCameraInstance;
import engine.root.ContextPackage;
import engine.root.InstancePackage;

public class WindowInstance extends InstancePackage {

    /*
     * Runtime window wrapper. Pairs with a context and owns the render queue
     * and menu list. Logical windows (tabs) have no native handle — they carry
     * a composite target and rect so FboRenderSystem can transparently redirect
     * their pushFbo calls to the correct OS window and screen region.
     *
     * zOrder controls both draw order during the screen pass and hit-test
     * priority when windows overlap — higher always wins both. OS windows
     * default to 0 and never change it. Every logical window is assigned a
     * unique, strictly increasing value via WindowManager.bringToFront() at
     * creation time, or whenever it needs to supersede everything else
     * currently open (a drag, a modal dialog, a drop-target preview). There
     * are no fixed per-role depth constants, so there is no way for two
     * unrelated windows to end up pinned to the same value by accident.
     *
     * Input is hover-driven — no active or focus concept exists at this level.
     *
     * captureEligible gates whether InputSystem may capture this window.
     * focusIndependent marks windows that must receive hover-driven input
     * regardless of which window owns focus. Editor chrome windows (e.g. the
     * toolbar) set both flags: captureEligible false so the cursor is never
     * pinned to them, focusIndependent true so menus and hit testing work
     * without requiring a prior click.
     *
     * screenX/screenY holds the OS-level screen position of this window as
     * reported by the platform (e.g. glfwGetWindowPos). Set by the platform
     * layer on window creation and on any window-moved callback. Only
     * meaningful for OS windows (hasNativeHandle() == true); logical windows
     * are positioned via compositeRect instead. Used by TabDragManager to
     * convert global screen cursor coordinates into window-local coordinates
     * for BSP drop-target resolution.
     */

    // Data
    private WindowData windowData;
    private long nativeHandle;

    // Render Queue
    private RenderQueueHandle renderQueueHandle;

    // Menu List
    private MenuListHandle menuListHandle;

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

    // Draw / hit-test order
    private int zOrder;

    // OS-level screen position — OS windows only, set by platform layer
    private float screenX;
    private float screenY;

    // Capture eligibility — false for editor chrome, true for all game windows
    private boolean captureEligible = true;

    // Focus-independent input — true for windows that must remain interactive
    // regardless of focus, such as the toolbar
    private boolean focusIndependent;

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

        this.menuListHandle = create(MenuListHandle.class);
        this.menuListHandle.constructor();
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
        if (hasNativeHandle())
            return this;
        return compositeTarget != null ? compositeTarget.getGLWindow() : this;
    }

    // Screen Position — OS windows only \\

    public float getScreenX() {
        return screenX;
    }

    public float getScreenY() {
        return screenY;
    }

    public void setScreenPosition(float x, float y) {
        this.screenX = x;
        this.screenY = y;
    }

    // Z-Order \\

    public int getZOrder() {
        return zOrder;
    }

    public void setZOrder(int zOrder) {
        this.zOrder = zOrder;
    }

    // Capture Eligibility \\

    public boolean isCaptureEligible() {
        return captureEligible;
    }

    public void setCaptureEligible(boolean captureEligible) {
        this.captureEligible = captureEligible;
    }

    public boolean isFocusIndependent() {
        return focusIndependent;
    }

    public void setFocusIndependent(boolean focusIndependent) {
        this.focusIndependent = focusIndependent;
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

        if (context != null)
            context.onResize(width, height);
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

    public MenuListHandle getMenuListHandle() {
        return menuListHandle;
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