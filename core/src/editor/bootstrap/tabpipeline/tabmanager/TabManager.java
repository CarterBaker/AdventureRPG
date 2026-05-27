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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TabManager extends ManagerPackage {

    /*
     * Coordinates tab registration, BSP bookkeeping, and rect propagation.
     *
     * Three structural operations:
     * openTab() — register, create chrome + content, add to BSP
     * closeTab() — remove from BSP, destroy, deregister
     * moveTabToOsWindow() — reparent via TabContext.moveTo(), update BSP
     *
     * openSecondaryWindowForTab() opens a new OS window then delegates to
     * moveTabToOsWindow() — no special casing.
     *
     * pushRects() is the only call site for TabContext.placeAt(). Content
     * placement is fully handled inside placeAt() — no compositor sync needed.
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
        return windowManager.openWindow(
                EngineSetting.WINDOW_TITLE_EDITOR_SECONDARY,
                editor.runtime.editor.EditorWindowSecondary.class);
    }

    /*
     * Registers a new tab. Creates chrome and content windows under the main OS
     * window, links them, adds to BSP, and pushes rects.
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
        tabWindow.setDepth(EngineSetting.TAB_DEFAULT_TAB_DEPTH);
        tabWindow.setCaptureEligible(false);
        tabWindow.setFocusIndependent(true);

        // Content window
        WindowInstance contentWindow = windowManager.createLogicalWindow(title, mainWindow);
        contentWindow.setDepth(EngineSetting.TAB_DEFAULT_CONTENT_DEPTH);

        // Contexts
        TabContext tabContext = internal.createContext(TabContext.class, tabWindow);
        ContextPackage contentContext = internal.createContext(contentClass, contentWindow);

        tabContext.linkContent(contentContext);

        // Handle
        TabHandle handle = create(TabHandle.class);
        handle.constructor(new TabData(title, contentClass));
        handle.mount(tabContext);

        int tabID = RegistryUtility.toIntID(title);
        tabName2TabID.put(title, tabID);
        tabID2TabHandle.put(tabID, handle);
        openTabs.add(handle);

        dockLayoutSystem.addTab(mainWindow, handle);
        pushRects();

        return handle;
    }

    /*
     * Removes from BSP, destroys both contexts and windows, deregisters.
     * Closes the source OS window if it is now empty and is not main.
     */
    public void closeTab(TabHandle handle) {

        if (handle == null)
            throwException("Cannot close a null tab handle.");

        if (!handle.isOpen())
            throwException("Cannot close tab that is not open: " + handle.getTabTitle());

        TabContext tabContext = handle.getTabContext();
        WindowInstance osWindow = resolveOsWindow(tabContext.getWindow());

        dockLayoutSystem.removeTab(osWindow, handle);

        ContextPackage contentContext = tabContext.getContentContext();
        if (contentContext != null) {
            contentContext.getWindow().getMenuListHandle().setLockReleaseListener(null);
            WindowInstance contentWindow = contentContext.getWindow();
            internal.destroyContext(contentContext);
            windowManager.removeWindow(contentWindow);
        }

        WindowInstance tabWindow = tabContext.getWindow();
        internal.destroyContext(tabContext);
        windowManager.removeWindow(tabWindow);

        handle.mount(null);

        windowManager.unlockHoveredWindow();

        int tabID = getTabIDFromTabName(handle.getTabTitle());
        tabName2TabID.removeInt(handle.getTabTitle());
        tabID2TabHandle.remove(tabID);
        openTabs.remove(handle);

        pushRects();

        if (osWindow != windowManager.getMainWindow() && isOsWindowEmpty(osWindow))
            closeOsWindow(osWindow);
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

        WindowInstance osWindow = windowManager.openWindow(
                EngineSetting.WINDOW_TITLE_EDITOR_SECONDARY,
                editor.runtime.editor.EditorWindowSecondary.class);

        dockLayoutSystem.initWindow(osWindow);
        dockRects.put(osWindow, new float[] { 0f, 0f, osWindow.getWidth(), osWindow.getHeight() });
        dockLayoutSystem.addTab(osWindow, handle);

        moveTabToOsWindow(handle, osWindow);

        pushRects();
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

    public void closeOsWindow(WindowInstance osWindow) {

        if (osWindow == null || osWindow == windowManager.getMainWindow())
            return;

        dockLayoutSystem.removeWindow(osWindow);
        dockRects.remove(osWindow);

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
            WindowInstance osWindow = resolveOsWindow(h.getTabContext().getWindow());

            float x = dockLayoutSystem.getTabX(osWindow, h);
            float y = dockLayoutSystem.getTabY(osWindow, h);
            float w = dockLayoutSystem.getTabW(osWindow, h);
            float hh = dockLayoutSystem.getTabH(osWindow, h);

            h.getTabContext().placeAt(x, y, w, hh);
        }
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

    // Utility \\

    private WindowInstance resolveOsWindow(WindowInstance window) {

        if (window.hasNativeHandle())
            return window;

        WindowInstance composite = window.getCompositeTarget();

        if (composite != null && composite.hasNativeHandle())
            return composite;

        return windowManager.getMainWindow();
    }
}