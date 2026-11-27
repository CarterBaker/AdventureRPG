package editor.bootstrap.roadplannerpipeline;

import editor.bootstrap.roadplannerpipeline.roadplannermanager.RoadPlannerManager;
import engine.root.PipelinePackage;

public class RoadPlannerPipeline extends PipelinePackage {

    /*
     * Registers the road the Road Planner draws across the biome planner's
     * world, the road type it is built as and the cells it runs through, so
     * every road planner tab, the Palette tab and the Biome Viewer show the
     * same road.
     */

    // Internal \\

    @Override
    protected void create() {
        create(RoadPlannerManager.class);
    }
}
