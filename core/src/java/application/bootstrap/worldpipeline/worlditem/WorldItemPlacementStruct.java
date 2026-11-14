package application.bootstrap.worldpipeline.worlditem;

import engine.root.StructPackage;

public class WorldItemPlacementStruct extends StructPackage {

    /*
     * Output container for a resolved world item placement: the chunk the
     * item's model grid corner falls in and its packed chunk-local sub-voxel
     * position and orientation there. Written in place — never allocated per
     * placement.
     */

    // Internal
    private long chunkCoordinate;
    private long packedPosition;

    // Accessible \\

    public long getChunkCoordinate() {
        return chunkCoordinate;
    }

    public long getPackedPosition() {
        return packedPosition;
    }

    public void set(long chunkCoordinate, long packedPosition) {
        this.chunkCoordinate = chunkCoordinate;
        this.packedPosition = packedPosition;
    }
}
