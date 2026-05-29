package editor.bootstrap.tabpipeline;

import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.layoutmanager.LayoutManager;
import editor.bootstrap.tabpipeline.tabdragmanager.TabDragManager;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.PipelinePackage;

public class TabPipeline extends PipelinePackage {

    // Internal \\

    @Override
    protected void create() {
        create(TabManager.class);
        create(TabDragManager.class);
        create(DockLayoutSystem.class);
        create(LayoutManager.class);
    }
}