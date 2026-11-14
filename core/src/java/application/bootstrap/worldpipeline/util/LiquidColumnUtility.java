package application.bootstrap.worldpipeline.util;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate3Int;

public class LiquidColumnUtility extends EngineUtility {

    /*
     * Block-space liquid queries shared by anything that needs to know where
     * a water surface actually is — SwimBranch, and WaveManager deciding
     * whether a camera is under the sea. findWaterY() is the one place that
     * decides which liquid a position stands in, a passing wave crest over
     * the open air above the tide line included. totalY follows the same
     * convention as WorldPositionUtility.findSafeSpawnHeight: an absolute
     * block Y, unrolled across every subchunk in the column
     * (0..WORLD_HEIGHT * CHUNK_SIZE), not a chunk-local coordinate.
     */

    public static final float NO_SURFACE = EngineSetting.LIQUID_NO_SURFACE;
    public static final int NO_WATER = EngineSetting.INDEX_NOT_FOUND;

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

    public static boolean isTidal(ChunkInstance chunkInstance, int blockX, int totalY, int blockZ) {

        if (totalY < 0 || totalY >= WORLD_TOP_Y)
            return false;

        SubChunkInstance subChunk = chunkInstance.getSubChunk(totalY / EngineSetting.CHUNK_SIZE);

        return subChunk.isLiquidTidal(Coordinate3Int.pack(blockX, totalY % EngineSetting.CHUNK_SIZE, blockZ));
    }

    // Water \\

    // The totalY of the liquid block a position at totalY can be under: its own block when that is liquid, or the
    // tidal block just beneath the tide line when the position is in the open air above it, where a wave crest
    // still passes over. Whether the surface actually covers the position is the caller's to compare.
    public static int findWaterY(
            ChunkInstance chunkInstance,
            BlockManager blockManager,
            int blockX,
            int totalY,
            int blockZ,
            float tideSurfaceHeight) {

        if (isLiquid(getBlockAt(chunkInstance, blockManager, blockX, totalY, blockZ)))
            return totalY;

        int beneathTideY = (int) Math.floor(tideSurfaceHeight) - 1;

        if (totalY <= beneathTideY)
            return NO_WATER;

        boolean tidalBeneath = isLiquid(getBlockAt(chunkInstance, blockManager, blockX, beneathTideY, blockZ))
                && isTidal(chunkInstance, blockX, beneathTideY, blockZ);

        return tidalBeneath ? beneathTideY : NO_WATER;
    }

    // Surface \\

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

    // Floor \\

    public static float findFloorHeight(
            ChunkInstance chunkInstance,
            BlockManager blockManager,
            int blockX,
            int fromTotalY,
            int blockZ,
            int minTotalY) {

        int floorLimit = Math.max(0, minTotalY);

        for (int scanY = fromTotalY; scanY >= floorLimit; scanY--) {

            BlockHandle block = getBlockAt(chunkInstance, blockManager, blockX, scanY, blockZ);

            if (!isLiquid(block))
                return scanY + 1;
        }

        return floorLimit;
    }
}