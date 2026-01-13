package com.internal.bootstrap.entitypipeline.entity;

import com.internal.bootstrap.entitypipeline.entityManager.StatisticsInstance;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.vectors.Vector3;

public class EntityHandle extends HandlePackage {

    // Internal
    private WorldHandle worldHandle;
    private WorldPositionStruct worldPositionStruct;
    private StatisticsInstance statisticsInstance;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.worldPositionStruct = new WorldPositionStruct();
        this.statisticsInstance = create(StatisticsInstance.class);
    }

    public void constructor(
            WorldHandle worldHandle,
            Vector3 position,
            long chunkCoordinate) {

        // Internal
        this.worldHandle = worldHandle;
        this.worldPositionStruct.setPosition(position);
        this.worldPositionStruct.setChunkCoordinate(chunkCoordinate);
    }

    // Accessible \\

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public WorldPositionStruct getWorldPositionStruct() {
        return worldPositionStruct;
    }

    public StatisticsInstance getStatisticsInstance() {
        return statisticsInstance;
    }
}
