package com.AdventureRPG.core.entitypipeline.entityManager;

import com.AdventureRPG.core.engine.HandlePackage;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3;
import com.AdventureRPG.playermanager.StatisticsInstance;

public class EntityHandle extends HandlePackage {

    // Internal
    private StatisticsInstance statisticsInstance;

    private Vector3 currentPosition;
    private long currentChunk;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.statisticsInstance = create(StatisticsInstance.class);
    }
}
