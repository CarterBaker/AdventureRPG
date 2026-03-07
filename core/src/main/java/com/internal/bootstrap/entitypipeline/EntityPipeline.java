package com.internal.bootstrap.entitypipeline;

import com.internal.bootstrap.entitypipeline.behaviormanager.BehaviorManager;
import com.internal.bootstrap.entitypipeline.entitymanager.EntityManager;
import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.core.engine.PipelinePackage;

public class EntityPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Entity Pipeline
        create(BehaviorManager.class);
        create(EntityManager.class);
        create(PlayerManager.class);
    }
}
