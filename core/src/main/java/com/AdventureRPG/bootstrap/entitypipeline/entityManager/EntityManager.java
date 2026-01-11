package com.AdventureRPG.bootstrap.entitypipeline.entityManager;

import com.AdventureRPG.bootstrap.entitypipeline.entity.EntityHandle;
import com.AdventureRPG.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.util.mathematics.Extras.Coordinate2Int;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3;

public class EntityManager extends ManagerPackage {

    private WorldStreamManager worldStreamManager;

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    public EntityHandle createEntity() {

        EntityHandle entityHandle = create(EntityHandle.class);
        entityHandle.constructor(
                worldStreamManager.getActiveWorld(),
                new Vector3(),
                Coordinate2Int.pack(0, 0));

        return entityHandle;
    }
}
