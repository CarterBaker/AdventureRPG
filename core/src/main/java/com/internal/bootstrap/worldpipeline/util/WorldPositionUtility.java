package com.internal.bootstrap.worldpipeline.util;

import java.util.concurrent.ThreadLocalRandom;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.UtilityPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.vectors.Vector2Int;

public class WorldPositionUtility extends UtilityPackage {

    public static long getRandomChunk(WorldHandle worldHandle) {

        Vector2Int scale = worldHandle.getWorldScale();

        int maxX = scale.x;
        int maxY = scale.y;

        int x = ThreadLocalRandom.current().nextInt(0, maxX);
        int y = ThreadLocalRandom.current().nextInt(0, maxY);

        return Coordinate2Long.pack(x, y);
    }

    public static int findSafeSpawnHeight(
            ChunkInstance chunkInstance,
            BlockManager blockManager,
            int blockX,
            int totalY,
            int blockZ) {

        // Check if current position is already safe
        if (totalY >= 0 && totalY < EngineSetting.WORLD_HEIGHT * EngineSetting.CHUNK_SIZE) {

            int subChunkIndex = totalY / EngineSetting.CHUNK_SIZE;
            int localY = totalY % EngineSetting.CHUNK_SIZE;

            SubChunkInstance subChunk = chunkInstance.getSubChunk(subChunkIndex);
            short blockId = subChunk.getBlock(blockX, localY, blockZ);
            BlockHandle block = blockManager.getBlockFromBlockID(blockId);

            if (block.getGeometry() == DynamicGeometryType.NONE && totalY > 0) {

                int belowSubChunk = (totalY - 1) / EngineSetting.CHUNK_SIZE;
                int belowLocalY = (totalY - 1) % EngineSetting.CHUNK_SIZE;

                SubChunkInstance belowChunk = chunkInstance.getSubChunk(belowSubChunk);
                short belowBlockId = belowChunk.getBlock(blockX, belowLocalY, blockZ);
                BlockHandle belowBlock = blockManager.getBlockFromBlockID(belowBlockId);

                if (belowBlock.getGeometry() != DynamicGeometryType.NONE)
                    return totalY;
            }
        }

        // Search from top down
        for (int subChunkIndex = EngineSetting.WORLD_HEIGHT - 1; subChunkIndex >= 0; subChunkIndex--) {
            SubChunkInstance subChunk = chunkInstance.getSubChunk(subChunkIndex);

            for (int localY = EngineSetting.CHUNK_SIZE - 1; localY >= 0; localY--) {
                short blockId = subChunk.getBlock(blockX, localY, blockZ);
                BlockHandle block = blockManager.getBlockFromBlockID(blockId);

                if (block.getGeometry() != DynamicGeometryType.NONE) {
                    int totalYHere = subChunkIndex * EngineSetting.CHUNK_SIZE + localY;

                    if (localY + 1 < EngineSetting.CHUNK_SIZE) {
                        // Above block is in same subchunk
                        short aboveBlockId = subChunk.getBlock(blockX, localY + 1, blockZ);
                        BlockHandle aboveBlock = blockManager.getBlockFromBlockID(aboveBlockId);

                        if (aboveBlock.getGeometry() == DynamicGeometryType.NONE) {
                            return totalYHere + 1;
                        }
                    } else if (subChunkIndex + 1 < EngineSetting.WORLD_HEIGHT) {
                        // Above block is in next subchunk
                        SubChunkInstance aboveChunk = chunkInstance.getSubChunk(subChunkIndex + 1);
                        short aboveBlockId = aboveChunk.getBlock(blockX, 0, blockZ);
                        BlockHandle aboveBlock = blockManager.getBlockFromBlockID(aboveBlockId);

                        if (aboveBlock.getGeometry() == DynamicGeometryType.NONE) {
                            return totalYHere + 1;
                        }
                    }
                }
            }
        }

        return -1;
    }
}
