package application.bootstrap.worldpipeline.worlditem;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.core.engine.InstancePackage;

public class WorldItemInstance extends InstancePackage {

    private ItemDefinitionHandle itemDefinitionHandle;
    private long chunkCoordinate;
    private int packedBlockCoordinate;
    private long packedPosition;
    private int packedItem;

    // Render slot — index into the CompositeBufferInstance for this item's type.
    // -1 means not currently registered in any buffer.
    private int instanceSlot;

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
        this.instanceSlot = -1;
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

    public int getInstanceSlot() {
        return instanceSlot;
    }

    public void setInstanceSlot(int slot) {
        this.instanceSlot = slot;
    }

    public void clearInstanceSlot() {
        this.instanceSlot = -1;
    }
}