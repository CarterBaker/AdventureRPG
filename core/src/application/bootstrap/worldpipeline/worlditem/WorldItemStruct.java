package application.bootstrap.worldpipeline.worlditem;

import application.bootstrap.itempipeline.item.ItemInstance;
import engine.root.StructPackage;

public class WorldItemStruct extends StructPackage {

    /*
     * Persistent record of one placed item in a subchunk: its packed sub-voxel
     * position and rotation, packed item ID, and the real item standing there
     * once one is needed.
     */

    public long packedPosition; // chunk-local sub-voxel XYZ + rotation via Coordinate4Long
    public int packedItem; // item ID + metadata
    public ItemInstance itemInstance; // the real item standing here, null until one is needed

    public WorldItemStruct(long packedPosition, int packedItem, ItemInstance itemInstance) {
        this.packedPosition = packedPosition;
        this.packedItem = packedItem;
        this.itemInstance = itemInstance;
    }
}
