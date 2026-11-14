package application.bootstrap.furnishingpipeline;

import application.bootstrap.furnishingpipeline.furnishingmanager.FurnishingManager;
import engine.root.PipelinePackage;

public class FurnishingPipeline extends PipelinePackage {

    /*
     * Registers the furnishing pipeline. FurnishingManager owns every shared
     * table of furniture a place can be furnished from, so a vehicle's cabin
     * and a house's hall draw their furniture the same way. The pipeline is
     * created after the item pipeline, whose definitions every table names,
     * and ahead of the vehicle and world pipelines that furnish from it.
     */

    @Override
    protected void create() {
        create(FurnishingManager.class);
    }
}
