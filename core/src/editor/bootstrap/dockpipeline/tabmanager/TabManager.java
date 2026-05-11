package editor.bootstrap.dockpipeline.tabmanager;

import application.kernel.windowpipeline.window.WindowData;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.dockpipeline.dockgeometrysystem.DockGeometrySystem;
import editor.bootstrap.dockpipeline.tab.TabContext;
import editor.bootstrap.dockpipeline.tab.TabData;
import editor.bootstrap.dockpipeline.tab.TabInstance;
import editor.bootstrap.dockpipeline.tabgroup.TabGroupInstance;
import engine.root.ContextPackage;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TabManager extends ManagerPackage {

    /*
     * Owns all tabs. Sole factory for TabInstance.
     * Each tab gets a logical WindowInstance — no native handle, just
     * dimensions and composite routing onto the OS window. Logical windows
     * are registered in WindowManager so they participate in the full render
     * loop alongside OS windows. createOSWindow = false tells WindowManager
     * to skip native platform window creation. Depth 1 ensures their FBO
     * blits always draw on top of the OS window's own blits during the screen pass.
     */

    // Registry
    private ObjectArrayList<TabInstance> tabs;
    private WindowManager windowManager;
    private DockGeometrySystem dockGeometrySystem;

    // Internal \\

    @Override
    protected void create() {
        this.tabs = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
        this.dockGeometrySystem = get(DockGeometrySystem.class);
    }

    // Tab Lifecycle \\

    public TabInstance createTab(
            String title,
            Class<? extends ContextPackage> contextClass,
            WindowInstance osWindow,
            int x, int y, int width, int height) {

        WindowInstance shellWindow = create(WindowInstance.class);
        shellWindow.constructor(new WindowData(windowManager.issueWindowID(), width, height, title, false));
        shellWindow.setCompositeTarget(osWindow);
        shellWindow.setCompositeRect(x, y, width, height);
        shellWindow.setDepth(1);
        windowManager.registerDetachedWindow(shellWindow);

        TabContext shellContext = internal.createTabContext(TabContext.class, shellWindow);

        WindowInstance contentWindow = create(WindowInstance.class);
        contentWindow.constructor(new WindowData(windowManager.issueWindowID(), width, height, title, false));
        contentWindow.setCompositeTarget(osWindow);
        contentWindow.setCompositeRect(x, y, width, height);
        contentWindow.setDepth(1);
        windowManager.registerDetachedWindow(contentWindow);

        ContextPackage contentContext = internal.createChildContext(shellContext, contextClass, contentWindow);
        shellContext.mountContent(contentWindow, contentContext);

        TabData data = new TabData(title, contextClass, x, y, width, height);
        TabInstance tab = create(TabInstance.class);
        tab.constructor(data);
        tab.setLogicalWindow(shellWindow);

        dockGeometrySystem.buildTabModel(tab);

        tab.setContext(shellContext);

        tabs.add(tab);
        return tab;
    }

    public void destroyTab(TabInstance tab) {
        if (tab.hasLogicalWindow())
            tab.getLogicalWindow().dispose();
        tabs.remove(tab);
    }

    // Activation \\

    public void activateTab(TabInstance tab, TabGroupInstance group) {
        TabInstance current = group.getActiveTab();
        if (current != null && current != tab)
            current.suspend();

        int index = group.getTabs().indexOf(tab);
        if (index == -1)
            return;

        group.setActiveIndex(index);
        tab.activate();
    }

    public void suspendTab(TabInstance tab) {
        tab.suspend();
    }

    // Resize \\

    public void resizeTab(TabInstance tab, int x, int y, int width, int height) {
        tab.resize(x, y, width, height);
        if (tab.hasLogicalWindow()) {
            WindowInstance logicalWindow = tab.getLogicalWindow();
            logicalWindow.resize(width, height);
            logicalWindow.setCompositeRect(x, y, width, height);
        }
    }

    // Accessible \\

    public ObjectArrayList<TabInstance> getTabs() {
        return tabs;
    }
}