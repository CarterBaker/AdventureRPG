package com.internal.bootstrap.physicspipeline;

import com.internal.bootstrap.physicspipeline.movementmanager.MovementManager;
import com.internal.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import com.internal.core.engine.PipelinePackage;

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