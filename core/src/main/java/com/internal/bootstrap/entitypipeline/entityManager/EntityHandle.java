package com.internal.bootstrap.entitypipeline.entityManager;

import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.vectors.Vector3;

public class EntityHandle extends HandlePackage {

    // Internal
    private EntityData entityData;
    private WorldHandle worldHandle;
    private WorldPositionStruct worldPositionStruct;
    private StatisticsInstance statisticsInstance;

    private Vector3 size;
    private float weight;

    // Internal \\

    @Override
    protected void create() {
        // Internal
        this.worldPositionStruct = new WorldPositionStruct();
        this.statisticsInstance = create(StatisticsInstance.class);
    }

    public void constructor(
            EntityData entityData,
            WorldHandle worldHandle,
            Vector3 position,
            long chunkCoordinate,
            Vector3 size,
            float weight) {

        // Internal
        this.entityData = entityData;
        this.worldHandle = worldHandle;
        this.worldPositionStruct.setPosition(position);
        this.worldPositionStruct.setChunkCoordinate(chunkCoordinate);
        this.size = size;
        this.weight = weight;
    }

    // Accessible \\

    public EntityData getEntityData() {
        return entityData;
    }

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public WorldPositionStruct getWorldPositionStruct() {
        return worldPositionStruct;
    }

    public StatisticsInstance getStatisticsInstance() {
        return statisticsInstance;
    }

    public Vector3 getSize() {
        return size;
    }

    public float getWeight() {
        return weight;
    }

    public float getEyeHeight() {
        return size.y * entityData.getEyeLevel();
    }
}