package editor.bootstrap.tabpipeline.tabmanager;

import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.layoutmanager.LayoutManager;
import editor.bootstrap.tabpipeline.tab.TabContext;
import editor.bootstrap.tabpipeline.tab.TabData;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.util.DropZone;
import engine.editor.EditorSecondaryWindowContext;
import engine.editor.EditorSetting;
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
     * Coordinates tab registration, BSP bookkeeping, and rect propagation
     * across every editor OS window. The main window and every secondary
     * window are registered through the same registerOsWindow() and publish
     * their dock through the same EditorDockSystem, so no path here branches
     * on which window a tab lives in.
     *
     * Each structural operation has exactly one owner:
     *
     * openTab() — register, create chrome + content on an OS window, add to BSP.
     * closeTab() — dispose the chrome window; TabContext.dispose() cascades
     * into everything else a tab owns exactly the same way regardless of
     * what triggered it.
     * dockTab() — reparent a tab onto an OS window and insert it into that
     * window's BSP, either at a leaf and zone or at the largest leaf. Tab
     * drops, drops into empty space, and every other move go through it.
     * openSecondaryOsWindow() — the one way a secondary editor OS window is
     * created, for a tab dropped into empty space and LayoutManager
     * restoring a saved session alike.
     * closeOsWindowIfEmpty() — the one rule for when a secondary window goes
     * away: it has no tabs left. The main window never closes here.
     *
     * pushRects() and notifyLayoutChanged() are the only call sites for
     * rect propagation and layout persistence respectively. Both silently
     * no-op while batching is active — see beginBatch()/endBatch() — so
     * every caller can invoke either one unconditionally after any
     * structural change.
     *
     * update() pushes every tab's current canvas bounds to its content window
     * after the chrome menus have rendered for the frame.
     */
    // Palette
    private Object2IntOpenHashMap<String> tabName2TabID;
    private Int2ObjectOpenHashMap<TabHandle> tabID2TabHandle;
    // Active
    private ObjectArrayList<TabHandle> openTabs;
    // Counter
    private Object2IntOpenHashMap<Class<? extends ContextPackage>> classInstanceCounter;
    // Dock Rects — one float[4] {x, y, w, h} per registered OS window
    private Object2ObjectOpenHashMap<WindowInstance, float[]> osWindow2DockRect;
    // Batch — suppresses pushRects()/notifyLayoutChanged() side effects while
    // many structural changes happen back to back
    private boolean batching;
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
        osWindow2DockRect = new Object2ObjectOpenHashMap<>();
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
    }

    @Override
    protected void awake() {
        registerOsWindow(windowManager.getMainWindow());
    }

    @Override
    protected void update() {
        Object[] elements = openTabs.elements();
        int size = openTabs.size();
        for (int i = 0; i < size; i++)
            ((TabHandle) elements[i]).getTabContext().syncContent();
    }

    // Management \\
    public TabHandle openPreview(WindowInstance osWindow) {
        return openTab(EngineSetting.TAB_TITLE_PREVIEW, application.runtime.RuntimeContext.class, osWindow);
    }

    public TabHandle openDev(WindowInstance osWindow) {
        return openTab(EngineSetting.TAB_TITLE_DEV, editor.dev.DevContext.class, osWindow);
    }

    public TabHandle openHierarchy(WindowInstance osWindow) {
        return openTab(EditorSetting.TAB_TITLE_HIERARCHY, editor.hierarchy.HierarchyContext.class, osWindow);
    }

    public TabHandle openItemEditor(WindowInstance osWindow) {
        return openTab(EditorSetting.TAB_TITLE_ITEM_EDITOR, editor.itemeditor.ItemEditorContext.class, osWindow);
    }

    public TabHandle openTextureViewer(WindowInstance osWindow) {
        return openTab(
                EditorSetting.TAB_TITLE_TEXTURE_VIEWER,
                editor.textureviewer.TextureViewerContext.class,
                osWindow);
    }

    /*
     * Registers a new tab on the given OS window. Creates chrome and content
     * windows composited onto it, pairs them, adds the tab to that window's
     * BSP, and pushes rects.
     */
    public TabHandle openTab(
            String baseTitle,
            Class<? extends ContextPackage> contentClass,
            WindowInstance osWindow) {
        if (baseTitle == null)
            throwException("Cannot open a tab with a null title.");
        if (contentClass == null)
            throwException("Cannot open tab '" + baseTitle + "' without a content context class.");
        if (!osWindow2DockRect.containsKey(osWindow))
            throwException("Cannot open tab '" + baseTitle + "' on an unregistered editor OS window.");
        int instance = classInstanceCounter.getInt(contentClass) + 1;
        classInstanceCounter.put(contentClass, instance);
        String title = baseTitle + " " + instance;
        if (hasTab(title))
            throwException("Tab title collision: " + title);
        // Chrome window
        WindowInstance tabWindow = windowManager.createLogicalWindow(title, osWindow);
        tabWindow.setCaptureEligible(false);
        tabWindow.setFocusIndependent(true);
        // Content window
        WindowInstance contentWindow = windowManager.createLogicalWindow(title, osWindow);
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
        dockLayoutSystem.addTab(osWindow, handle);
        pushRects();
        notifyLayoutChanged();
        return handle;
    }

    /*
     * Closes a tab by disposing its chrome window. TabContext.dispose() —
     * triggered as part of that — removes the tab from the BSP, disposes
     * the content window, and deregisters this tab from TabManager's own
     * tables. The OS window it lived on closes too if that left it empty.
     */
    public void closeTab(TabHandle handle) {
        if (handle == null)
            throwException("Cannot close a null tab handle.");
        if (!handle.isOpen())
            throwException("Cannot close tab that is not open: " + handle.getTabTitle());
        WindowInstance osWindow = handle.getTabContext().getWindow().getGLWindow();
        handle.getTabContext().getWindow().dispose();
        closeOsWindowIfEmpty(osWindow);
        pushRects();
        notifyLayoutChanged();
    }

    /*
     * Closes every secondary OS window — which cascades into every tab on it —
     * then every tab left on the main window. Leaves the editor with only the
     * empty main window, ready for LayoutManager to restore into.
     */
    public void closeAll() {
        ObjectArrayList<WindowInstance> osWindows = new ObjectArrayList<>(osWindow2DockRect.keySet());
        for (int i = 0; i < osWindows.size(); i++)
            closeOsWindow(osWindows.get(i));
        ObjectArrayList<TabHandle> remaining = new ObjectArrayList<>(openTabs);
        for (int i = 0; i < remaining.size(); i++)
            closeTab(remaining.get(i));
    }

    /*
     * Removes the given tab from every bookkeeping table TabManager owns.
     * Called exactly once, from within TabContext.dispose(), regardless of
     * what triggered that dispose. Safe to call more than once — a second
     * call for a handle that's already gone is a no-op.
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
     * Moves a tab that is currently out of every BSP onto the target OS window
     * and docks it there — split into the given leaf at the given zone, or
     * into the window's largest leaf when no leaf is given. The OS window the
     * tab came from closes if that left it empty.
     */
    public void dockTab(TabHandle handle, WindowInstance targetOsWindow, DockNodeStruct leaf, DropZone zone) {
        if (handle == null)
            throwException("Cannot dock a null tab handle.");
        if (targetOsWindow == null)
            throwException("Cannot dock tab '" + handle.getTabTitle() + "' onto a null OS window.");
        WindowInstance sourceOsWindow = handle.getTabContext().getWindow().getGLWindow();
        handle.getTabContext().moveTo(targetOsWindow);
        if (leaf != null)
            dockLayoutSystem.addTabToLeaf(leaf, handle, zone);
        else
            dockLayoutSystem.addTab(targetOsWindow, handle);
        closeOsWindowIfEmpty(sourceOsWindow);
        pushRects();
        notifyLayoutChanged();
    }

    public void openSecondaryWindowForTab(TabHandle handle) {
        dockTab(handle, openSecondaryOsWindow(), null, null);
    }

    // OS Window Lifecycle \\
    /*
     * The one and only way a secondary editor OS window is ever created. The
     * window runs EditorSecondaryWindowContext — the same dock as the main
     * window, filling the whole window, with no toolbar.
     */
    public WindowInstance openSecondaryOsWindow() {
        WindowInstance osWindow = windowManager.openWindow(
                EngineSetting.WINDOW_TITLE_EDITOR_SECONDARY,
                EditorSecondaryWindowContext.class);
        registerOsWindow(osWindow);
        return osWindow;
    }

    /*
     * Registers the dock tree and dock rect for an editor OS window, and wires
     * a dispose listener that unregisters both the moment the window is torn
     * down — whether via closeOsWindow(), the platform's own window-close
     * button, or engine shutdown — and marks the layout changed so a window
     * closed from its title bar is not restored with the next session. The
     * dock rect starts empty so nothing is placed until the window's dock
     * canvas has been measured.
     */
    private void registerOsWindow(WindowInstance osWindow) {
        dockLayoutSystem.initWindow(osWindow);
        osWindow2DockRect.put(osWindow, new float[4]);
        osWindow.setDisposeListener(() -> {
            dockLayoutSystem.removeWindow(osWindow);
            osWindow2DockRect.remove(osWindow);
            notifyLayoutChanged();
        });
    }

    public boolean isOsWindowEmpty(WindowInstance osWindow) {
        Object[] elements = openTabs.elements();
        int size = openTabs.size();
        for (int i = 0; i < size; i++) {
            TabHandle h = (TabHandle) elements[i];
            if (h.getTabContext().getWindow().getGLWindow() == osWindow)
                return false;
        }
        return true;
    }

    public void closeOsWindowIfEmpty(WindowInstance osWindow) {
        if (isOsWindowEmpty(osWindow))
            closeOsWindow(osWindow);
    }

    /*
     * Disposes the OS window. Everything composited onto it — every tab's
     * chrome and content windows, its toolbar, any dialog or drag ghost —
     * is torn down automatically as part of that single dispose() call, and
     * this window's own dispose listener unregisters its dock tree and dock
     * rect at the same time. The main window is never closed here.
     */
    public void closeOsWindow(WindowInstance osWindow) {
        if (osWindow == null || osWindow == windowManager.getMainWindow())
            return;
        windowManager.destroyOsWindow(osWindow);
    }

    // Rect Propagation \\
    /*
     * Called every frame by each editor window with its measured dock canvas.
     * Pushes rects only when the canvas actually changed.
     */
    public void setDockRect(WindowInstance osWindow, float x, float y, float w, float h) {
        float[] rect = osWindow2DockRect.get(osWindow);
        if (rect == null)
            throwException("Dock rect published for an unregistered editor OS window.");
        if (rect[0] == x && rect[1] == y && rect[2] == w && rect[3] == h)
            return;
        rect[0] = x;
        rect[1] = y;
        rect[2] = w;
        rect[3] = h;
        pushRects();
    }

    /*
     * Recomputes BSP rects for every registered OS window then calls
     * placeAt() on each tab — chrome and content are positioned together in
     * that single call. No-ops while batching is active; endBatch() calls
     * the underlying computation directly exactly once after the batch ends.
     */
    public void pushRects() {
        if (batching)
            return;
        computeAndPlaceRects();
    }

    private void computeAndPlaceRects() {
        for (Object2ObjectOpenHashMap.Entry<WindowInstance, float[]> entry : osWindow2DockRect
                .object2ObjectEntrySet()) {
            float[] r = entry.getValue();
            if (r[2] <= 0 || r[3] <= 0)
                continue;
            dockLayoutSystem.computeRects(entry.getKey(), r[0], r[1], r[2], r[3]);
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
     * Routes to LayoutManager, which writes the session once at the end of the
     * frame. Called after every structural mutation and after a divider drag
     * completes. No-ops while batching is active, for the same reason
     * pushRects() does.
     */
    public void notifyLayoutChanged() {
        if (batching)
            return;
        layoutManager.notifyLayoutChanged();
    }

    /*
     * Suppresses pushRects()/notifyLayoutChanged() side effects from every
     * structural call until endBatch() is called. Used by LayoutManager to
     * restore a whole saved session as a single operation instead of
     * recomputing rects and re-saving the session file once per tab along
     * the way. Not designed to nest — nothing in the editor needs more than
     * one batch active at a time.
     */
    public void beginBatch() {
        batching = true;
    }

    public void endBatch() {
        batching = false;
        computeAndPlaceRects();
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
}
