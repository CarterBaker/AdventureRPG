package application.bootstrap.worldpipeline.util;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate2Long;

public class ColumnHeightUtility extends EngineUtility {

    /*
     * Block-space column queries for anything that needs to know what lies
     * open to the sky — precipitation sheltering, and where the wind's lines
     * and the falling leaves meet the ground. A column's top is the absolute
     * block Y directly above its highest block with any geometry, liquids
     * included, using the same unrolled totalY convention as
     * LiquidColumnUtility. The scan walks down from the top of the world and
     * skips whole subchunks known to be empty, so an open-air column costs a
     * handful of flag checks rather than a thousand block reads. A column is
     * named either by its chunk and place in it, or by its absolute block X
     * and Z, wrapped into the world.
     */

    // Column \\

    // The top of an absolute block column, read from the chunk holding it, or INDEX_NOT_FOUND while that chunk is not
    // loaded or not yet generated
    public static int findColumnTop(
            WorldStreamManager worldStreamManager,
            BlockManager blockManager,
            WorldHandle world,
            int columnX,
            int columnZ) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(resolveColumnChunk(world, columnX, columnZ));

        if (chunk == null || !chunk.getChunkDataSyncContainer().hasData(ChunkData.GENERATION_DATA))
            return EngineSetting.INDEX_NOT_FOUND;

        return findColumnTop(
                chunk,
                blockManager,
                Math.floorMod(columnX, EngineSetting.CHUNK_SIZE),
                Math.floorMod(columnZ, EngineSetting.CHUNK_SIZE));
    }

    // The chunk an absolute block column stands in, wrapped into the world
    public static long resolveColumnChunk(WorldHandle world, int columnX, int columnZ) {
        return WorldWrapUtility.wrapAroundWorld(
                world,
                Coordinate2Long.pack(
                        Math.floorDiv(columnX, EngineSetting.CHUNK_SIZE),
                        Math.floorDiv(columnZ, EngineSetting.CHUNK_SIZE)));
    }

    public static int findColumnTop(ChunkInstance chunkInstance, BlockManager blockManager, int blockX, int blockZ) {

        for (int subChunkIndex = EngineSetting.WORLD_HEIGHT - 1; subChunkIndex >= 0; subChunkIndex--) {

            SubChunkInstance subChunk = chunkInstance.getSubChunk(subChunkIndex);

            if (subChunk == null || subChunk.isKnownEmpty())
                continue;

            int subChunkBase = subChunkIndex * EngineSetting.CHUNK_SIZE;

            if (subChunk.isUniformFill()) {

                if (subChunk.getUniformGeometryType() != DynamicGeometryType.NONE)
                    return subChunkBase + EngineSetting.CHUNK_SIZE;

                continue;
            }

            for (int localY = EngineSetting.CHUNK_SIZE - 1; localY >= 0; localY--) {

                BlockHandle block = blockManager.getBlockHandleFromBlockID(subChunk.getBlock(blockX, localY, blockZ));

                if (block != null && block.getGeometry() != DynamicGeometryType.NONE)
                    return subChunkBase + localY + 1;
            }
        }

        return 0;
    }
}
