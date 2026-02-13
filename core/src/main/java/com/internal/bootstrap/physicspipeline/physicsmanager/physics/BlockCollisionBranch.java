// BlockCollisionBranch.java
package com.internal.bootstrap.physicspipeline.physicsmanager.physics;

import com.internal.bootstrap.entitypipeline.entityManager.EntityHandle;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import com.internal.bootstrap.physicspipeline.util.BlockCompositionStruct;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate3Int;
import com.internal.core.util.mathematics.Extras.Direction3Vector;
import com.internal.core.util.mathematics.vectors.Vector3;
import com.internal.core.util.mathematics.vectors.Vector3Int;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class BlockCollisionBranch extends BranchPackage {

    // Internal
    private ChunkStreamManager chunkStreamManager;
    private BlockManager blockManager;

    private int CHUNK_SIZE;
    private int WORLD_HEIGHT;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        this.WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT * CHUNK_SIZE;
    }

    @Override
    protected void get() {

        // Internal
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Utility \\

    public void calculate(
            Vector3Int input,
            Vector3 position,
            Vector3 movement,
            EntityHandle entityHandle) {

        BlockCompositionStruct blockCompositionStruct = entityHandle.getBlockCompositionStruct();

        if (input.x != 0) {

            Direction3Vector direction = Direction3Vector.getDirectionX(input.x);
            if (hasCollisionInDirection(blockCompositionStruct, direction, entityHandle))
                movement.x = 0;
        }

        if (input.y != 0) {

            Direction3Vector direction = Direction3Vector.getDirectionY(input.y);
            if (hasCollisionInDirection(blockCompositionStruct, direction, entityHandle))
                movement.y = 0;
        }

        if (input.z != 0) {

            Direction3Vector direction = Direction3Vector.getDirectionZ(input.z);
            if (hasCollisionInDirection(blockCompositionStruct, direction, entityHandle))
                movement.z = 0;
        }
    }

    private boolean hasCollisionInDirection(
            BlockCompositionStruct blockCompositionStruct,
            Direction3Vector directionection,
            EntityHandle entityHandle) {

        Long2IntOpenHashMap adjacentBlocks = blockCompositionStruct.getAllBlocksForSide(directionection);

        // Iterate through all adjacent blocks in this directionection
        for (Long2IntMap.Entry entry : adjacentBlocks.long2IntEntrySet()) {
            long chunkCoordinate = entry.getLongKey();
            int blockCoordinate = entry.getIntValue();

            // Get chunk
            ChunkInstance chunk = chunkStreamManager.getChunkInstance(chunkCoordinate);
            if (chunk == null)
                continue;

            // Get subchunk
            int localX = Coordinate3Int.unpackX(blockCoordinate);
            int worldY = Coordinate3Int.unpackY(blockCoordinate);
            int localZ = Coordinate3Int.unpackZ(blockCoordinate);

            int subChunkY = worldY / CHUNK_SIZE; // which subchunk
            int localY = worldY % CHUNK_SIZE; // block inside subchunk

            SubChunkInstance subChunk = chunk.getSubChunk(subChunkY);
            if (subChunk == null)
                continue;

            int localCoordinate = Coordinate3Int.pack(localX, localY, localZ);

            // Get block
            short blockID = subChunk.getBlockPaletteHandle().getBlock(localCoordinate);
            BlockHandle block = blockManager.getBlockFromBlockID(blockID);

            // Check if solid
            if (block != null && block.getGeometry() != DynamicGeometryType.NONE)
                return true; // Collision detected
        }

        return false; // No collision
    }
}