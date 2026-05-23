package editor.bootstrap.tabpipeline.tabmanager;

import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.tab.TabContext;
import editor.bootstrap.tabpipeline.tab.TabData;
import editor.bootstrap.tabpipeline.tab.TabHandle;
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
     * are wired via linkContent() so TabContext.onResize() can cascade canvas
     * bounds down to the content window.
     * Notifies DockLayoutSystem on every open and close so the BSP tree stays
     * in sync with the live tab set.
     *
     * Uniqueness is keyed on a generated instance title, not on the content
     * class. A per-class counter produces "Preview 1", "Preview 2", etc. so
     * any number of tabs of the same type can be open simultaneously.
     *
     * Rect propagation fires only on structural changes — tab opened, tab
     * closed, dock canvas resized. The compositor calls setDockRect() on
     * resize. openTab() and closeTab() call pushRects() directly.
     *
     * Authority resolution: TabManager pushes a resolver lambda into InputSystem
     * once in get() so the kernel never names any editor type.
     *
     * getDockLayoutSystem() is package-accessible to TabDragSystem so drag
     * resolution can call findLeafAt() and addTabToLeaf() without TabManager
     * re-exposing every DockLayoutSystem method.
     *
     * isOsWindowEmpty() walks open tabs and checks whether any tab's chrome
     * window resolves to the given OS window. Used by TabDragSystem after
     * removeTab() to decide whether to close a vacated secondary window.
     *
     * closeOsWindow() destroys the platform window and removes all logical
     * windows that composite into it. Safe to call only when the window has
     * no remaining tabs.
     *
     * openSecondaryWindowForTab() opens a new OS window and registers the
     * given handle into a fresh BSP tree rooted in that window. Called by
     * TabDragSystem when a tab is dropped outside all existing OS windows.
     */

    // Palette
    private Object2IntOpenHashMap<String> tabName2TabID;
    private Int2ObjectOpenHashMap<TabHandle> tabID2TabHandle;

    // Active
    private ObjectArrayList<TabHandle> openTabs;

    // Counter
    private Object2IntOpenHashMap<Class<? extends ContextPackage>> classInstanceCounter;

    // Dock Rect
    private float dockX;
    private float dockY;
    private float dockW;
    private float dockH;

    // Internal
    private WindowManager windowManager;
    private DockLayoutSystem dockLayoutSystem;
    private InputManager inputManager;

    // Internal \\

    @Override
    protected void create() {

        // Palette
        tabName2TabID = new Object2IntOpenHashMap<>();
        tabName2TabID.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        tabID2TabHandle = new Int2ObjectOpenHashMap<>();

        // Active
        openTabs = new ObjectArrayList<>();

        // Counter
        classInstanceCounter = new Object2IntOpenHashMap<>();
        classInstanceCounter.defaultReturnValue(0);
    }

    @Override
    protected void get() {

        // Internal
        windowManager = get(WindowManager.class);
        dockLayoutSystem = get(DockLayoutSystem.class);
        inputManager = get(InputManager.class);

        inputManager.setAuthorityResolver(window -> {
            TabHandle tab = getTabHandleForWindow(window);
            return tab != null ? tab.getWindow() : window;
        });
    }

    // Management \\

    public TabHandle openPreview() {
        return openTab(EngineSetting.TAB_TITLE_PREVIEW, application.runtime.RuntimeContext.class);
    }

    public WindowInstance openSecondaryWindow() {
        return windowManager.openWindow(
                EngineSetting.WINDOW_TITLE_EDITOR_SECONDARY,
                editor.runtime.editor.EditorWindowSecondary.class);
    }

    // Accessible \\

    public TabHandle openTab(
            String baseTitle,
            Class<? extends ContextPackage> contentClass) {

        if (baseTitle == null)
            throwException("Cannot open a tab with a null title.");

        if (contentClass == null)
            throwException("Cannot open tab '" + baseTitle + "' without a content context class.");

        int instance = classInstanceCounter.getInt(contentClass) + 1;
        classInstanceCounter.put(contentClass, instance);
        String title = baseTitle + " " + instance;

        if (hasTab(title))
            throwException("Tab title collision (this is a bug): " + title);

        WindowInstance mainWindow = windowManager.getMainWindow();

        WindowInstance tabWindow = windowManager.createLogicalWindow(title, mainWindow);
        tabWindow.setDepth(1);

        TabContext tabContext = internal.createContext(TabContext.class, tabWindow);

        WindowInstance contentWindow = windowManager.createLogicalWindow(title, mainWindow);
        contentWindow.setDepth(2);

        ContextPackage contentContext = internal.createContext(contentClass, contentWindow);

        tabContext.linkContent(contentContext);

        TabHandle handle = create(TabHandle.class);
        handle.constructor(new TabData(title, contentClass));
        handle.mount(tabContext, contentContext);

        contentWindow.getMenuListHandle().setLockReleaseListener(
                () -> inputManager.onInputLockReleased(contentWindow));

        int tabID = RegistryUtility.toIntID(title);
        tabName2TabID.put(title, tabID);
        tabID2TabHandle.put(tabID, handle);
        openTabs.add(handle);

        dockLayoutSystem.addTab(handle);
        pushRects();

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

        WindowInstance tabWindow = tabContext.getWindow();
        WindowInstance contentWindow = contentContext.getWindow();

        contentWindow.getMenuListHandle().setLockReleaseListener(null);

        internal.destroyContext(contentContext);
        internal.destroyContext(tabContext);

        windowManager.removeWindow(contentWindow);
        windowManager.removeWindow(tabWindow);

        windowManager.unlockHoveredWindow();

        int tabID = getTabIDFromTabName(handle.getTabTitle());
        tabName2TabID.removeInt(handle.getTabTitle());
        tabID2TabHandle.remove(tabID);
        openTabs.remove(handle);

        pushRects();
    }

    public void openSecondaryWindowForTab(TabHandle handle) {

        if (handle == null)
            throwException("Cannot open secondary window for null tab handle.");

        WindowInstance osWindow = windowManager.openWindow(
                EngineSetting.WINDOW_TITLE_EDITOR_SECONDARY,
                editor.runtime.editor.EditorWindowSecondary.class);

        WindowInstance tabWindow = windowManager.createLogicalWindow(
                handle.getTabTitle(), osWindow);
        tabWindow.setDepth(1);

        TabContext tabContext = internal.createContext(TabContext.class, tabWindow);

        WindowInstance contentWindow = windowManager.createLogicalWindow(
                handle.getTabTitle(), osWindow);
        contentWindow.setDepth(2);

        tabContext.linkContent(handle.getContentContext());
        handle.mount(tabContext, handle.getContentContext());

        contentWindow.getMenuListHandle().setLockReleaseListener(
                () -> inputManager.onInputLockReleased(contentWindow));

        dockLayoutSystem.addTab(handle);
        pushRects();
    }

    public boolean isOsWindowEmpty(WindowInstance osWindow) {

        Object[] elements = openTabs.elements();
        int size = openTabs.size();

        for (int i = 0; i < size; i++) {

            TabHandle handle = (TabHandle) elements[i];
            WindowInstance tabWindow = handle.getTabContext().getWindow();
            WindowInstance composite = tabWindow.getCompositeTarget();

            if (composite == osWindow)
                return false;
        }

        return true;
    }

    public void closeOsWindow(WindowInstance osWindow) {

        if (osWindow == null)
            return;

        if (osWindow == windowManager.getMainWindow())
            return;

        ObjectArrayList<WindowInstance> windows = windowManager.getWindows();
        ObjectArrayList<WindowInstance> toRemove = new ObjectArrayList<>();

        Object[] elements = windows.elements();
        int size = windows.size();

        for (int i = 0; i < size; i++) {
            WindowInstance w = (WindowInstance) elements[i];
            if (w.getCompositeTarget() == osWindow)
                toRemove.add(w);
        }

        Object[] removeElements = toRemove.elements();
        int removeSize = toRemove.size();

        for (int i = 0; i < removeSize; i++)
            windowManager.removeWindow((WindowInstance) removeElements[i]);

        windowManager.removeWindow(osWindow);
    }

    public void setDockRect(float x, float y, float w, float h) {

        dockX = x;
        dockY = y;
        dockW = w;
        dockH = h;

        pushRects();
    }

    public void pushRects() {

        if (dockW <= 0 || dockH <= 0)
            return;

        dockLayoutSystem.computeRects(dockX, dockY, dockW, dockH);

        Object[] elements = openTabs.elements();
        int size = openTabs.size();

        for (int i = 0; i < size; i++) {

            TabHandle handle = (TabHandle) elements[i];

            float x = dockLayoutSystem.getTabX(handle);
            float y = dockLayoutSystem.getTabY(handle);
            float w = dockLayoutSystem.getTabW(handle);
            float h = dockLayoutSystem.getTabH(handle);

            WindowInstance tabWindow = handle.getTabContext().getWindow();
            tabWindow.setCompositeRect(x, y, w, h);
            tabWindow.resize((int) w, (int) h);

            WindowInstance contentWindow = handle.getContentContext().getWindow();
            contentWindow.setCompositeRect(x, y, w, h);
        }
    }

    public TabHandle getTabHandleForWindow(WindowInstance window) {

        Object[] elements = openTabs.elements();
        int size = openTabs.size();

        for (int i = 0; i < size; i++) {
            TabHandle handle = (TabHandle) elements[i];
            if (handle.getTabContext().getWindow() == window)
                return handle;
        }

        return null;
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

    public DockLayoutSystem getDockLayoutSystem() {
        return dockLayoutSystem;
    }
}