package editor.bootstrap.tabmanager;

import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tab.TabContext;
import editor.bootstrap.tab.TabData;
import editor.bootstrap.tab.TabHandle;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TabManager extends ManagerPackage {

    /*
     * Editor bootstrap manager that owns tab registration and tab lifecycle.
     * Both the TabContext shell and the content context are created as peers via
     * internal.createContext() — no parent/child nesting. The returned references
     * are wired together through mountContent so the tab shell can propagate
     * resize events to the content window without owning the content lifecycle.
     * Notifies DockLayoutSystem on every open and close so the BSP tree stays
     * in sync with the live tab set.
     *
     * Uniqueness is keyed on a generated instance title, not on the content
     * class. A per-class counter produces "Preview 1", "Preview 2", etc. so
     * any number of tabs of the same type can be open simultaneously.
     *
     * Depth: tab logical windows are depth 1 — chrome draws on top of content.
     * Content logical windows are depth 0 — they composite directly to mainWindow
     * and are drawn before chrome. Both logical windows share mainWindow as their
     * composite target so their FBOs land in the same blit queue and are drawn
     * in depth order.
     */

    // Palette
    private Object2IntOpenHashMap<String> tabName2TabID;
    private Int2ObjectOpenHashMap<TabHandle> tabID2TabHandle;

    // Active
    private ObjectArrayList<TabHandle> openTabs;

    // Counter — tracks how many instances of each content class have been
    // opened so every generated title is unique for the lifetime of the session.
    private Object2IntOpenHashMap<Class<? extends ContextPackage>> classInstanceCounter;

    // Internal
    private WindowManager windowManager;
    private DockLayoutSystem dockLayoutSystem;

    // Internal \\

    @Override
    protected void create() {

        // Palette
        this.tabName2TabID = new Object2IntOpenHashMap<>();
        this.tabName2TabID.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.tabID2TabHandle = new Int2ObjectOpenHashMap<>();

        // Active
        this.openTabs = new ObjectArrayList<>();

        // Counter
        this.classInstanceCounter = new Object2IntOpenHashMap<>();
        this.classInstanceCounter.defaultReturnValue(0);
    }

    @Override
    protected void get() {

        // Internal
        this.windowManager = get(WindowManager.class);
        this.dockLayoutSystem = get(DockLayoutSystem.class);
    }

    // Management \\

    public TabHandle openPreview() {
        return openTab(EngineSetting.TAB_TITLE_PREVIEW, application.runtime.RuntimeContext.class);
    }

    public WindowInstance openSecondaryWindow() {
        return windowManager.openWindow(
                EngineSetting.WINDOW_TITLE_EDITOR_SECONDARY,
                editor.runtime.EditorWindowSecondary.class);
    }

    // Accessible \\

    public TabHandle openTab(
            String baseTitle,
            Class<? extends ContextPackage> contentClass) {

        if (baseTitle == null)
            throwException("Cannot open a tab with a null title.");

        if (contentClass == null)
            throwException("Cannot open tab '" + baseTitle + "' without a content context class.");

        // Generate a unique instance title using a per-class counter.
        // "Preview" becomes "Preview 1", "Preview 2", etc.
        int instance = classInstanceCounter.getInt(contentClass) + 1;
        classInstanceCounter.put(contentClass, instance);
        String title = baseTitle + " " + instance;

        // Collision should never happen with the counter, but guard anyway.
        if (hasTab(title))
            throwException("Tab title collision (this is a bug): " + title);

        WindowInstance mainWindow = windowManager.getMainWindow();

        // tabWindow is depth 1 — chrome draws on top of content.
        WindowInstance tabWindow = windowManager.createLogicalWindow(title, mainWindow);
        tabWindow.setDepth(1);

        TabContext tabContext = internal.createContext(TabContext.class, tabWindow);

        // contentWindow composites directly to mainWindow at depth 0.
        // FBOs land in the mainWindow blit queue and are drawn before tab chrome
        // so content appears underneath the chrome overlay.
        WindowInstance contentWindow = windowManager.createLogicalWindow(title, mainWindow);
        contentWindow.setDepth(0);

        ContextPackage contentContext = internal.createContext(contentClass, contentWindow);

        tabContext.mountContent(contentWindow);

        TabHandle handle = create(TabHandle.class);
        handle.constructor(new TabData(title, contentClass));
        handle.mount(tabContext, contentContext);

        int tabID = RegistryUtility.toIntID(title);
        tabName2TabID.put(title, tabID);
        tabID2TabHandle.put(tabID, handle);
        openTabs.add(handle);

        dockLayoutSystem.addTab(handle);

        return handle;
    }

    public void closeTab(TabHandle handle) {

        if (handle == null)
            throwException("Cannot close a null tab handle.");

        if (!handle.isOpen())
            throwException("Cannot close tab because it is not open: " + handle.getTabTitle());

        dockLayoutSystem.removeTab(handle);

        TabContext tabContext = handle.getTabContext();
        ContextPackage contentContext = handle.getContentContext();

        tabContext.unmountContent();
        internal.destroyContext(contentContext);
        internal.destroyContext(tabContext);

        int tabID = getTabIDFromTabName(handle.getTabTitle());
        tabName2TabID.removeInt(handle.getTabTitle());
        tabID2TabHandle.remove(tabID);
        openTabs.remove(handle);
        handle.unmount();
    }

    public boolean hasTab(String name) {
        return tabName2TabID.containsKey(name);
    }

    public int getTabIDFromTabName(String name) {

        if (!tabName2TabID.containsKey(name))
            throwException("Tab name not found: " + name);

        return tabName2TabID.getInt(name);
    }

    public TabHandle getTabHandleFromTabID(int id) {

        TabHandle handle = tabID2TabHandle.get(id);

        if (handle == null)
            throwException("Tab ID not found: " + id);

        return handle;
    }

    public TabHandle getTabHandleFromTabName(String name) {
        return getTabHandleFromTabID(getTabIDFromTabName(name));
    }

    public ObjectArrayList<TabHandle> getOpenTabs() {
        return openTabs;
    }
}