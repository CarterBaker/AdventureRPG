package com.internal.bootstrap.physicspipeline.util;

import com.internal.core.engine.StructPackage;
import com.internal.core.settings.EngineSetting;
import com.internal.core.util.mathematics.extras.Coordinate2Long;
import com.internal.core.util.mathematics.extras.Coordinate3Int;
import com.internal.core.util.mathematics.extras.Direction3Vector;
import com.internal.core.util.mathematics.vectors.Vector3;
import com.internal.core.util.mathematics.vectors.Vector3Int;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

public class BlockCompositionStruct extends StructPackage {

    /*
     * Tracks the set of blocks occupied by an entity and all adjacent blocks
     * per face direction. Rebuilt only when the entity moves to a new block.
     * Used by BlockCollisionBranch for per-axis collision queries.
     */

    // Internal
    private Vector3Int currentBlock;
    private Int2LongOpenHashMap blockCompositionMap;
    private Int2LongOpenHashMap[] blockCoordinate2ChunkCoordinate;

    // Settings
    private int directionCount;
    private int chunkSize;
    private int worldHeight;

    // Constructor \\

    public BlockCompositionStruct() {

        // Settings
        this.directionCount = Direction3Vector.LENGTH;
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.worldHeight = EngineSetting.WORLD_HEIGHT * chunkSize;

        // Internal
        this.currentBlock = new Vector3Int();
        this.blockCompositionMap = new Int2LongOpenHashMap();
        this.blockCoordinate2ChunkCoordinate = new Int2LongOpenHashMap[directionCount];

        for (int i = 0; i < directionCount; i++)
            blockCoordinate2ChunkCoordinate[i] = new Int2LongOpenHashMap();
    }

    // Utility \\

    public void updateBlockComposition(
            Vector3Int blockComposition,
            Vector3 currentPosition,
            long chunkCoordinate) {

        int x = (int) currentPosition.x;
        int y = (int) currentPosition.y;
        int z = (int) currentPosition.z;

        if (x == currentBlock.x && y == currentBlock.y && z == currentBlock.z)
            return;

        currentBlock.x = x;
        currentBlock.y = y;
        currentBlock.z = z;

        buildBlockComposition(blockComposition, chunkCoordinate);
        buildAdjacentBlocks(blockComposition, chunkCoordinate);
    }

    private void buildBlockComposition(
            Vector3Int blockComposition,
            long chunkCoordinate) {

        blockCompositionMap.clear();

        for (int blockX = 0; blockX < blockComposition.x; blockX++)
            for (int blockY = 0; blockY < blockComposition.y; blockY++)
                for (int blockZ = 0; blockZ < blockComposition.z; blockZ++)
                    addBlockToMap(
                            currentBlock.x + blockX,
                            currentBlock.y + blockY,
                            currentBlock.z + blockZ,
                            chunkCoordinate,
                            blockCompositionMap);
    }

    private void buildAdjacentBlocks(
            Vector3Int blockComposition,
            long chunkCoordinate) {

        for (int i = 0; i < directionCount; i++) {

            Direction3Vector direction = Direction3Vector.VALUES[i];
            Int2LongOpenHashMap directionMap = blockCoordinate2ChunkCoordinate[i];
            directionMap.clear();

            Direction3Vector[] tangents = Direction3Vector.getTangents(direction);
            Direction3Vector tangentA = tangents[0];
            Direction3Vector tangentB = tangents[1];

            int faceX = currentBlock.x + (direction.x > 0 ? blockComposition.x : direction.x);
            int faceY = currentBlock.y + (direction.y > 0 ? blockComposition.y : direction.y);
            int faceZ = currentBlock.z + (direction.z > 0 ? blockComposition.z : direction.z);

            int sizeA = (tangentA.x != 0) ? blockComposition.x
                    : (tangentA.y != 0) ? blockComposition.y : blockComposition.z;
            int sizeB = (tangentB.x != 0) ? blockComposition.x
                    : (tangentB.y != 0) ? blockComposition.y : blockComposition.z;

            for (int a = 0; a < sizeA; a++) {
                for (int b = 0; b < sizeB; b++) {

                    int blockX = faceX + (tangentA.x * a) + (tangentB.x * b);
                    int blockY = faceY + (tangentA.y * a) + (tangentB.y * b);
                    int blockZ = faceZ + (tangentA.z * a) + (tangentB.z * b);

                    addBlockToMap(blockX, blockY, blockZ, chunkCoordinate, directionMap);
                }
            }
        }
    }

    private void addBlockToMap(
            int blockX,
            int blockY,
            int blockZ,
            long chunkCoordinate,
            Int2LongOpenHashMap map) {

        int chunkOffsetX = 0;
        int chunkOffsetZ = 0;

        if (blockX < 0) {
            chunkOffsetX = -1;
            blockX += chunkSize;
        } else if (blockX >= chunkSize) {
            chunkOffsetX = 1;
            blockX -= chunkSize;
        }

        if (blockY < 0)
            blockY = 0;
        else if (blockY >= worldHeight)
            blockY = worldHeight - 1;

        if (blockZ < 0) {
            chunkOffsetZ = -1;
            blockZ += chunkSize;
        } else if (blockZ >= chunkSize) {
            chunkOffsetZ = 1;
            blockZ -= chunkSize;
        }

        if (chunkOffsetX != 0 || chunkOffsetZ != 0)
            chunkCoordinate = Coordinate2Long.add(chunkCoordinate, chunkOffsetX, chunkOffsetZ);

        int blockCoordinate = Coordinate3Int.pack(blockX, blockY, blockZ);

        map.put(blockCoordinate, chunkCoordinate);
    }

    // Accessible \\

    public Int2LongOpenHashMap getBlockCompositionMap() {
        return blockCompositionMap;
    }

    public Int2LongOpenHashMap getAllBlocksForSide(Direction3Vector direction) {
        return blockCoordinate2ChunkCoordinate[direction.index];
    }
}