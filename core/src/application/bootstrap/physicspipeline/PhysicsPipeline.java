package application.bootstrap.physicspipeline;

import application.bootstrap.physicspipeline.movementmanager.MovementManager;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import engine.root.PipelinePackage;

public class PhysicsPipeline extends PipelinePackage {

    /*
     * Registers all physics pipeline managers in dependency order.
     */

    @Override
    protected void create() {
        create(MovementManager.class);
        create(RaycastManager.class);
    }
}