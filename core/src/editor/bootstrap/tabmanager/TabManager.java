package editor.bootstrap.tabmanager;

import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tab.TabContext;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class TabManager extends ManagerPackage {

    /*
     * Editor bootstrap manager that owns tab registration and tab lifecycle.
     * The manager opens a TabContext shell on a logical window, opens the child
     * content ContextPackage on its own logical window, and mounts the child
     * into the tab shell for per-frame compositing.
     */

    // Palette
    private Object2IntOpenHashMap<String> tabName2TabID;
    private Int2ObjectOpenHashMap<TabHandle> tabID2TabHandle;

    // Branch
    private EditorBranch editorBranch;

    // Internal
    private WindowManager windowManager;

    // Internal \\

    @Override
    protected void create() {

        // Palette
        this.tabName2TabID = new Object2IntOpenHashMap<>();
        this.tabName2TabID.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.tabID2TabHandle = new Int2ObjectOpenHashMap<>();

        // Branch
        create(EditorBranch.class);
    }

    @Override
    protected void get() {

        // Branch
        this.editorBranch = get(EditorBranch.class);

        // Internal
        this.windowManager = get(WindowManager.class);
    }

    // Management \\

    TabHandle openPreview() {
        return openTab(EngineSetting.TAB_TITLE_PREVIEW, application.runtime.RuntimeContext.class);
    }

    WindowInstance openSecondaryWindow() {
        WindowInstance window = windowManager.openWindow(
                EngineSetting.WINDOW_TITLE_EDITOR_SECONDARY,
                editor.runtime.EditorWindowSecondary.class);
        editorBranch.openSecondaryMenu(window);
        return window;
    }

    // Accessible \\

    public TabHandle openTab(
            String title,
            Class<? extends ContextPackage> contentClass) {

        if (title == null)
            throwException("Cannot open a tab with a null title.");

        if (contentClass == null)
            throwException("Cannot open tab '" + title + "' without a content context class.");

        if (hasTab(title))
            throwException("Tab is already open: " + title);

        WindowInstance mainWindow = windowManager.getMainWindow();
        WindowInstance tabWindow = windowManager.createLogicalWindow(title, mainWindow);
        TabContext tabContext = internal.createTabContext(TabContext.class, tabWindow);

        WindowInstance contentWindow = windowManager.createLogicalWindow(title, tabWindow);
        ContextPackage contentContext = internal.createChildContext(tabContext, contentClass, contentWindow);

        tabContext.mountContent(contentWindow, contentContext);

        TabHandle handle = create(TabHandle.class);
        handle.constructor(new TabData(title, contentClass));
        handle.mount(tabContext, contentContext);

        int tabID = RegistryUtility.toIntID(title);
        tabName2TabID.put(title, tabID);
        tabID2TabHandle.put(tabID, handle);

        return handle;
    }

    public void closeTab(TabHandle handle) {

        if (handle == null)
            throwException("Cannot close a null tab handle.");

        if (!handle.isOpen())
            throwException("Cannot close tab because it is not open: " + handle.getTabTitle());

        TabContext tabContext = handle.getTabContext();
        ContextPackage contentContext = handle.getContentContext();
        WindowInstance tabWindow = tabContext.getWindow();
        WindowInstance contentWindow = contentContext.getWindow();

        tabContext.unmountContent();
        internal.destroyContext(contentContext);
        windowManager.removeWindow(contentWindow);
        internal.destroyContext(tabContext);
        windowManager.removeWindow(tabWindow);

        int tabID = getTabIDFromTabName(handle.getTabTitle());
        tabName2TabID.removeInt(handle.getTabTitle());
        tabID2TabHandle.remove(tabID);
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
}
