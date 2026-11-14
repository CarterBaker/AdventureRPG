package application.bootstrap.vehiclepipeline;

import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import engine.root.PipelinePackage;

public class VehiclePipeline extends PipelinePackage {

    /*
     * Registers the vehicle pipeline. VehicleManager owns every vehicle type
     * and every vehicle in the world, and steps them in UPDATE on their own
     * fixed clock. The pipeline is created after the physics pipeline and
     * ahead of the entity pipeline, so vehicles have moved and carried their
     * riders before any entity moves or any camera is placed each frame, and
     * every entity stands on a deck exactly where that deck is drawn.
     */

    @Override
    protected void create() {
        create(VehicleManager.class);
    }
}
