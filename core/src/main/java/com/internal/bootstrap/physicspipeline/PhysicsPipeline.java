package com.internal.bootstrap.physicspipeline;

import com.internal.bootstrap.physicspipeline.moevementmanager.MovementManager;
import com.internal.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import com.internal.core.engine.PipelinePackage;

public class PhysicsPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Entity Pipeline
        create(MovementManager.class);
        create(RaycastManager.class);
    }
}