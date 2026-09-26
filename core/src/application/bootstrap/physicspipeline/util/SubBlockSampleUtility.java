package application.bootstrap.physicspipeline.util;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate3Int;

public class SubBlockSampleUtility extends EngineUtility {

    /*
     * Sub-block-space world queries for physics. A sub-cell is addressed in
     * sub-block units measured from the corner of a chunk — X and Z relative
     * to that chunk and free to run into its neighbors, Y absolute across the
     * whole column — which is exactly how an entity's chunk-local position
     * scales into sub-blocks. A whole block answers for all eight of its
     * sub-cells and an absent octant of a subdivided block reads as empty,
     * so collision and raycasting see sub-blocks at their true size. Every
     * read is a value read, never realizing a virtual subchunk's storage.
     */

    // Settings
    private static final int CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    private static final int WORLD_TOP_CELL = EngineSetting.WORLD_HEIGHT * EngineSetting.CHUNK_SIZE;

    // Internal \\

    private SubBlockSampleUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    // Lookup \\

    /*
     * The block occupying a sub-cell, or null where the sub-cell is empty,
     * lies outside the world, or falls in a chunk that is not loaded.
     */
    public static BlockHandle getSubBlockAt(
            WorldStreamManager worldStreamManager,
            BlockManager blockManager,
            long chunkCoordinate,
            int subX, int subY, int subZ) {

        int cellX = Math.floorDiv(subX, SubBlockUtility.DIVISIONS);
        int cellY = Math.floorDiv(subY, SubBlockUtility.DIVISIONS);
        int cellZ = Math.floorDiv(subZ, SubBlockUtility.DIVISIONS);

        if (cellY < 0 || cellY >= WORLD_TOP_CELL)
            return null;

        int chunkOffsetX = Math.floorDiv(cellX, CHUNK_SIZE);
        int chunkOffsetZ = Math.floorDiv(cellZ, CHUNK_SIZE);

        if (chunkOffsetX != 0 || chunkOffsetZ != 0)
            chunkCoordinate = Coordinate2Long.add(chunkCoordinate, chunkOffsetX, chunkOffsetZ);

        ChunkInstance chunk = worldStreamManager.getChunkInstance(chunkCoordinate);

        if (chunk == null)
            return null;

        SubChunkInstance subChunk = chunk.getSubChunk(cellY / CHUNK_SIZE);
        int cellXYZ = Coordinate3Int.pack(
                Math.floorMod(cellX, CHUNK_SIZE),
                cellY % CHUNK_SIZE,
                Math.floorMod(cellZ, CHUNK_SIZE));

        int octant = SubBlockUtility.getOctant(
                Math.floorMod(subX, SubBlockUtility.DIVISIONS),
                Math.floorMod(subY, SubBlockUtility.DIVISIONS),
                Math.floorMod(subZ, SubBlockUtility.DIVISIONS));

        if (!SubBlockUtility.hasOctant(subChunk.getSubBlockMask(cellXYZ), octant))
            return null;

        BlockHandle blockHandle = blockManager.getBlockHandleFromBlockID(subChunk.getBlock(cellXYZ));

        return blockHandle.getGeometry() == DynamicGeometryType.NONE ? null : blockHandle;
    }

    /*
     * Whether a sub-cell blocks movement. Liquid never does, and below the
     * world is treated as solid so nothing can ever fall out of it.
     */
    public static boolean isSolid(
            WorldStreamManager worldStreamManager,
            BlockManager blockManager,
            long chunkCoordinate,
            int subX, int subY, int subZ) {

        if (subY < 0)
            return true;

        BlockHandle blockHandle = getSubBlockAt(worldStreamManager, blockManager, chunkCoordinate, subX, subY, subZ);

        return blockHandle != null && blockHandle.getGeometry() != DynamicGeometryType.LIQUID;
    }

    // Conversion \\

    // The sub-cell index holding a coordinate given in blocks
    public static int toSub(float blocks) {
        return (int) Math.floor(blocks / SubBlockUtility.SIZE);
    }
}
