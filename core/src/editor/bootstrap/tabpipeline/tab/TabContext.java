package editor.bootstrap.tabpipeline.tab;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.ContextPackage;
import engine.root.EngineSetting;

public class TabContext extends ContextPackage {

    /*
     * Owns the chrome window, chrome FBO, chrome menu, and the content context
     * that lives inside the tab's canvas area. A "tab" is really this one
     * object wrapping two windows — nothing outside TabContext ever touches
     * the content window directly.
     *
     * placeAt() positions the chrome window. The compositor calls syncContent()
     * each frame after MenuRenderSystem has written fresh canvas bounds — that
     * is the only place the content window's composite rect is set.
     *
     * moveTo() reparents both windows to a new OS window. No destroy, no rebuild.
     *
     * bringToFront() elevates chrome and content together, always keeping
     * content exactly one zOrder above chrome — the only place either
     * window's zOrder is ever assigned.
     *
     * dispose() is the single teardown path for a tab, regardless of what
     * triggered it — TabManager.closeTab() disposing this tab specifically,
     * the OS window it lives on being disposed (which cascades into every
     * window composited onto it, this one included), or the whole engine
     * shutting down. It removes this tab from the dock tree, disposes the
     * content window (which cascades into the content context, its VAOs,
     * and its render resources exactly like any other window teardown),
     * closes the chrome menu, and deregisters from TabManager's bookkeeping.
     * TabManager never reaches into a tab's teardown by hand — it only ever
     * calls dispose() on the chrome window and lets this method do the rest.
     */

    // Internal
    private MenuManager menuManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;
    private WindowManager windowManager;
    private InputManager inputManager;
    private DockLayoutSystem dockLayoutSystem;
    private TabManager tabManager;

    // Render Target
    private FboInstance uiFbo;

    // Chrome
    private MenuInstance chromeMenu;

    // Content — owned by this tab, positioned within this tab's canvas
    private ContextPackage contentContext;

    // Identity — the handle that owns this context, wired immediately after
    // both are constructed. Lets dispose() deregister itself from TabManager
    // without TabManager needing to know anything about teardown order.
    private TabHandle ownerHandle;

    // Internal \\

    @Override
    protected void get() {
        menuManager = get(MenuManager.class);
        fboManager = get(FboManager.class);
        fboRenderSystem = get(FboRenderSystem.class);
        windowManager = get(WindowManager.class);
        inputManager = get(InputManager.class);
        dockLayoutSystem = get(DockLayoutSystem.class);
        tabManager = get(TabManager.class);
    }

    @Override
    protected void awake() {
        uiFbo = fboManager.cloneFbo(RuntimeSetting.FBO_UI, getWindow());
        menuManager.setMenuTargetFbo(getWindow(), uiFbo);
        chromeMenu = menuManager.openMenu(EngineSetting.MENU_TAB_SHELL, getWindow());
    }

    @Override
    public void onResize(int width, int height) {

        if (uiFbo == null || width <= 0 || height <= 0)
            return;

        uiFbo.resize(width, height);
    }

    // Render \\

    @Override
    protected void render() {

        if (!getWindow().hasCompositeRect())
            return;

        fboRenderSystem.pushFbo(uiFbo, RuntimeSetting.LAYER_UI, getWindow());
    }

    // Dispose \\

    @Override
    protected void dispose() {

        WindowInstance osWindow = getWindow().getGLWindow();
        dockLayoutSystem.removeTab(osWindow, ownerHandle);

        if (contentContext != null) {
            contentContext.getWindow().getMenuListHandle().setLockReleaseListener(null);
            contentContext.getWindow().dispose();
        }

        if (chromeMenu != null) {
            menuManager.closeMenu(chromeMenu);
            chromeMenu = null;
        }

        menuManager.setMenuTargetFbo(getWindow(), null);

        tabManager.deregisterTab(ownerHandle);
    }

    // Management \\

    /*
     * Pairs this tab to its content context. Called once after both contexts are
     * created. Wires the input lock release listener to the content window.
     */
    public void linkContent(ContextPackage contentContext) {

        this.contentContext = contentContext;

        contentContext.getWindow().getMenuListHandle().setLockReleaseListener(
                () -> inputManager.onInputLockReleased(contentContext.getWindow()));
    }

    /*
     * Wires this context back to the TabHandle that owns it. Called once,
     * immediately after both are constructed, alongside linkContent().
     */
    public void setOwnerHandle(TabHandle handle) {
        this.ownerHandle = handle;
    }

    /*
     * Elevates this tab — chrome and content together — strictly above
     * everything else currently open. Called once when the tab is opened,
     * and again whenever it needs to float above everything else (e.g. the
     * start of a drag). Content always ends up exactly one zOrder above
     * chrome; nothing outside this method ever assigns either window's
     * zOrder directly.
     */
    public void bringToFront() {

        if (contentContext == null)
            throwException("bringToFront() called before linkContent() — chrome and content must be paired first.");

        WindowInstance chromeWindow = getWindow();
        windowManager.bringToFront(chromeWindow);
        contentContext.getWindow().setZOrder(chromeWindow.getZOrder() + 1);
    }

    /*
     * Positions the chrome window. Called by pushRects() and drag tracking.
     * Content placement is handled separately by syncContent() each frame after
     * MenuRenderSystem has written fresh canvas bounds.
     */
    public void placeAt(float x, float y, float w, float h) {

        if (w <= 0 || h <= 0)
            return;

        WindowInstance tabWindow = getWindow();
        tabWindow.setCompositeRect(x, y, w, h);
        tabWindow.resize((int) w, (int) h);
    }

    /*
     * Pushes the current canvas bounds to the content window. Called by the
     * compositor each frame after MenuRenderSystem has rendered and written fresh
     * bounds into the canvas. This is the only place the content window's
     * composite rect is written.
     *
     * Canvas coords are in chrome-local pixel space (origin top-left of chrome
     * window). Adding the chrome composite origin gives OS-window space.
     */
    public void syncContent() {

        if (contentContext == null || chromeMenu == null)
            return;

        if (chromeMenu.getCanvas() == null)
            return;

        float cw = chromeMenu.getCanvas().getW();
        float ch = chromeMenu.getCanvas().getH();

        if (cw <= 0 || ch <= 0)
            return;

        WindowInstance tabWindow = getWindow();
        float cx = tabWindow.getCompositeX() + chromeMenu.getCanvas().getX();
        float cy = tabWindow.getCompositeY() + chromeMenu.getCanvas().getY();

        WindowInstance contentWindow = contentContext.getWindow();

        if (cx == contentWindow.getCompositeX()
                && cy == contentWindow.getCompositeY()
                && cw == contentWindow.getCompositeW()
                && ch == contentWindow.getCompositeH())
            return;

        contentWindow.setCompositeRect(cx, cy, cw, ch);
        contentWindow.resize((int) cw, (int) ch);
    }

    /*
     * Reparents both the chrome window and the content window to a different OS
     * window. Both contexts stay alive — no rebuild, no lifecycle gap.
     */
    public void moveTo(WindowInstance targetOsWindow) {
        windowManager.reparentWindow(getWindow(), targetOsWindow);
        windowManager.reparentWindow(contentContext.getWindow(), targetOsWindow);
    }

    // Accessible \\

    public MenuInstance getChromeMenu() {
        return chromeMenu;
    }

    public FboInstance getUiFbo() {
        return uiFbo;
    }

    public ContextPackage getContentContext() {
        return contentContext;
    }
}