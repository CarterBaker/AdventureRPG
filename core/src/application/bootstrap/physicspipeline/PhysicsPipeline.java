package application.bootstrap.physicspipeline;

import application.bootstrap.physicspipeline.movementmanager.MovementManager;
import application.bootstrap.physicspipeline.physicsnoisemanager.PhysicsNoiseManager;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import engine.root.PipelinePackage;

public class PhysicsPipeline extends PipelinePackage {

    /*
     * Registers all physics pipeline managers in dependency order.
     * PhysicsNoiseManager is created first since BlockCollisionBranch reads
     * from it on every axis test.
     */

    @Override
    protected void create() {
        create(PhysicsNoiseManager.class);
        create(MovementManager.class);
        create(RaycastManager.class);
    }
}