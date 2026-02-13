package com.internal.bootstrap.physicspipeline.util;

import com.internal.core.engine.StructPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.Extras.Coordinate3Int;
import com.internal.core.util.mathematics.Extras.Direction3Vector;
import com.internal.core.util.mathematics.vectors.Vector3;
import com.internal.core.util.mathematics.vectors.Vector3Int;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

public class BlockCompositionStruct extends StructPackage {

    // internal
    private Vector3Int currentBlock;
    private Int2LongOpenHashMap blockCompositionMap;
    private Int2LongOpenHashMap[] blockCoordinate2ChunkCoordinate;

    private int DIRECTION_COUNT;
    private int CHUNK_SIZE;
    private int WORLD_HEIGHT;

    // Internal \\

    public BlockCompositionStruct() {

        // Internal
        this.DIRECTION_COUNT = Direction3Vector.LENGTH;
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        this.WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT * CHUNK_SIZE;

        this.currentBlock = new Vector3Int();
        this.blockCompositionMap = new Int2LongOpenHashMap();
        this.blockCoordinate2ChunkCoordinate = new Int2LongOpenHashMap[DIRECTION_COUNT];
        for (int i = 0; i < DIRECTION_COUNT; i++)
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

        if (x == currentBlock.x &&
                y == currentBlock.y &&
                z == currentBlock.z)
            return;

        currentBlock.x = x;
        currentBlock.y = y;
        currentBlock.z = z;

        buildBlockComposition(
                blockComposition,
                chunkCoordinate);

        buildAdjacentBlocks(
                blockComposition,
                chunkCoordinate);
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

        for (int i = 0; i < DIRECTION_COUNT; i++) {

            Direction3Vector direction = Direction3Vector.VALUES[i];
            Int2LongOpenHashMap directionMap = this.blockCoordinate2ChunkCoordinate[i];
            directionMap.clear();

            // Get tangents (perpendicular axes)
            Direction3Vector[] tangents = Direction3Vector.getTangents(direction);
            Direction3Vector tangentA = tangents[0];
            Direction3Vector tangentB = tangents[1];

            // Calculate face base position
            int faceX = currentBlock.x + (direction.x > 0 ? blockComposition.x : direction.x);
            int faceY = currentBlock.y + (direction.y > 0 ? blockComposition.y : direction.y);
            int faceZ = currentBlock.z + (direction.z > 0 ? blockComposition.z : direction.z);

            // Determine sizes along tangent directions
            int sizeA = (tangentA.x != 0) ? blockComposition.x
                    : (tangentA.y != 0) ? blockComposition.y : blockComposition.z;
            int sizeB = (tangentB.x != 0) ? blockComposition.x
                    : (tangentB.y != 0) ? blockComposition.y : blockComposition.z;

            // Iterate the 2D face
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
        int chunkOffsetY = 0;

        // X axis
        if (blockX < 0) {
            chunkOffsetX = -1;
            blockX += CHUNK_SIZE;
        }

        else if (blockX >= CHUNK_SIZE) {
            chunkOffsetX = 1;
            blockX -= CHUNK_SIZE;
        }

        // Y axis
        if (blockY < 0)
            blockY = 0;

        else if (blockY >= WORLD_HEIGHT)
            blockY = WORLD_HEIGHT - 1;

        // Z axis
        if (blockZ < 0) {
            chunkOffsetY = -1;
            blockZ += CHUNK_SIZE;
        }

        else if (blockZ >= CHUNK_SIZE) {
            chunkOffsetY = 1;
            blockZ -= CHUNK_SIZE;
        }

        if (chunkOffsetX != 0 || chunkOffsetY != 0)
            chunkCoordinate = Coordinate2Long.add(chunkCoordinate, chunkOffsetX, chunkOffsetY);
        int blockCoordinate = Coordinate3Int.pack(blockX, blockY, blockZ);

        map.put(blockCoordinate, chunkCoordinate);
    }

    // Accessiblity \\

    public Int2LongOpenHashMap getBlockCompositionMap() {
        return blockCompositionMap;
    }

    public Int2LongOpenHashMap getAllBlocksForSide(Direction3Vector direction) {
        return blockCoordinate2ChunkCoordinate[direction.index];
    }
}