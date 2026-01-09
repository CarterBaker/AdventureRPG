package com.AdventureRPG.bootstrap.entitypipeline;

import com.AdventureRPG.bootstrap.entitypipeline.entityManager.EntityManager;
import com.AdventureRPG.bootstrap.entitypipeline.movementmanager.MovementManager;
import com.AdventureRPG.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.AdventureRPG.core.engine.PipelinePackage;

public class EntityPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Entity Pipeline
        create(MovementManager.class);
        create(EntityManager.class);
        create(PlayerManager.class);
    }
}
