package application.bootstrap.worldpipeline.util;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class LiquidColumnUtility extends EngineUtility {

    /*
     * Block-space liquid queries shared by anything that needs to know where
     * a water surface actually is — currently just SwimBranch. totalY follows
     * the same convention as WorldPositionUtility.findSafeSpawnHeight: an
     * absolute block Y, unrolled across every subchunk in the column
     * (0..WORLD_HEIGHT * CHUNK_SIZE), not a chunk-local coordinate.
     */

    public static final float NO_SURFACE = Float.NaN;

    private static final int WORLD_TOP_Y = EngineSetting.WORLD_HEIGHT * EngineSetting.CHUNK_SIZE;

    // Lookup \\

    public static BlockHandle getBlockAt(
            ChunkInstance chunkInstance,
            BlockManager blockManager,
            int blockX,
            int totalY,
            int blockZ) {

        if (totalY < 0 || totalY >= WORLD_TOP_Y)
            return null;

        int subChunkIndex = totalY / EngineSetting.CHUNK_SIZE;
        int localY = totalY % EngineSetting.CHUNK_SIZE;

        SubChunkInstance subChunk = chunkInstance.getSubChunk(subChunkIndex);
        short blockID = subChunk.getBlock(blockX, localY, blockZ);

        return blockManager.getBlockHandleFromBlockID(blockID);
    }

    public static boolean isLiquid(BlockHandle block) {
        return block != null && block.getGeometry() == DynamicGeometryType.LIQUID;
    }

    // Surface \\

    /*
     * Walks upward from fromTotalY through consecutive blocks of the same
     * liquid type, returning where the fluid actually ends: either the
     * fractional fill height of the first partially-filled block found
     * (LiquidTickBranch/FluidSimulationSystem only ever leave a partial
     * level at a true surface — everything below is packed to
     * LIQUID_LEVEL_MAX), or the integer top of the last full block if the
     * column runs straight into air. Returns NO_SURFACE if fromTotalY
     * itself isn't inside liquid at all.
     */
    public static float findSurfaceHeight(
            ChunkInstance chunkInstance,
            BlockManager blockManager,
            int blockX,
            int fromTotalY,
            int blockZ) {

        BlockHandle startBlock = getBlockAt(chunkInstance, blockManager, blockX, fromTotalY, blockZ);

        if (!isLiquid(startBlock))
            return NO_SURFACE;

        short liquidBlockID = startBlock.getBlockID();
        int scanY = Math.max(0, fromTotalY);

        for (; scanY < WORLD_TOP_Y; scanY++) {

            int subChunkIndex = scanY / EngineSetting.CHUNK_SIZE;
            int localY = scanY % EngineSetting.CHUNK_SIZE;

            SubChunkInstance subChunk = chunkInstance.getSubChunk(subChunkIndex);
            short blockID = subChunk.getBlock(blockX, localY, blockZ);

            if (blockID != liquidBlockID)
                return scanY;

            int level = subChunk.getLiquidLevel(blockX, localY, blockZ);

            if (level < EngineSetting.LIQUID_LEVEL_MAX)
                return scanY + ((float) level / EngineSetting.LIQUID_LEVEL_MAX);
        }

        return WORLD_TOP_Y;
    }
}