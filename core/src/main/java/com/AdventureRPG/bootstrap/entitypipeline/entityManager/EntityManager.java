package com.AdventureRPG.bootstrap.entitypipeline.entityManager;

import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.util.mathematics.Extras.Coordinate2Int;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3;

public class EntityManager extends ManagerPackage {

    public EntityHandle createEntity() {

        EntityHandle entityHandle = create(EntityHandle.class);
        entityHandle.constructor(new Vector3(), Coordinate2Int.pack(0, 0));

        return entityHandle;
    }
}
