package com.internal.bootstrap.physicspipeline;

import com.internal.bootstrap.physicspipeline.physicsmanager.PhysicsManager;
import com.internal.core.engine.PipelinePackage;

public class PhysicsPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Entity Pipeline
        create(PhysicsManager.class);
    }
}