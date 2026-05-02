package editor.bootstrap.dockpipeline.tabmanager;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.dockpipeline.tab.TabData;
import editor.bootstrap.dockpipeline.tab.TabInstance;
import editor.bootstrap.dockpipeline.tabgroup.TabGroupInstance;
import engine.root.ContextPackage;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TabManager extends ManagerPackage {

    /*
     * Owns all tabs. Sole factory for TabInstance.
     * Drives context lifecycle — creates, activates, suspends, and destroys
     * contexts as tabs are opened, switched, and closed.
     * The context inside each tab owns its own FBO and render pipeline.
     * TabManager does not touch FBOs — that is the context's responsibility.
     */

    // Registry
    private ObjectArrayList<TabInstance> tabs;
    private FboManager fboManager;

    // Internal \\

    @Override
    protected void create() {
        this.tabs = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.fboManager = get(FboManager.class);
    }

    // Tab Lifecycle \\

    public TabInstance createTab(
            String title,
            Class<? extends ContextPackage> contextClass,
            WindowInstance window,
            int x, int y, int width, int height) {

        FboInstance fbo = fboManager.cloneFbo(RuntimeSetting.FBO_UI, window);
        fboManager.resize(fbo, width, height);

        TabData data = new TabData(title, contextClass, x, y, width, height);
        TabInstance tab = create(TabInstance.class);
        tab.constructor(data);
        tab.setFbo(fbo);

        ContextPackage context = internal.createTabContext(contextClass, window, fbo);
        tab.setContext(context);

        tabs.add(tab);
        return tab;
    }

    public void destroyTab(TabInstance tab) {
        if (tab.hasContext())
            internal.destroyContext(tab.getContext());
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
    }

    // Accessible \\

    public ObjectArrayList<TabInstance> getTabs() {
        return tabs;
    }
}
