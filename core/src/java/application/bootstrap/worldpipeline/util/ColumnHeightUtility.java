package application.bootstrap.worldpipeline.util;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class ColumnHeightUtility extends EngineUtility {

    /*
     * Block-space column queries for anything that needs to know what lies
     * open to the sky — precipitation sheltering today. A column's top is
     * the absolute block Y directly above its highest block with any
     * geometry, liquids included, using the same unrolled totalY convention
     * as LiquidColumnUtility. The scan walks down from the top of the world
     * and skips whole subchunks known to be empty, so an open-air column
     * costs a handful of flag checks rather than a thousand block reads.
     */

    // Column \\

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
