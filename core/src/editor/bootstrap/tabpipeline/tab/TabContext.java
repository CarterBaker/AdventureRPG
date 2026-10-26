package editor.bootstrap.tabpipeline.tab;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.runtime.EditorSetting;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TabContext extends ContextPackage {

    /*
     * One editor tab: a chrome window with its menu, plus the content context
     * living in its canvas. Placement, content sync, reparenting, z-ordering
     * and the tab strip all go through here. The tab a stack shows is placed
     * on its leaf with a strip listing the whole stack, rebuilt only when the
     * stack changes; every other tab in it is withdrawn with its content.
     * dispose() is the single teardown path however the tab closes.
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

    // Strip — the stack it lists, and this tab alone for while it is out of every stack
    private ObjectArrayList<TabHandle> stripTabs;
    private ObjectArrayList<TabHandle> loneStrip;
    private MenuColorStruct activeStripColor;
    private MenuColorStruct activeStripAccentColor;

    // Internal \\

    @Override
    protected void create() {

        // Strip
        this.stripTabs = new ObjectArrayList<>();
        this.loneStrip = new ObjectArrayList<>(1);
        this.activeStripColor = new MenuColorStruct(ThemeColor.ACCENT, EditorSetting.TAB_STRIP_ACTIVE_ALPHA);
        this.activeStripAccentColor = new MenuColorStruct(ThemeColor.ACCENT, EngineSetting.MENU_THEME_ALPHA_DEFAULT);
    }

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
        loneStrip.add(handle);
        showStrip(loneStrip);
    }

    public void bringToFront() {

        if (contentContext == null)
            throwException("bringToFront() called before linkContent() — chrome and content must be paired first.");

        windowManager.bringToFront(getWindow());
        windowManager.bringToFront(contentContext.getWindow());
    }

    // The stack's shown tab fills its leaf; any other tab in it waits withdrawn behind that one
    public void dockIn(DockNodeStruct leaf) {

        if (leaf.getActiveTab() != ownerHandle) {
            withdraw();
            return;
        }

        placeAt(leaf.getX(), leaf.getY(), leaf.getW(), leaf.getH());
        showStrip(leaf.getTabs());
    }

    public void undock() {
        showStrip(loneStrip);
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

        if (contentWindow.isPlacedAt(cx, cy, cw, ch))
            return;

        contentWindow.place(cx, cy, cw, ch);
    }

    public void moveTo(WindowInstance targetOsWindow) {
        windowManager.reparentWindow(getWindow(), targetOsWindow);
        windowManager.reparentWindow(contentContext.getWindow(), targetOsWindow);
    }

    // Utility \\

    private void withdraw() {

        WindowInstance contentWindow = contentContext.getWindow();

        getWindow().withdraw();
        contentWindow.withdraw();
        inputManager.releaseWindow(contentWindow);
    }

    private void showStrip(ObjectArrayList<TabHandle> tabs) {

        if (stripTabs.equals(tabs))
            return;

        stripTabs.clear();
        stripTabs.addAll(tabs);
        menuManager.ejectAll(chromeMenu, EditorSetting.TAB_ENTRY_STRIP);

        for (int i = 0; i < tabs.size(); i++)
            injectStripEntry(tabs.get(i));
    }

    private void injectStripEntry(TabHandle tab) {
        menuManager.inject(
                chromeMenu, EditorSetting.TAB_ENTRY_STRIP, EditorSetting.MENU_TAB_STRIP_ENTRY,
                entry -> {
                    entry.setOnDragArgOverride(tab.getTabTitle());
                    entry.findChildById(EditorSetting.ELEMENT_TAB_STRIP_LABEL).setFontText(tab.getTabTitle());
                    entry.findChildById(EditorSetting.ELEMENT_TAB_STRIP_CLOSE).setActionArgOverride(tab.getTabTitle());

                    if (tab != ownerHandle)
                        return;

                    entry.setColorOverride(activeStripColor);
                    entry.findChildById(EditorSetting.ELEMENT_TAB_STRIP_ACCENT)
                            .setColorOverride(activeStripAccentColor);
                });
    }

    // Accessible \\

    public MenuInstance getChromeMenu() {
        return chromeMenu;
    }

    public ContextPackage getContentContext() {
        return contentContext;
    }
}
