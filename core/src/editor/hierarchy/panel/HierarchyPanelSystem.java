package editor.hierarchy.panel;

import application.bootstrap.menupipeline.hierarchy.HierarchyInstance;
import application.bootstrap.menupipeline.hierarchymanager.HierarchyManager;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.SystemPackage;

public class HierarchyPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and hosts a hierarchy panel for
     * the context's lifetime.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private HierarchyManager hierarchyManager;

    // Panel
    private HierarchyInstance hierarchy;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.hierarchyManager = get(HierarchyManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.hierarchy = hierarchyManager.openHierarchy(window);
    }

    @Override
    protected void dispose() {

        hierarchyManager.closeHierarchy(hierarchy);
        menuManager.setMenuTargetFbo(context.getWindow(), null);
    }
}
