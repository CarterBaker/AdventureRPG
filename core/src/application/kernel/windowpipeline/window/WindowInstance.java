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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WindowInstance extends InstancePackage {

    /*
     * Runtime window wrapper. Pairs with a context and owns the render queue
     * and menu list. Logical windows (tabs) have no native handle — they carry
     * a composite target and rect so FboRenderSystem can transparently redirect
     * their pushFbo calls to the correct OS window and screen region.
     *
     * zOrder controls both draw order during the screen pass and hit-test
     * priority when windows overlap — higher always wins both. See
     * WindowManager.bringToFront().
     *
     * children is the reverse of compositeTarget — every window whose
     * compositeTarget is this one. Maintained automatically by
     * setCompositeTarget() so it can never drift out of sync with the
     * forward reference. dispose() walks it to guarantee that tearing down
     * a window always tears down everything visually composited onto it —
     * every tab, every dialog, every drag ghost — with no separate manual
     * scan anywhere else in the engine responsible for remembering to do so.
     *
     * disposeListener is an optional hook fired once, after this window is
     * fully torn down, regardless of what triggered the teardown — an
     * explicit close request, the platform's own window-close button, or
     * engine shutdown. Editor-layer code (TabManager) uses this to keep its
     * own per-window bookkeeping in sync without WindowInstance or
     * WindowManager needing to know anything about tabs or docking.
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
    private final ObjectArrayList<WindowInstance> children = new ObjectArrayList<>();
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

    /*
     * Sets which window this one composites onto, keeping that window's
     * children list in sync automatically — detaching from the previous
     * target and attaching to the new one in the same call. This is the
     * only place either side of that relationship is ever touched, so the
     * forward reference (compositeTarget) and the reverse reference
     * (children) can never disagree with each other.
     */
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

    // Disposal \\

    /*
     * Registers a callback fired once dispose() has fully torn this window
     * down — resources released, context destroyed, removed from
     * WindowManager. Fires no matter which of the (identical) code paths
     * triggered that teardown. One listener per window is sufficient for
     * every current use; nothing has needed more than one reason to care.
     */
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

    /*
     * Tears this window down completely and unconditionally cascades to
     * every window composited onto it first — so closing an OS window
     * always closes every tab, dialog, and drag ghost living on it, with
     * no separate manual cleanup required anywhere else. Iterates a
     * snapshot of children since each child's own dispose() mutates this
     * window's live children list via setCompositeTarget(null).
     *
     * Safe to call more than once on the same instance — every step here
     * is a no-op the second time (empty children, context already
     * detached, compositeTarget already cleared, already removed from
     * WindowManager), which is what lets ownership-level teardown code
     * (e.g. TabContext explicitly disposing its content window) and this
     * generic composite-cascade both reach the same window without either
     * needing to coordinate who goes first.
     */
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