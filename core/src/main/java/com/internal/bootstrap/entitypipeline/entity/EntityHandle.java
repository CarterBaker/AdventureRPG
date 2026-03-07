package com.internal.bootstrap.entitypipeline.entity;

import com.internal.bootstrap.entitypipeline.behavior.BehaviorHandle;
import com.internal.bootstrap.entitypipeline.inventory.InventoryHandle;
import com.internal.bootstrap.entitypipeline.statistics.StatisticsStruct;
import com.internal.bootstrap.physicspipeline.util.BlockCompositionStruct;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.vectors.Vector3;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public class EntityHandle extends HandlePackage {

    // Internal
    private EntityData entityData;
    private WorldHandle worldHandle;
    private BehaviorHandle behaviorHandle;

    private WorldPositionStruct worldPositionStruct;
    private Vector3Int blockComposition;
    private BlockCompositionStruct blockCompositionStruct;
    private StatisticsStruct statisticsStruct;
    private EntityStateHandle entityStateHandle;
    private InventoryHandle inventoryHandle;

    private Vector3 size;
    private float weight;

    // Internal \\

    @Override
    protected void create() {
        this.worldPositionStruct = new WorldPositionStruct();
        this.blockComposition = new Vector3Int();
        this.blockCompositionStruct = new BlockCompositionStruct();
        this.statisticsStruct = new StatisticsStruct();
        this.entityStateHandle = create(EntityStateHandle.class);
        this.inventoryHandle = create(InventoryHandle.class);

        this.entityStateHandle.constructor();
        this.inventoryHandle.constructor();
    }

    public void constructor(
            EntityData entityData,
            WorldHandle worldHandle,
            BehaviorHandle behaviorHandle,
            Vector3 position,
            long chunkCoordinate,
            Vector3 size,
            float weight) {

        this.entityData = entityData;
        this.worldHandle = worldHandle;
        this.behaviorHandle = behaviorHandle;

        this.worldPositionStruct.setPosition(position);
        this.worldPositionStruct.setChunkCoordinate(chunkCoordinate);

        setEntitySize(size);
        this.weight = weight;
    }

    public void update() {
        this.blockCompositionStruct.updateBlockComposition(
                blockComposition,
                worldPositionStruct.getPosition(),
                worldPositionStruct.getChunkCoordinate());
    }

    // Utility \\

    private void setEntitySize(Vector3 size) {
        this.blockComposition.x = (int) Math.ceil(size.x);
        this.blockComposition.y = (int) Math.ceil(size.y);
        this.blockComposition.z = (int) Math.ceil(size.z);
        this.size = size;
        update();
    }

    // Accessible \\

    public EntityData getEntityData() {
        return entityData;
    }

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public BehaviorHandle getBehaviorHandle() {
        return behaviorHandle;
    }

    public EntityStateHandle getEntityStateHandle() {
        return entityStateHandle;
    }

    public StatisticsStruct getStatisticsInstance() {
        return statisticsStruct;
    }

    public InventoryHandle getInventoryHandle() {
        return inventoryHandle;
    }

    public WorldPositionStruct getWorldPositionStruct() {
        return worldPositionStruct;
    }

    public Vector3Int getBlockComposition() {
        return blockComposition;
    }

    public BlockCompositionStruct getBlockCompositionStruct() {
        return blockCompositionStruct;
    }

    public Vector3 getSize() {
        return size;
    }

    public float getWeight() {
        return weight;
    }

    public void setSize(Vector3 size) {
        setEntitySize(size);
    }

    public void setBehaviorHandle(BehaviorHandle behaviorHandle) {
        this.behaviorHandle = behaviorHandle;
    }

    public float getEyeHeight() {
        return size.y * entityData.getEyeLevel();
    }
}