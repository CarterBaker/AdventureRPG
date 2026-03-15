package com.internal.bootstrap.entitypipeline.entity;

import com.internal.bootstrap.entitypipeline.behavior.BehaviorHandle;
import com.internal.bootstrap.entitypipeline.inventory.InventoryHandle;
import com.internal.bootstrap.entitypipeline.statistics.StatisticsHandle;
import com.internal.bootstrap.physicspipeline.util.BlockCompositionStruct;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.core.engine.InstancePackage;
import com.internal.core.util.mathematics.vectors.Vector3;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public class EntityInstance extends InstancePackage {

    /*
     * Runtime entity handed out by EntityManager.spawnEntity(). Holds a
     * reference to its template EntityData plus all per-instance runtime
     * state — position, physics, statistics, inventory, and movement state.
     */

    // Internal
    private EntityData entityData;
    private WorldHandle worldHandle;
    private BehaviorHandle behaviorHandle;

    // State
    private EntityStateHandle entityStateHandle;
    private StatisticsHandle statisticsHandle;
    private InventoryHandle inventoryHandle;

    // Physics
    private WorldPositionStruct worldPositionStruct;
    private Vector3Int blockComposition;
    private BlockCompositionStruct blockCompositionStruct;

    // Runtime
    private Vector3 size;
    private float weight;

    // Internal \\

    @Override
    protected void create() {

        // State
        this.entityStateHandle = create(EntityStateHandle.class);
        this.statisticsHandle = create(StatisticsHandle.class);
        this.inventoryHandle = create(InventoryHandle.class);

        // Physics
        this.worldPositionStruct = new WorldPositionStruct();
        this.blockComposition = new Vector3Int();
        this.blockCompositionStruct = new BlockCompositionStruct();
    }

    // Constructor \\

    public void constructor(
            EntityData entityData,
            WorldHandle worldHandle,
            BehaviorHandle behaviorHandle,
            Vector3 position,
            long chunkCoordinate,
            Vector3 size,
            float weight) {

        // Internal
        this.entityData = entityData;
        this.worldHandle = worldHandle;
        this.behaviorHandle = behaviorHandle;

        // Physics
        this.worldPositionStruct.setPosition(position);
        this.worldPositionStruct.setChunkCoordinate(chunkCoordinate);

        // Runtime
        setEntitySize(size);
        this.weight = weight;
    }

    // Utility \\

    private void setEntitySize(Vector3 size) {

        this.blockComposition.x = (int) Math.ceil(size.x);
        this.blockComposition.y = (int) Math.ceil(size.y);
        this.blockComposition.z = (int) Math.ceil(size.z);
        this.size = size;

        updateBlockComposition();
    }

    private void updateBlockComposition() {
        this.blockCompositionStruct.updateBlockComposition(
                blockComposition,
                worldPositionStruct.getPosition(),
                worldPositionStruct.getChunkCoordinate());
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

    public void setBehaviorHandle(BehaviorHandle behaviorHandle) {
        this.behaviorHandle = behaviorHandle;
    }

    public EntityStateHandle getEntityStateHandle() {
        return entityStateHandle;
    }

    public StatisticsHandle getStatisticsHandle() {
        return statisticsHandle;
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

    public void setSize(Vector3 size) {
        setEntitySize(size);
    }

    public float getWeight() {
        return weight;
    }

    public float getEyeHeight() {
        return size.y * entityData.getEyeLevel();
    }
}