package application.bootstrap.worldpipeline.worlditem;

import application.bootstrap.itempipeline.item.ItemInstance;
import engine.root.StructPackage;

public class WorldItemStruct extends StructPackage {

    /*
     * Persistent record of one placed item in a subchunk: its packed sub-voxel
     * position and rotation, packed item ID, the loot table a container found
     * here is filled from, and the real item standing there once one is
     * needed.
     */

    public long packedPosition; // chunk-local sub-voxel XYZ + rotation via Coordinate4Long
    public int packedItem; // item ID + metadata
    public short lootTableID; // the loot table its contents are drawn from, LOOT_TABLE_NONE when it comes empty
    public ItemInstance itemInstance; // the real item standing here, null until one is needed

    public WorldItemStruct(long packedPosition, int packedItem, short lootTableID, ItemInstance itemInstance) {
        this.packedPosition = packedPosition;
        this.packedItem = packedItem;
        this.lootTableID = lootTableID;
        this.itemInstance = itemInstance;
    }
}
