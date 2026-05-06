package editor.runtime;

import editor.bootstrap.dockpipeline.dockmanager.DockManager;
import editor.bootstrap.dockpipeline.dockinputsystem.DockInputSystem;
import editor.bootstrap.dockpipeline.dockrendersystem.DockRenderSystem;
import engine.root.ContextPackage;

public class EditorWindowSecondary extends ContextPackage {

    /*
     * Bound to each secondary editor OS window.
     * Clones UI FBO, creates dock container for this window,
     * and runs chrome render and input systems.
     */

    // Managers
    private DockManager dockManager;

    @Override
    protected void create() {
        create(MenuTargetFboSystem.class);
        create(DockRenderSystem.class);
        create(DockInputSystem.class);
    }

    @Override
    protected void get() {
        this.dockManager = get(DockManager.class);
    }

    @Override
    protected void start() {
        if (dockManager.getContainerForWindow(getWindow()) == null)
            dockManager.createContainer(getWindow(), "dock_canvas");
    }
}
