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
     * Editor bootstrap manager that owns tab registration and tab lifecycle.
     * Both the TabContext shell and the content context are created as peers via
     * internal.createContext() — no parent/child nesting. The returned references
     * are wired via linkContent() so TabContext.onResize() can cascade canvas
     * bounds down to the content window.
     *
     * Notifies DockLayoutSystem on every open and close so each OS window's BSP
     * tree stays in sync with its live tab set. Every call to DockLayoutSystem
     * is window-scoped — the OS window is resolved via resolveOsWindow() before
     * being passed through.
     *
     * Uniqueness is keyed on a generated instance title, not on the content
     * class. A per-class counter produces "Preview 1", "Preview 2", etc. so
     * any number of tabs of the same type can be open simultaneously.
     *
     * Rect propagation fires only on structural changes — tab opened, tab
     * closed, dock canvas resized. Compositors call setDockRect() with their
     * OS window on canvas resize. openTab() and closeTab() call pushRects()
     * directly. dockRects holds one float[4] per registered OS window so
     * pushRects() can computeRects on each tree independently.
     *
     * Authority resolution: TabManager pushes a resolver lambda into InputSystem
     * once in get() so the kernel never names any editor type.
     *
     * isOsWindowEmpty() walks open tabs and checks whether any tab's chrome
     * window resolves to the given OS window. Used by TabDragManager after
     * removeTab() to decide whether to close a vacated secondary window.
     *
     * closeOsWindow() destroys the platform window and removes all logical
     * windows that composite into it. Calls DockLayoutSystem.removeWindow()
     * to clean up the BSP entry. Safe to call only when the window has no
     * remaining tabs.
     *
     * openSecondaryWindowForTab() opens a new OS window, registers a fresh BSP
     * tree via DockLayoutSystem.initWindow(), seeds the dock rect from the OS
     * window dimensions so pushRects() has valid data before the secondary
     * compositor fires for the first time, tears down the old TabContext and its
     * logical window, creates a new TabContext under the secondary OS window,
     * and re-parents the content window via setCompositeTarget().
     *
     * moveTabToOsWindow() re-parents an existing tab to a different OS window
     * that is already open and registered. Used by TabDragManager when a drag
     * is dropped onto an existing OS window rather than the void. Tears down the
     * old TabContext and its logical window, creates a fresh TabContext shell
     * under the target OS window, and re-parents the content window. Does not
     * touch the BSP — addTabToLeaf in executeDrop handles insertion. Ensures
     * dockRects has a valid entry for the target OS window so pushRects() does
     * not skip it on the same frame.
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

        // Register the main window's BSP tree immediately.
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
        tabWindow.setDepth(EngineSetting.TAB_DEFAULT_TAB_DEPTH);

        TabContext tabContext = internal.createContext(TabContext.class, tabWindow);

        WindowInstance contentWindow = windowManager.createLogicalWindow(title, mainWindow);
        contentWindow.setDepth(EngineSetting.TAB_DEFAULT_CONTENT_DEPTH);

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

        dockLayoutSystem.addTab(mainWindow, handle);
        pushRects();

        return handle;
    }

    public void closeTab(TabHandle handle) {

        if (handle == null)
            throwException("Cannot close a null tab handle.");

        if (!handle.isOpen())
            throwException("Cannot close tab because it is not open: " + handle.getTabTitle());

        WindowInstance osWindow = resolveOsWindow(handle.getTabContext().getWindow());
        dockLayoutSystem.removeTab(osWindow, handle);

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

        // Register a fresh BSP tree for this OS window before adding any tabs.
        dockLayoutSystem.initWindow(osWindow);

        // Seed the dock rect immediately so pushRects() at the end of this method
        // has valid dimensions before SecondaryTabCompositorSystem.update() fires
        // for the first time. Without this, computeRects is never called for this
        // window on the first frame and all tab rects are 0,0,0,0 — meaning
        // findLeafAt returns null on subsequent drags to this window, causing
        // executeDrop to open yet another new OS window instead of dropping into
        // the existing one.
        dockRects.put(osWindow, new float[] { 0f, 0f, osWindow.getWidth(), osWindow.getHeight() });

        // Tear down the old TabContext and its logical window. It still composites
        // to the source OS window and must not linger — it would render ghost
        // chrome on the wrong window and confuse syncContentRects in both
        // compositors.
        TabContext oldTabContext = handle.getTabContext();
        if (oldTabContext != null) {
            WindowInstance oldTabWindow = oldTabContext.getWindow();
            internal.destroyContext(oldTabContext);
            windowManager.removeWindow(oldTabWindow);
        }

        // Create a fresh TabContext shell under the secondary OS window.
        WindowInstance tabWindow = windowManager.createLogicalWindow(
                handle.getTabTitle(), osWindow);
        tabWindow.setDepth(EngineSetting.TAB_DEFAULT_TAB_DEPTH);

        TabContext tabContext = internal.createContext(TabContext.class, tabWindow);

        // Re-parent the existing content window to the secondary OS window so
        // its FBO blits route to the correct render target. The content context
        // itself is untouched — only the composite target changes.
        WindowInstance contentWindow = handle.getContentContext().getWindow();
        windowManager.reparentWindow(contentWindow, osWindow);
        contentWindow.setDepth(EngineSetting.TAB_DEFAULT_CONTENT_DEPTH);

        tabContext.linkContent(handle.getContentContext());
        handle.mount(tabContext, handle.getContentContext());

        contentWindow.getMenuListHandle().setLockReleaseListener(
                () -> inputManager.onInputLockReleased(contentWindow));

        dockLayoutSystem.addTab(osWindow, handle);
        pushRects();
    }

    public void moveTabToOsWindow(TabHandle handle, WindowInstance targetOsWindow) {

        if (handle == null)
            throwException("Cannot move a null tab handle to an OS window.");

        if (targetOsWindow == null)
            throwException("Cannot move tab to a null OS window.");

        // Ensure dockRects has an entry for the target OS window so pushRects()
        // does not skip it on the first frame after the move. If a compositor has
        // already registered the window the existing rect is preserved — only seed
        // when the key is absent.
        if (!dockRects.containsKey(targetOsWindow))
            dockRects.put(targetOsWindow,
                    new float[] { 0f, 0f, targetOsWindow.getWidth(), targetOsWindow.getHeight() });

        // Tear down the old TabContext and its logical window. It still composites
        // to the source OS window and must not linger — it would render ghost
        // chrome on the wrong window.
        TabContext oldTabContext = handle.getTabContext();
        if (oldTabContext != null) {
            WindowInstance oldTabWindow = oldTabContext.getWindow();
            internal.destroyContext(oldTabContext);
            windowManager.removeWindow(oldTabWindow);
        }

        // Create a fresh TabContext shell under the target OS window.
        WindowInstance tabWindow = windowManager.createLogicalWindow(
                handle.getTabTitle(), targetOsWindow);
        tabWindow.setDepth(EngineSetting.TAB_DEFAULT_TAB_DEPTH);

        TabContext tabContext = internal.createContext(TabContext.class, tabWindow);

        // Re-parent the existing content window to the target OS window so its
        // FBO blits route to the correct render target. The content context itself
        // is untouched — only the composite target changes.
        WindowInstance contentWindow = handle.getContentContext().getWindow();
        windowManager.reparentWindow(contentWindow, targetOsWindow);
        contentWindow.setDepth(EngineSetting.TAB_DEFAULT_CONTENT_DEPTH);

        tabContext.linkContent(handle.getContentContext());
        handle.mount(tabContext, handle.getContentContext());

        contentWindow.getMenuListHandle().setLockReleaseListener(
                () -> inputManager.onInputLockReleased(contentWindow));
    }

    public boolean isOsWindowEmpty(WindowInstance osWindow) {

        Object[] elements = openTabs.elements();
        int size = openTabs.size();

        for (int i = 0; i < size; i++) {

            TabHandle h = (TabHandle) elements[i];
            WindowInstance tabWindow = h.getTabContext().getWindow();
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

        windowManager.removeWindow(osWindow);
    }

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

            WindowInstance tabWindow = h.getTabContext().getWindow();
            tabWindow.setCompositeRect(x, y, w, hh);
            tabWindow.resize((int) w, (int) hh);

            WindowInstance contentWindow = h.getContentContext().getWindow();
            contentWindow.setCompositeRect(x, y, w, hh);
        }
    }

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