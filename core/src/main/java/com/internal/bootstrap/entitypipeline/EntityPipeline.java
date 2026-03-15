package com.internal.bootstrap.entitypipeline;

import com.internal.bootstrap.entitypipeline.behaviormanager.BehaviorManager;
import com.internal.bootstrap.entitypipeline.entitymanager.EntityManager;
import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.core.engine.PipelinePackage;

public class EntityPipeline extends PipelinePackage {

    /*
     * Registers all entity pipeline managers in dependency order. BehaviorManager
     * and EntityManager are registered before PlayerManager since player spawn
     * depends on both being available.
     */

    @Override
    protected void create() {
        create(BehaviorManager.class);
        create(EntityManager.class);
        create(PlayerManager.class);
    }
}