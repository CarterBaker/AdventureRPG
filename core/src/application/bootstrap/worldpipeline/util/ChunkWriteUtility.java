package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate3Int;

public final class ChunkWriteUtility extends EngineUtility {

    /*
     * The one way world generation writes a block into a chunk column after
     * its terrain is laid: structures, roads and settlement walls all go
     * through here. A write sets the block whole or as a sub-block mask, its
     * orientation, and fills or empties its liquid, and a write outside the
     * world's height is dropped, so a caller never checks the column's ends.
     */

    // Settings
    private static final int CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    private static final int WORLD_HEIGHT_BLOCKS = EngineSetting.WORLD_HEIGHT * EngineSetting.CHUNK_SIZE;

    // Write \\

    public static void writeBlock(
            SubChunkInstance[] subChunks,
            int localX,
            int worldY,
            int localZ,
            short blockID,
            short orientation,
            int mask,
            boolean liquid) {

        if (worldY < 0 || worldY >= WORLD_HEIGHT_BLOCKS)
            return;

        SubChunkInstance subChunk = subChunks[worldY / CHUNK_SIZE];
        int packedXYZ = Coordinate3Int.pack(localX, worldY % CHUNK_SIZE, localZ);

        subChunk.setSubBlocks(packedXYZ, blockID, mask);
        subChunk.getBlockRotationPaletteHandle().setBlock(packedXYZ, orientation);
        subChunk.setLiquidLevel(packedXYZ, liquid ? EngineSetting.LIQUID_LEVEL_MAX : EngineSetting.LIQUID_LEVEL_EMPTY);
    }

    // A whole block, unturned and dry
    public static void writeSolid(SubChunkInstance[] subChunks, int localX, int worldY, int localZ, short blockID) {
        writeBlock(
                subChunks, localX, worldY, localZ, blockID,
                EngineSetting.DEFAULT_BLOCK_ORIENTATION, SubBlockUtility.MASK_FULL, false);
    }

    // Air, drained of any liquid that stood there
    public static void clearBlock(SubChunkInstance[] subChunks, int localX, int worldY, int localZ) {
        writeBlock(
                subChunks, localX, worldY, localZ, EngineSetting.REGISTRY_RESERVED_ID,
                EngineSetting.DEFAULT_BLOCK_ORIENTATION, SubBlockUtility.MASK_EMPTY, false);
    }

    // Every block from one height up to another, both included, cleared to air
    public static void clearColumn(SubChunkInstance[] subChunks, int localX, int fromY, int toY, int localZ) {
        for (int worldY = Math.max(fromY, 0); worldY <= Math.min(toY, WORLD_HEIGHT_BLOCKS - 1); worldY++)
            clearBlock(subChunks, localX, worldY, localZ);
    }
}
