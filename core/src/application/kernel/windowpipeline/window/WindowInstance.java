package application.kernel.windowpipeline.window;

import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.menupipeline.menulist.MenuListHandle;
import application.bootstrap.renderpipeline.render.RenderQueueHandle;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.assets.camera.CameraInstance;
import engine.assets.camera.OrthographicCameraInstance;
import engine.root.ContextPackage;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WindowInstance extends InstancePackage {

    /*
     * Runtime window wrapper. Pairs with a context and owns its render queue
     * and menu list. Logical windows (tabs) have no native handle and composite
     * onto an OS window through a target and rect. zOrder decides both draw
     * order and hit priority, and dispose() tears down every window composited
     * onto this one along with its render resources before firing the dispose
     * listener.
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
    private final ObjectArrayList<WindowInstance> children = new ObjectArrayList<>();
    private float compositeX;
    private float compositeY;
    private float compositeW;
    private float compositeH;
    private boolean compositeRect;

    // Input region — logical windows only, OS-window space like compositeRect
    private float inputX;
    private float inputY;
    private float inputW;
    private float inputH;
    private boolean inputRect;

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

    // Disposal
    private Runnable disposeListener;

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
        if (this.compositeTarget != null)
            this.compositeTarget.children.remove(this);
        this.compositeTarget = compositeTarget;
        if (compositeTarget != null)
            compositeTarget.children.add(this);
    }

    public boolean hasCompositeTarget() {
        return compositeTarget != null;
    }

    public void place(float x, float y, float w, float h) {
        setCompositeRect(x, y, w, h);
        resize((int) w, (int) h);
        renderManager.resizeWindowResources(this);
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

    // Input Region \\

    public void setInputRect(float x, float y, float w, float h) {
        this.inputX = x;
        this.inputY = y;
        this.inputW = w;
        this.inputH = h;
        this.inputRect = true;
    }

    public boolean acceptsInputAt(float x, float y) {

        if (inputRect)
            return x >= inputX && x < inputX + inputW && y >= inputY && y < inputY + inputH;

        return x >= compositeX && x < compositeX + compositeW && y >= compositeY && y < compositeY + compositeH;
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

    // Disposal \\

    public void setDisposeListener(Runnable listener) {
        this.disposeListener = listener;
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

    public void migrateRenderResources(WindowInstance previousGLWindow) {
        renderManager.migrateWindowResources(this, previousGLWindow);
    }

    public void dispose() {

        for (WindowInstance child : new ObjectArrayList<>(children))
            child.dispose();

        vaoManager.removeWindowVAOs(getWindowID());
        renderManager.removeWindowResources(this);

        if (context != null)
            internal.destroyContext(context);

        setCompositeTarget(null);
        windowManager.removeWindow(this);

        if (disposeListener != null)
            disposeListener.run();
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