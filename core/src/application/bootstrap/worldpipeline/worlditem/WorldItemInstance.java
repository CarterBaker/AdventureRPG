package application.bootstrap.worldpipeline.worlditem;

import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import engine.root.InstancePackage;

public class WorldItemInstance extends InstancePackage {

    /*
     * One placed world item at runtime: its definition, chunk, block and packed
     * sub-voxel position, and its slot in the item type's composite buffer. The
     * WorldItemStruct it was built from keeps the real item across palette
     * rebuilds.
     */

    // The subchunk struct this instance was built from — it outlives the
    // instance across palette rebuilds, so the real item is kept on it.
    private WorldItemStruct worldItemStruct;

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
            WorldItemStruct worldItemStruct,
            ItemDefinitionHandle itemDefinitionHandle,
            long chunkCoordinate,
            int packedBlockCoordinate,
            long packedPosition,
            int packedItem) {
        this.worldItemStruct = worldItemStruct;
        this.itemDefinitionHandle = itemDefinitionHandle;
        this.chunkCoordinate = chunkCoordinate;
        this.packedBlockCoordinate = packedBlockCoordinate;
        this.packedPosition = packedPosition;
        this.packedItem = packedItem;
        this.instanceSlot = -1;
    }

    // Accessible \\

    public boolean hasItemInstance() {
        return worldItemStruct.itemInstance != null;
    }

    public ItemInstance getItemInstance() {
        return worldItemStruct.itemInstance;
    }

    public void setItemInstance(ItemInstance itemInstance) {
        worldItemStruct.itemInstance = itemInstance;
    }

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