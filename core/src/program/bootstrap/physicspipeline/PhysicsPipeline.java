package program.bootstrap.physicspipeline;

import program.bootstrap.physicspipeline.movementmanager.MovementManager;
import program.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import program.core.engine.PipelinePackage;

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