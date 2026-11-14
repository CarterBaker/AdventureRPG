package application.bootstrap.shipwrightpipeline;

import application.bootstrap.shipwrightpipeline.shipwrightmanager.ShipwrightManager;
import engine.root.PipelinePackage;

public class ShipwrightPipeline extends PipelinePackage {

    /*
     * Registers the shipwright pipeline. ShipwrightManager owns the drafting
     * table the editor and the game share, and every ship drawn at it. The
     * pipeline is created after the vehicle pipeline, so every ship is
     * registered as a vehicle once the vehicles are loaded.
     */

    @Override
    protected void create() {
        create(ShipwrightManager.class);
    }
}
