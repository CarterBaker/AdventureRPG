package com.internal.bootstrap.entitypipeline.entityManager;

import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.worldpipeline.util.WorldPositionUtility;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.mathematics.vectors.Vector3;

public class EntityManager extends ManagerPackage {

    private WorldStreamManager worldStreamManager;

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    public EntityHandle createEntity() {

        WorldHandle activeWorldHandle = worldStreamManager.getActiveWorld();
        long randomChunk = WorldPositionUtility.getRandomChunk(activeWorldHandle);

        EntityHandle entityHandle = create(EntityHandle.class);
        entityHandle.constructor(
                worldStreamManager.getActiveWorld(),
                new Vector3(),
                randomChunk);

        return entityHandle;
    }
}
