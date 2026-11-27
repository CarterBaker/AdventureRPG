package editor.bootstrap.layoutplannerpipeline;

import editor.bootstrap.layoutplannerpipeline.layoutplannermanager.LayoutPlannerManager;
import engine.root.PipelinePackage;

public class LayoutPlannerPipeline extends PipelinePackage {

    /*
     * Registers the settlement type the Layout Planner designs, the node of
     * its layout picked and how its preview is planned, so every layout
     * planner tab, the Layers tab and the Palette tab show the same design.
     */

    // Internal \\

    @Override
    protected void create() {
        create(LayoutPlannerManager.class);
    }
}
