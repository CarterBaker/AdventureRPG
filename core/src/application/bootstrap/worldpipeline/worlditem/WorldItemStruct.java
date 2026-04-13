package application.bootstrap.worldpipeline.worlditem;

import engine.root.StructPackage;

public class WorldItemStruct extends StructPackage {

    public long packedPosition; // chunk-local sub-voxel XYZ + rotation via Coordinate4Long
    public int packedItem; // item ID + metadata

    public WorldItemStruct(long packedPosition, int packedItem) {
        this.packedPosition = packedPosition;
        this.packedItem = packedItem;
    }
}