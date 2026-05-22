package editor.bootstrap.tabmanager;

import application.kernel.inputpipeline.inputmanager.InputManager;
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
     * are wired via linkContent() so TabContext.onResize() can cascade canvas
     * bounds down to the content window, the same way EditorTabCompositorSystem
     * cascades editor canvas bounds down to tab windows.
     * Notifies DockLayoutSystem on every open and close so the BSP tree stays
     * in sync with the live tab set.
     *
     * Uniqueness is keyed on a generated instance title, not on the content
     * class. A per-class counter produces "Preview 1", "Preview 2", etc. so
     * any number of tabs of the same type can be open simultaneously.
     *
     * FboRenderSystem sort key = windowDepth * LAYER_STRIDE + layer, so this
     * ordering is automatically enforced each frame without any manual sorting
     * at this level.
     *
     * Rect propagation fires only on structural changes — tab opened, tab
     * closed, dock canvas resized. The compositor calls setDockRect() on
     * resize. openTab() and closeTab() call pushRects() directly using the
     * cached bounds so every tab window always holds a valid composite rect.
     *
     * Authority resolution: InputSystem is kernel-level and cannot reach up to
     * TabManager, so the dependency is inverted. TabManager pushes a resolver
     * lambda into InputSystem once in get() — the kernel sees only
     * WindowInstance in and WindowInstance out. getTabHandleForWindow() drives
     * that resolver and is also used by the lock-release listener registered on
     * each content window's MenuListHandle in openTab(), so capture is
     * automatically restored when a lock_input menu closes without a re-click.
     *
     * pushRects() is the sole authority for composite rect position on both the
     * tab chrome window and the content window. TabContext.onResize() must only
     * forward canvas dimensions (w, h) to the content context for rendering —
     * it must never call setCompositeRect on the content window because it does
     * not have authoritative absolute position. Without the correct (x, y) on
     * the content window, WindowManager.syncHoveredWindow() cannot resolve
     * hover into it, blocking focus, input routing, and menu raycast for every
     * tab beyond the first.
     */

    // Palette
    private Object2IntOpenHashMap<String> tabName2TabID;
    private Int2ObjectOpenHashMap<TabHandle> tabID2TabHandle;

    // Active
    private ObjectArrayList<TabHandle> openTabs;

    // Counter — tracks how many instances of each content class have been
    // opened so every generated title is unique for the lifetime of the session.
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

        // Invert the tab→input dependency: push a resolver lambda down into the
        // kernel so InputSystem can map a focused tab chrome window to its content
        // window without importing TabManager.
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

        // Tab window is depth 1 — chrome draws on top of the editor base layer.
        WindowInstance tabWindow = windowManager.createLogicalWindow(title, mainWindow);
        tabWindow.setDepth(1);

        TabContext tabContext = internal.createContext(TabContext.class, tabWindow);

        // Content window is depth 2 — draws on top of chrome.
        WindowInstance contentWindow = windowManager.createLogicalWindow(title, mainWindow);
        contentWindow.setDepth(2);
        tabWindow.setAlwaysHover(true);

        ContextPackage contentContext = internal.createContext(contentClass, contentWindow);

        // Bridge the two peer contexts so TabContext.onResize() can cascade
        // canvas dimensions down to the content window — same pattern as the
        // compositor cascading editor canvas bounds down to tab windows.
        // TabContext.onResize() must only forward (w, h); composite position
        // is owned exclusively by pushRects().
        tabContext.linkContent(contentContext);

        TabHandle handle = create(TabHandle.class);
        handle.constructor(new TabData(title, contentClass));
        handle.mount(tabContext, contentContext);

        // Register a lock-release listener on the content window so InputSystem
        // can restore capture automatically when a lock_input menu (e.g. inventory)
        // closes — without requiring a re-click from the user.
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

        // The hover lock is set by ElementHitSystem when the cursor enters the
        // close button. The tab is destroyed before hover exit fires, so the lock
        // is never released. Unlock here so syncHoveredWindow runs next frame and
        // hover resolves to a valid window.
        windowManager.unlockHoveredWindow();

        int tabID = getTabIDFromTabName(handle.getTabTitle());
        tabName2TabID.removeInt(handle.getTabTitle());
        tabID2TabHandle.remove(tabID);
        openTabs.remove(handle);

        pushRects();
    }

    public void setDockRect(float x, float y, float w, float h) {

        dockX = x;
        dockY = y;
        dockW = w;
        dockH = h;

        pushRects();
    }

    private void pushRects() {

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
            // ↑ resize cascades through TabContext.onResize → contentWindow.resize()
            // → contentContext.onResize(). Any composite rect reset inside
            // contentContext.onResize() fires here, before the line below.

            // Set composite rect on the content window after the cascade so
            // contentContext.onResize() cannot overwrite the correct (x, y).
            // The content window must have its real screen position for
            // WindowManager.syncHoveredWindow() to route hover into it —
            // without this every content window sits at (0, 0) and
            // WindowManager's tie-break permanently locks hover to tab 1.
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
}