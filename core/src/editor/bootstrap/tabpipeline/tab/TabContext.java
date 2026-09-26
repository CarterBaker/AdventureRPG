package editor.bootstrap.tabpipeline.tab;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.runtime.EditorSetting;
import engine.root.ContextPackage;

public class TabContext extends ContextPackage {

    /*
     * One editor tab: a chrome window with its menu, plus the content context
     * living in its canvas. Placement, content sync, reparenting and z-ordering
     * all go through here, and dispose() is the single teardown path however
     * the tab closes.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
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
        fboManager = get(FBOManager.class);
        windowManager = get(WindowManager.class);
        inputManager = get(InputManager.class);
        dockLayoutSystem = get(DockLayoutSystem.class);
        tabManager = get(TabManager.class);
    }

    @Override
    protected void awake() {
        menuManager.setMenuTargetFbo(getWindow(), fboManager.cloneFbo(RuntimeSetting.FBO_UI, getWindow()));
        chromeMenu = menuManager.openMenu(EditorSetting.MENU_TAB_SHELL, getWindow());
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

    public void linkContent(ContextPackage contentContext) {

        this.contentContext = contentContext;

        contentContext.getWindow().getMenuListHandle().setLockReleaseListener(
                () -> inputManager.onInputLockReleased(contentContext.getWindow()));
    }

    public void setOwnerHandle(TabHandle handle) {
        this.ownerHandle = handle;
        showTitle(handle.getTabTitle());
    }

    public void bringToFront() {

        if (contentContext == null)
            throwException("bringToFront() called before linkContent() — chrome and content must be paired first.");

        windowManager.bringToFront(getWindow());
        windowManager.bringToFront(contentContext.getWindow());
    }

    public void placeAt(float x, float y, float w, float h) {

        if (w <= 0 || h <= 0)
            return;

        getWindow().place(x, y, w, h);
    }

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

    public void moveTo(WindowInstance targetOsWindow) {
        windowManager.reparentWindow(getWindow(), targetOsWindow);
        windowManager.reparentWindow(contentContext.getWindow(), targetOsWindow);
    }

    // Utility \\

    private void showTitle(String title) {

        ElementInstance titleLabel = chromeMenu.getEntryPoint(EditorSetting.TAB_ENTRY_TITLE);

        if (titleLabel != null)
            titleLabel.setFontText(title);
    }

    // Accessible \\

    public MenuInstance getChromeMenu() {
        return chromeMenu;
    }

    public ContextPackage getContentContext() {
        return contentContext;
    }
}
