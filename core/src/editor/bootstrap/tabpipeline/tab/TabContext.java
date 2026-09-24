package editor.bootstrap.tabpipeline.tab;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
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
     * Owns the chrome window, chrome menu, and the content context that lives
     * inside the tab's canvas area. A "tab" is really this one object wrapping
     * two windows — nothing outside TabContext ever touches the content window
     * directly.
     *
     * The chrome menu renders through MenuManager like every other menu, and
     * each window keeps its own FBOs sized and bound to the right GL context,
     * so this context has no render or resize work of its own.
     *
     * placeAt() positions the chrome window. TabManager calls syncContent()
     * each frame after the chrome menu has written fresh canvas bounds — that
     * is the only place the content window's composite rect is set.
     *
     * moveTo() reparents both windows to a new OS window. No destroy, no rebuild.
     *
     * bringToFront() elevates chrome and content together, content directly
     * above chrome — the only place either window's zOrder is ever assigned.
     *
     * dispose() is the single teardown path for a tab, regardless of what
     * triggered it — TabManager.closeTab() disposing this tab specifically,
     * the OS window it lives on being disposed (which cascades into every
     * window composited onto it, this one included), or the whole engine
     * shutting down. It removes this tab from the dock tree, disposes the
     * content window (which cascades into the content context, its VAOs,
     * and its render resources exactly like any other window teardown),
     * closes the chrome menu, and deregisters from TabManager's bookkeeping.
     */

    // Internal
    private MenuManager menuManager;
    private FboManager fboManager;
    private WindowManager windowManager;
    private InputManager inputManager;
    private DockLayoutSystem dockLayoutSystem;
    private TabManager tabManager;

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
        windowManager = get(WindowManager.class);
        inputManager = get(InputManager.class);
        dockLayoutSystem = get(DockLayoutSystem.class);
        tabManager = get(TabManager.class);
    }

    @Override
    protected void awake() {
        menuManager.setMenuTargetFbo(getWindow(), fboManager.cloneFbo(RuntimeSetting.FBO_UI, getWindow()));
        chromeMenu = menuManager.openMenu(EngineSetting.MENU_TAB_SHELL, getWindow());
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
     * start of a drag). Both windows take consecutive values from the single
     * zOrder counter, so content always sits directly above its own chrome
     * and never shares a zOrder with any other window.
     */
    public void bringToFront() {

        if (contentContext == null)
            throwException("bringToFront() called before linkContent() — chrome and content must be paired first.");

        windowManager.bringToFront(getWindow());
        windowManager.bringToFront(contentContext.getWindow());
    }

    /*
     * Positions the chrome window. Called by pushRects() and drag tracking.
     * Content placement is handled separately by syncContent() each frame after
     * the chrome menu has written fresh canvas bounds.
     */
    public void placeAt(float x, float y, float w, float h) {

        if (w <= 0 || h <= 0)
            return;

        getWindow().place(x, y, w, h);
    }

    /*
     * Pushes the current canvas bounds to the content window. This is the only
     * place the content window's composite rect is written. Nothing is pushed
     * until the chrome itself has been placed.
     *
     * Canvas coords are in chrome-local pixel space (origin at the chrome
     * window's composite origin). Adding the chrome composite origin gives
     * OS-window space.
     */
    public void syncContent() {

        if (contentContext == null || chromeMenu == null || chromeMenu.getCanvas() == null)
            return;

        WindowInstance tabWindow = getWindow();

        if (!tabWindow.hasCompositeRect())
            return;

        float cw = chromeMenu.getCanvas().getW();
        float ch = chromeMenu.getCanvas().getH();

        if (cw <= 0 || ch <= 0)
            return;

        float cx = tabWindow.getCompositeX() + chromeMenu.getCanvas().getX();
        float cy = tabWindow.getCompositeY() + chromeMenu.getCanvas().getY();

        WindowInstance contentWindow = contentContext.getWindow();

        if (cx == contentWindow.getCompositeX()
                && cy == contentWindow.getCompositeY()
                && cw == contentWindow.getCompositeW()
                && ch == contentWindow.getCompositeH())
            return;

        contentWindow.place(cx, cy, cw, ch);
    }

    /*
     * Reparents both the chrome window and the content window to a different OS
     * window. Both contexts stay alive — no rebuild, no lifecycle gap. Their
     * render resources follow them into the target window's GL context as part
     * of WindowManager.reparentWindow().
     */
    public void moveTo(WindowInstance targetOsWindow) {
        windowManager.reparentWindow(getWindow(), targetOsWindow);
        windowManager.reparentWindow(contentContext.getWindow(), targetOsWindow);
    }

    // Accessible \\

    public MenuInstance getChromeMenu() {
        return chromeMenu;
    }

    public ContextPackage getContentContext() {
        return contentContext;
    }
}
