package com.AdventureRPG.core.entitypipeline;

import com.AdventureRPG.core.engine.PipelinePackage;
import com.AdventureRPG.core.entitypipeline.entityManager.EntityManager;

public class EntityPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Entity Pipeline
        create(EntityManager.class);
    }
}
