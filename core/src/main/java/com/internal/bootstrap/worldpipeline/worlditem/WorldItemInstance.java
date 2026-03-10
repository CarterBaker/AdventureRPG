package com.internal.bootstrap.worldpipeline.worlditem;

import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.core.engine.InstancePackage;

public class WorldItemInstance extends InstancePackage {

    private ItemDefinitionHandle itemDefinitionHandle;
    private ModelInstance model;
    private int packedBlockCoordinate; // chunk-local block XYZ packed via Coordinate3Int
    private long packedPosition; // chunk-local sub-voxel XYZ + rotation via Coordinate4Long
    private int packedItem; // item ID + metadata

    // Constructor \\

    public void constructor(
            ItemDefinitionHandle itemDefinitionHandle,
            ModelInstance model,
            int packedBlockCoordinate,
            long packedPosition,
            int packedItem) {
        this.itemDefinitionHandle = itemDefinitionHandle;
        this.model = model;
        this.packedBlockCoordinate = packedBlockCoordinate;
        this.packedPosition = packedPosition;
        this.packedItem = packedItem;
    }

    // Accessible \\

    public ItemDefinitionHandle getItemDefinitionHandle() {
        return itemDefinitionHandle;
    }

    public ModelInstance getModel() {
        return model;
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