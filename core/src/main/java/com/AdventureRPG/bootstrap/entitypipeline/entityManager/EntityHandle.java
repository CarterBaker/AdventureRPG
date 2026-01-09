package com.AdventureRPG.bootstrap.entitypipeline.entityManager;

import com.AdventureRPG.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.AdventureRPG.core.engine.HandlePackage;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3;

public class EntityHandle extends HandlePackage {

    // Internal
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
            Vector3 position,
            long chunkCoordinate) {

        // Internal
        this.worldPositionStruct.setPosition(position);
        this.worldPositionStruct.setChunkCoordinate(chunkCoordinate);
    }

    // Accessible \\

    public WorldPositionStruct getWorldPositionStruct() {
        return worldPositionStruct;
    }

    public StatisticsInstance getStatisticsInstance() {
        return statisticsInstance;
    }
}
