package editor.bootstrap.tabpipeline.tabmanager;

import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeContext;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.layoutmanager.LayoutManager;
import editor.bootstrap.tabpipeline.tab.TabContext;
import editor.bootstrap.tabpipeline.tab.TabData;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.util.DropZone;
import editor.commandconsole.CommandConsoleContext;
import editor.console.ConsoleContext;
import editor.dev.DevContext;
import editor.hierarchy.HierarchyContext;
import editor.infopanel.InfoPanelContext;
import editor.itemeditor.ItemEditorContext;
import editor.runtime.EditorSecondaryWindowContext;
import editor.runtime.EditorSetting;
import editor.textureviewer.TextureViewerContext;
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
     * Coordinates tabs across every editor OS window, main and secondary alike.
     * Each structural operation has one owner: openTab(), closeTab(),
     * dockTab(), openSecondaryOsWindow() and closeOsWindowIfEmpty(). Tab
     * content runs isolated, so a crashing tab closes instead of taking the
     * editor down. pushRects() and notifyLayoutChanged() are the single call
     * sites for rect propagation and layout saving and no-op while a batch is
     * open.
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
        return openTab(EditorSetting.TAB_TITLE_PREVIEW, RuntimeContext.class, osWindow);
    }

    public TabHandle openDev(WindowInstance osWindow) {
        return openTab(EditorSetting.TAB_TITLE_DEV, DevContext.class, osWindow);
    }

    public TabHandle openHierarchy(WindowInstance osWindow) {
        return openTab(EditorSetting.TAB_TITLE_HIERARCHY, HierarchyContext.class, osWindow);
    }

    public TabHandle openInfoPanel(WindowInstance osWindow) {
        return openTab(EditorSetting.TAB_TITLE_INFO_PANEL, InfoPanelContext.class, osWindow);
    }

    public TabHandle openItemEditor(WindowInstance osWindow) {
        return openTab(EditorSetting.TAB_TITLE_ITEM_EDITOR, ItemEditorContext.class, osWindow);
    }

    public TabHandle openTextureViewer(WindowInstance osWindow) {
        return openTab(
                EditorSetting.TAB_TITLE_TEXTURE_VIEWER,
                TextureViewerContext.class,
                osWindow);
    }

    public TabHandle openConsole(WindowInstance osWindow) {
        return openTab(EditorSetting.TAB_TITLE_CONSOLE, ConsoleContext.class, osWindow);
    }

    public TabHandle openCommandConsole(WindowInstance osWindow) {
        return openTab(
                EditorSetting.TAB_TITLE_COMMAND_CONSOLE,
                CommandConsoleContext.class,
                osWindow);
    }

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
        // Handle
        TabHandle handle = create(TabHandle.class);
        handle.constructor(new TabData(baseTitle, title, contentClass));
        // Chrome window
        WindowInstance tabWindow = windowManager.createLogicalWindow(title, osWindow);
        tabWindow.setCaptureEligible(false);
        tabWindow.setFocusIndependent(true);
        // Content window
        WindowInstance contentWindow = windowManager.createLogicalWindow(title, osWindow);
        contentWindow.setCaptureEligible(true);
        // Contexts
        TabContext tabContext = internal.createContext(TabContext.class, tabWindow);
        ContextPackage contentContext = internal.createContext(
                contentClass,
                contentWindow,
                () -> closeCrashedTab(handle));
        tabContext.linkContent(contentContext);
        tabContext.bringToFront();
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

    private void closeCrashedTab(TabHandle handle) {
        if (!handle.isOpen())
            return;
        errorLog("Tab '" + handle.getTabTitle() + "' crashed and was closed.");
        closeTab(handle);
    }

    public void closeAll() {
        ObjectArrayList<WindowInstance> osWindows = new ObjectArrayList<>(osWindow2DockRect.keySet());
        for (int i = 0; i < osWindows.size(); i++)
            closeOsWindow(osWindows.get(i));
        ObjectArrayList<TabHandle> remaining = new ObjectArrayList<>(openTabs);
        for (int i = 0; i < remaining.size(); i++)
            closeTab(remaining.get(i));
    }

    public void deregisterTab(TabHandle handle) {
        if (handle == null || !openTabs.contains(handle))
            return;
        int tabID = getTabIDFromTabName(handle.getTabTitle());
        tabName2TabID.removeInt(handle.getTabTitle());
        tabID2TabHandle.remove(tabID);
        openTabs.remove(handle);
        handle.mount(null);
    }

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
    public WindowInstance openSecondaryOsWindow() {
        WindowInstance osWindow = windowManager.openWindow(
                EditorSetting.WINDOW_TITLE_EDITOR_SECONDARY,
                EditorSecondaryWindowContext.class);
        registerOsWindow(osWindow);
        return osWindow;
    }

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

    public void closeOsWindow(WindowInstance osWindow) {
        if (osWindow == null || osWindow == windowManager.getMainWindow())
            return;
        windowManager.destroyOsWindow(osWindow);
    }

    // Rect Propagation \\
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
    public void notifyLayoutChanged() {
        if (batching)
            return;
        layoutManager.notifyLayoutChanged();
    }

    public void beginBatch() {
        batching = true;
    }

    public void endBatch() {
        batching = false;
        computeAndPlaceRects();
    }

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
