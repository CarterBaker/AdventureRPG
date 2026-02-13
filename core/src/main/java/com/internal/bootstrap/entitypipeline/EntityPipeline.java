package com.internal.bootstrap.entitypipeline;

import com.internal.bootstrap.entitypipeline.entityManager.EntityManager;
import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.core.engine.PipelinePackage;

public class EntityPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Entity Pipeline
        create(EntityManager.class);
        create(PlayerManager.class);
    }
}
