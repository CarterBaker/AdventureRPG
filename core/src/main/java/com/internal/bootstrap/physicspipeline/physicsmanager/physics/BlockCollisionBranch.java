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

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

public class BlockCollisionBranch extends BranchPackage {

    // Internal
    private ChunkStreamManager chunkStreamManager;
    private BlockManager blockManager;

    private int CHUNK_SIZE;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {

        // Internal
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Utility \\

    public void calculate(
            Vector3 position,
            Vector3 movement,
            EntityHandle entityHandle) {

        BlockCompositionStruct blockCompositionStruct = entityHandle.getBlockCompositionStruct();
        Vector3 size = entityHandle.getSize();

        if (movement.x != 0) {
            Direction3Vector direction = Direction3Vector.getDirectionX((int) Math.signum(movement.x));
            if (hasCollisionInDirection(
                    blockCompositionStruct,
                    direction,
                    entityHandle,
                    position.x,
                    size.x,
                    movement.x))
                movement.x = 0;
        }

        if (movement.y != 0) {
            Direction3Vector direction = Direction3Vector.getDirectionY((int) Math.signum(movement.y));
            if (hasCollisionInDirection(
                    blockCompositionStruct,
                    direction,
                    entityHandle,
                    position.y,
                    size.y,
                    movement.y))
                movement.y = 0;
        }

        if (movement.z != 0) {
            Direction3Vector direction = Direction3Vector.getDirectionZ((int) Math.signum(movement.z));
            if (hasCollisionInDirection(
                    blockCompositionStruct,
                    direction,
                    entityHandle,
                    position.z,
                    size.z,
                    movement.z))
                movement.z = 0;
        }
    }

    private boolean hasCollisionInDirection(
            BlockCompositionStruct blockCompositionStruct,
            Direction3Vector direction,
            EntityHandle entityHandle,
            float axisPosition,
            float axisSize,
            float axisMovement) {

        Int2LongOpenHashMap adjacentBlocks = blockCompositionStruct.getAllBlocksForSide(direction);

        // Iterate through all adjacent blocks in this direction
        for (Int2LongMap.Entry entry : adjacentBlocks.int2LongEntrySet()) {
            int blockCoordinate = entry.getIntKey();
            long chunkCoordinate = entry.getLongValue();

            // Get chunk
            ChunkInstance chunk = chunkStreamManager.getChunkInstance(chunkCoordinate);
            if (chunk == null)
                continue;

            // Get subchunk
            int localX = Coordinate3Int.unpackX(blockCoordinate);
            int localY = Coordinate3Int.unpackY(blockCoordinate);
            int localZ = Coordinate3Int.unpackZ(blockCoordinate);

            int subChunkY = localY / CHUNK_SIZE;
            int subLocalY = localY % CHUNK_SIZE;

            SubChunkInstance subChunk = chunk.getSubChunk(subChunkY);
            if (subChunk == null)
                continue;

            int localCoordinate = Coordinate3Int.pack(localX, subLocalY, localZ);

            // Get block
            short blockID = subChunk.getBlockPaletteHandle().getBlock(localCoordinate);
            BlockHandle block = blockManager.getBlockFromBlockID(blockID);

            if (isColliding(
                    block,
                    direction,
                    axisPosition,
                    axisSize,
                    axisMovement))
                return true;
        }

        return false;
    }

    private boolean isColliding(
            BlockHandle block,
            Direction3Vector direction,
            float axisPosition,
            float axisSize,
            float axisMovement) {

        // Not a solid block
        if (block == null || block.getGeometry() == DynamicGeometryType.NONE)
            return false;

        // Get fractional position within current block
        float fractional = axisPosition - (int) axisPosition;

        // Negative direction: origin is moving backward
        if (direction.negative) {
            float newPosition = fractional + axisMovement;
            return newPosition <= 0.0f;
        }

        // Positive direction: the far edge (origin + size) is moving forward
        float edgePosition = fractional + axisSize;
        float newEdgePosition = edgePosition + axisMovement;
        return newEdgePosition >= 1.0f;
    }
}