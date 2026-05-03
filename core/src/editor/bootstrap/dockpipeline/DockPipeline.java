package editor.bootstrap.dockpipeline;

import editor.bootstrap.dockpipeline.dockgeometrysystem.DockGeometrySystem;
import editor.bootstrap.dockpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.dockpipeline.dockmanager.DockManager;
import editor.bootstrap.dockpipeline.tabmanager.TabManager;
import engine.root.PipelinePackage;

public class DockPipeline extends PipelinePackage {

    /*
     * Registers dock infrastructure in dependency order.
     * DockManager before TabManager — tabs need the manager to exist first.
     * Render and input systems live in runtime contexts, not here.
     */

    @Override
    protected void create() {
        create(DockManager.class);
        create(TabManager.class);
        create(DockGeometrySystem.class);
        create(DockLayoutSystem.class);
    }
}