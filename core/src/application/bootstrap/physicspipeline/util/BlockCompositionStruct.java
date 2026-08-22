package application.bootstrap.physicspipeline.util;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector3Int;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

public class BlockCompositionStruct extends StructPackage {

    /*
     * Tracks the set of blocks an entity's AABB currently occupies and every
     * block adjacent to it per face direction, re-deriving the occupied span
     * per axis from the entity's real size and fractional position on every
     * call rather than caching a fixed ceil(size) — an AABB offset from a
     * cell boundary can touch one more cell than its raw size implies, and a
     * footprint that misses that silently drops the neighboring block a
     * collision check needed to see. Rebuilt only when the floor cell or the
     * resulting span changes on any axis. Used by BlockCollisionBranch for
     * per-axis collision queries.
     */

    // Internal
    private Vector3Int currentBlock;
    private Vector3Int currentSpan;
    private Int2LongOpenHashMap blockCompositionMap;
    private Int2LongOpenHashMap[] blockCoordinate2ChunkCoordinate;

    // Settings
    private int directionCount;
    private int chunkSize;
    private int worldHeight;
    private float straddleEpsilon;

    // Constructor \\

    public BlockCompositionStruct() {

        // Settings
        this.directionCount = Direction3Vector.LENGTH;
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.worldHeight = EngineSetting.WORLD_HEIGHT * chunkSize;
        this.straddleEpsilon = EngineSetting.BLOCK_STRADDLE_EPSILON;

        // Internal
        this.currentBlock = new Vector3Int();
        this.currentSpan = new Vector3Int();
        this.blockCompositionMap = new Int2LongOpenHashMap();
        this.blockCoordinate2ChunkCoordinate = new Int2LongOpenHashMap[directionCount];

        for (int i = 0; i < directionCount; i++)
            blockCoordinate2ChunkCoordinate[i] = new Int2LongOpenHashMap();
    }

    // Utility \\

    public void updateBlockComposition(
            Vector3 entitySize,
            Vector3 currentPosition,
            long chunkCoordinate) {

        int minX = (int) Math.floor(currentPosition.x);
        int minY = (int) Math.floor(currentPosition.y);
        int minZ = (int) Math.floor(currentPosition.z);

        int spanX = computeSpan(currentPosition.x, entitySize.x);
        int spanY = computeSpan(currentPosition.y, entitySize.y);
        int spanZ = computeSpan(currentPosition.z, entitySize.z);

        if (minX == currentBlock.x && minY == currentBlock.y && minZ == currentBlock.z
                && spanX == currentSpan.x && spanY == currentSpan.y && spanZ == currentSpan.z)
            return;

        currentBlock.x = minX;
        currentBlock.y = minY;
        currentBlock.z = minZ;

        currentSpan.x = spanX;
        currentSpan.y = spanY;
        currentSpan.z = spanZ;

        buildBlockComposition(chunkCoordinate);
        buildAdjacentBlocks(chunkCoordinate);
    }

    private int computeSpan(float position, float size) {

        int minCell = (int) Math.floor(position);
        int maxCell = (int) Math.floor(position + size - straddleEpsilon);

        return Math.max(1, maxCell - minCell + 1);
    }

    private void buildBlockComposition(long chunkCoordinate) {

        blockCompositionMap.clear();

        for (int blockX = 0; blockX < currentSpan.x; blockX++)
            for (int blockY = 0; blockY < currentSpan.y; blockY++)
                for (int blockZ = 0; blockZ < currentSpan.z; blockZ++)
                    addBlockToMap(
                            currentBlock.x + blockX,
                            currentBlock.y + blockY,
                            currentBlock.z + blockZ,
                            chunkCoordinate,
                            blockCompositionMap);
    }

    private void buildAdjacentBlocks(long chunkCoordinate) {

        for (int i = 0; i < directionCount; i++) {

            Direction3Vector direction = Direction3Vector.VALUES[i];
            Int2LongOpenHashMap directionMap = blockCoordinate2ChunkCoordinate[i];
            directionMap.clear();

            Direction3Vector[] tangents = Direction3Vector.getTangents(direction);
            Direction3Vector tangentA = tangents[0];
            Direction3Vector tangentB = tangents[1];

            int faceX = currentBlock.x + (direction.x > 0 ? currentSpan.x : direction.x);
            int faceY = currentBlock.y + (direction.y > 0 ? currentSpan.y : direction.y);
            int faceZ = currentBlock.z + (direction.z > 0 ? currentSpan.z : direction.z);

            int sizeA = (tangentA.x != 0) ? currentSpan.x
                    : (tangentA.y != 0) ? currentSpan.y : currentSpan.z;
            int sizeB = (tangentB.x != 0) ? currentSpan.x
                    : (tangentB.y != 0) ? currentSpan.y : currentSpan.z;

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

    public Vector3Int getCurrentSpan() {
        return currentSpan;
    }
}