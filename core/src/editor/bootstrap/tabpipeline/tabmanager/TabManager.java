package editor.bootstrap.tabpipeline.tabmanager;

import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.layoutmanager.LayoutManager;
import editor.bootstrap.tabpipeline.tab.TabContext;
import editor.bootstrap.tabpipeline.tab.TabData;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TabManager extends ManagerPackage {
    /*
     * Coordinates tab registration, BSP bookkeeping, and rect propagation.
     *
     * Two structural operations, both single call sites for tearing a tab
     * down or bringing one up:
     *
     * openTab() — register, create chrome + content, pair them, add to BSP.
     * closeTab() — dispose the chrome window; TabContext.dispose() cascades
     * into everything else a tab owns (content window, BSP membership,
     * TabManager's own bookkeeping via deregisterTab()) exactly the same
     * way whether closeTab() triggered it, the OS window it lived on was
     * closed by the user via the platform's own close button, or the whole
     * engine is shutting down. There is no second, hand-rolled teardown
     * path anywhere.
     *
     * openSecondaryOsWindow() is the one and only way a secondary editor OS
     * window is ever created — it registers the dock tree, the dock rect,
     * and a dispose listener that unregisters both automatically when the
     * window closes, however it closes. openSecondaryWindow(),
     * openSecondaryWindowForTab(), and LayoutManager's restore path all
     * call it, so none of them can drift out of sync with what "a window
     * that can host tabs" requires.
     *
     * moveTabToOsWindow() — reparent via TabContext.moveTo(), update BSP.
     *
     * pushRects() is the only call site for TabContext.placeAt(). Content
     * placement is fully handled inside placeAt() — no compositor sync needed.
     *
     * notifyLayoutChanged() is the single call site for layout persistence.
     * All structural mutations route through it. LayoutManager suppresses
     * re-entrant calls during restore.
     *
     * Every tab window resolves its own OS window via WindowInstance.getGLWindow()
     * — the only "what OS window is this on" logic that exists anywhere.
     */
    // Palette
    private Object2IntOpenHashMap<String> tabName2TabID;
    private Int2ObjectOpenHashMap<TabHandle> tabID2TabHandle;
    // Active
    private ObjectArrayList<TabHandle> openTabs;
    // Counter
    private Object2IntOpenHashMap<Class<? extends ContextPackage>> classInstanceCounter;
    // Dock Rects — one float[4] {x, y, w, h} per registered OS window
    private Object2ObjectOpenHashMap<WindowInstance, float[]> dockRects;
    // Internal
    private WindowManager windowManager;
    private DockLayoutSystem dockLayoutSystem;
    private InputManager inputManager;
    private LayoutManager layoutManager;

    // Internal \\
    @Override
    protected void create() {
        tabName2TabID = new Object2IntOpenHashMap<>();
        tabName2TabID.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        tabID2TabHandle = new Int2ObjectOpenHashMap<>();
        openTabs = new ObjectArrayList<>();
        classInstanceCounter = new Object2IntOpenHashMap<>();
        classInstanceCounter.defaultReturnValue(0);
        dockRects = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        windowManager = get(WindowManager.class);
        dockLayoutSystem = get(DockLayoutSystem.class);
        inputManager = get(InputManager.class);
        layoutManager = get(LayoutManager.class);
        inputManager.setAuthorityResolver(window -> {
            TabHandle tab = getTabHandleForWindow(window);
            return tab != null ? tab.getWindow() : window;
        });
        dockLayoutSystem.initWindow(windowManager.getMainWindow());
    }

    // Management \\
    public TabHandle openPreview() {
        return openTab(EngineSetting.TAB_TITLE_PREVIEW, application.runtime.RuntimeContext.class);
    }

    public WindowInstance openSecondaryWindow() {
        return openSecondaryOsWindow();
    }

    /*
     * Registers a new tab. Creates chrome and content windows under the main OS
     * window, pairs them, adds to BSP, and pushes rects.
     */
    public TabHandle openTab(String baseTitle, Class<? extends ContextPackage> contentClass) {
        if (baseTitle == null)
            throwException("Cannot open a tab with a null title.");
        if (contentClass == null)
            throwException("Cannot open tab '" + baseTitle + "' without a content context class.");
        int instance = classInstanceCounter.getInt(contentClass) + 1;
        classInstanceCounter.put(contentClass, instance);
        String title = baseTitle + " " + instance;
        if (hasTab(title))
            throwException("Tab title collision: " + title);
        WindowInstance mainWindow = windowManager.getMainWindow();
        // Chrome window
        WindowInstance tabWindow = windowManager.createLogicalWindow(title, mainWindow);
        tabWindow.setCaptureEligible(false);
        tabWindow.setFocusIndependent(true);
        // Content window
        WindowInstance contentWindow = windowManager.createLogicalWindow(title, mainWindow);
        contentWindow.setCaptureEligible(true);
        // Contexts
        TabContext tabContext = internal.createContext(TabContext.class, tabWindow);
        ContextPackage contentContext = internal.createContext(contentClass, contentWindow);
        tabContext.linkContent(contentContext);
        tabContext.bringToFront();
        // Handle
        TabHandle handle = create(TabHandle.class);
        handle.constructor(new TabData(baseTitle, title, contentClass));
        handle.mount(tabContext);
        tabContext.setOwnerHandle(handle);
        int tabID = RegistryUtility.toIntID(title);
        tabName2TabID.put(title, tabID);
        tabID2TabHandle.put(tabID, handle);
        openTabs.add(handle);
        dockLayoutSystem.addTab(mainWindow, handle);
        pushRects();
        notifyLayoutChanged();
        return handle;
    }

    /*
     * Closes a tab by disposing its chrome window. TabContext.dispose() —
     * triggered as part of that — removes the tab from the BSP, disposes
     * the content window, and deregisters this tab from TabManager's own
     * tables. Closes the source OS window afterward if it is now empty and
     * is not main.
     */
    public void closeTab(TabHandle handle) {
        if (handle == null)
            throwException("Cannot close a null tab handle.");
        if (!handle.isOpen())
            throwException("Cannot close tab that is not open: " + handle.getTabTitle());

        WindowInstance osWindow = handle.getTabContext().getWindow().getGLWindow();

        handle.getTabContext().getWindow().dispose();

        pushRects();
        notifyLayoutChanged();

        if (osWindow != windowManager.getMainWindow() && isOsWindowEmpty(osWindow))
            closeOsWindow(osWindow);
    }

    /*
     * Removes the given tab from every bookkeeping table TabManager owns.
     * Called exactly once, from within TabContext.dispose(), regardless of
     * what triggered that dispose. Safe to call more than once — a second
     * call for a handle that's already gone is a no-op, matching the same
     * idempotent-teardown guarantee WindowInstance.dispose() itself relies on.
     */
    public void deregisterTab(TabHandle handle) {
        if (handle == null || !openTabs.contains(handle))
            return;
        int tabID = getTabIDFromTabName(handle.getTabTitle());
        tabName2TabID.removeInt(handle.getTabTitle());
        tabID2TabHandle.remove(tabID);
        openTabs.remove(handle);
        handle.mount(null);
    }

    /*
     * Reparents chrome and content to the target OS window via TabContext.moveTo().
     * Ensures dockRects has a valid entry for the target. BSP insertion is handled
     * by the caller after this returns.
     */
    public void moveTabToOsWindow(TabHandle handle, WindowInstance targetOsWindow) {
        if (handle == null)
            throwException("Cannot move a null tab handle to an OS window.");
        if (targetOsWindow == null)
            throwException("Cannot move tab to a null OS window.");
        if (!dockRects.containsKey(targetOsWindow))
            dockRects.put(targetOsWindow,
                    new float[] { 0f, 0f, targetOsWindow.getWidth(), targetOsWindow.getHeight() });
        handle.getTabContext().moveTo(targetOsWindow);
    }

    /*
     * Opens a new OS window, registers its BSP tree and dockRects entry, adds
     * the tab to the new tree, then delegates to moveTabToOsWindow().
     */
    public void openSecondaryWindowForTab(TabHandle handle) {
        if (handle == null)
            throwException("Cannot open secondary window for null tab handle.");
        WindowInstance osWindow = openSecondaryOsWindow();
        dockLayoutSystem.addTab(osWindow, handle);
        moveTabToOsWindow(handle, osWindow);
        pushRects();
        notifyLayoutChanged();
    }

    /*
     * The one and only way a secondary editor OS window is ever created.
     * Registers the dock tree and dock rect for it, and wires a dispose
     * listener that unregisters both the moment the window is torn down —
     * whether via closeOsWindow(), the platform's own window-close button,
     * or engine shutdown. LayoutManager's restore path calls this too, so
     * a restored window is set up identically to a freshly opened one.
     */
    public WindowInstance openSecondaryOsWindow() {
        WindowInstance osWindow = windowManager.openWindow(
                EngineSetting.WINDOW_TITLE_EDITOR_SECONDARY,
                engine.editor.EditorWindowSecondary.class);
        dockLayoutSystem.initWindow(osWindow);
        dockRects.put(osWindow, new float[] { 0f, 0f, osWindow.getWidth(), osWindow.getHeight() });
        osWindow.setDisposeListener(() -> {
            dockLayoutSystem.removeWindow(osWindow);
            dockRects.remove(osWindow);
        });
        return osWindow;
    }

    // OS Window Lifecycle \\
    public boolean isOsWindowEmpty(WindowInstance osWindow) {
        Object[] elements = openTabs.elements();
        int size = openTabs.size();
        for (int i = 0; i < size; i++) {
            TabHandle h = (TabHandle) elements[i];
            if (h.getTabContext().getWindow().getCompositeTarget() == osWindow)
                return false;
        }
        return true;
    }

    /*
     * Disposes the OS window. Everything composited onto it — every tab's
     * chrome and content windows, any toolbar, any dialog or drag ghost —
     * is torn down automatically as part of that single dispose() call, and
     * this window's own dispose listener unregisters its dock tree and dock
     * rect at the same time. There is nothing left for this method to do
     * by hand.
     */
    public void closeOsWindow(WindowInstance osWindow) {
        if (osWindow == null || osWindow == windowManager.getMainWindow())
            return;
        windowManager.destroyOsWindow(osWindow);
    }

    // Rect Propagation \\
    public void setDockRect(WindowInstance osWindow, float x, float y, float w, float h) {
        if (osWindow == null)
            return;
        float[] rect = dockRects.get(osWindow);
        if (rect == null) {
            rect = new float[4];
            dockRects.put(osWindow, rect);
        }
        rect[0] = x;
        rect[1] = y;
        rect[2] = w;
        rect[3] = h;
        pushRects();
    }

    /*
     * The only call site for TabContext.placeAt(). Recomputes BSP rects for every
     * registered OS window then calls placeAt() on each tab — chrome and content
     * are positioned together in that single call.
     */
    public void pushRects() {
        for (Object2ObjectOpenHashMap.Entry<WindowInstance, float[]> entry : dockRects.object2ObjectEntrySet()) {
            WindowInstance osWindow = entry.getKey();
            float[] r = entry.getValue();
            if (r[2] <= 0 || r[3] <= 0)
                continue;
            dockLayoutSystem.computeRects(osWindow, r[0], r[1], r[2], r[3]);
        }
        Object[] elements = openTabs.elements();
        int size = openTabs.size();
        for (int i = 0; i < size; i++) {
            TabHandle h = (TabHandle) elements[i];
            WindowInstance osWindow = h.getTabContext().getWindow().getGLWindow();
            float x = dockLayoutSystem.getTabX(osWindow, h);
            float y = dockLayoutSystem.getTabY(osWindow, h);
            float w = dockLayoutSystem.getTabW(osWindow, h);
            float hh = dockLayoutSystem.getTabH(osWindow, h);
            h.getTabContext().placeAt(x, y, w, hh);
        }
    }

    // Layout \\
    /*
     * Routes to LayoutManager. Called by this manager after every structural
     * mutation and by TabDragManager after drop resolution. LayoutManager
     * suppresses calls during restore via its own restoring flag.
     */
    public void notifyLayoutChanged() {
        if (layoutManager != null)
            layoutManager.notifyLayoutChanged();
    }

    /*
     * Clears all tab instance counters. Called by LayoutManager before
     * re-opening tabs during restore so generated titles are deterministic.
     */
    public void resetCounters() {
        classInstanceCounter.clear();
    }

    // Lookup \\
    public TabHandle getTabHandleForWindow(WindowInstance window) {
        Object[] elements = openTabs.elements();
        int size = openTabs.size();
        for (int i = 0; i < size; i++) {
            TabHandle h = (TabHandle) elements[i];
            if (h.getTabContext().getWindow() == window)
                return h;
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