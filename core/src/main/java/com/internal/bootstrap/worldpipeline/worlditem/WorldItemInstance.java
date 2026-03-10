package com.internal.bootstrap.worldpipeline.worlditem;

import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.core.engine.InstancePackage;

public class WorldItemInstance extends InstancePackage {

    private ItemDefinitionHandle itemDefinitionHandle;
    private long chunkCoordinate;
    private int packedBlockCoordinate;
    private long packedPosition;
    private int packedItem;

    // Constructor \\

    public void constructor(
            ItemDefinitionHandle itemDefinitionHandle,
            long chunkCoordinate,
            int packedBlockCoordinate,
            long packedPosition,
            int packedItem) {
        this.itemDefinitionHandle = itemDefinitionHandle;
        this.chunkCoordinate = chunkCoordinate;
        this.packedBlockCoordinate = packedBlockCoordinate;
        this.packedPosition = packedPosition;
        this.packedItem = packedItem;
    }

    // Accessible \\

    public ItemDefinitionHandle getItemDefinitionHandle() {
        return itemDefinitionHandle;
    }

    public long getChunkCoordinate() {
        return chunkCoordinate;
    }

    public int getPackedBlockCoordinate() {
        return packedBlockCoordinate;
    }

    public long getPackedPosition() {
        return packedPosition;
    }

    public int getPackedItem() {
        return packedItem;
    }
}