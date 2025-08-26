package com.AdventureRPG.WorldSystem.Chunks;

import java.util.BitSet;

import com.AdventureRPG.Util.Direction2Int;
import com.AdventureRPG.Util.Direction3Int;
import com.AdventureRPG.WorldSystem.PackedCoordinate3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Blocks.Type;

public class ChunkBuilder {

    // Game Manager
    private final WorldSystem worldSystem;
    private final PackedCoordinate3Int packedCoordinate3Int;

    // Settings
    private final int CHUNK_SIZE;
    private final int WORLD_HEIGHT;

    // Coordinate Tracking
    BitSet passedCoordinates;
    BitSet batchedBlocks;

    public ChunkBuilder(WorldSystem worldSystem) {

        // Game Manager
        this.worldSystem = worldSystem;
        this.packedCoordinate3Int = worldSystem.packedCoordinate3Int;

        // Settings
        this.CHUNK_SIZE = worldSystem.settings.CHUNK_SIZE;
        this.WORLD_HEIGHT = worldSystem.settings.WORLD_HEIGHT;

        // Coordinate Tracking
        this.passedCoordinates = new BitSet(packedCoordinate3Int.chunkSize);
        this.batchedBlocks = new BitSet(packedCoordinate3Int.chunkSize);
    }

    public void build(Chunk chunk, int subChunkIndex) {

        SubChunk subChunk = chunk.getSubChunk(subChunkIndex);

        for (int axisIndex = 0; axisIndex < Axis.values().length; axisIndex++) {

            Axis axis = Axis.VALUES[axisIndex];

            for (int index = 0; index < packedCoordinate3Int.chunkSize; index++) {

                int xyz = packedCoordinate3Int.getPackedBlockCoordinate(index);

                if (batchedBlocks.get(xyz))
                    continue;

                int aX = packedCoordinate3Int.unpackX(xyz);
                int aY = packedCoordinate3Int.unpackY(xyz);
                int aZ = packedCoordinate3Int.unpackZ(xyz);

                int blockID = subChunk.getBlock(aX, aY, aZ);
                Type type = worldSystem.getBlockType(blockID);

                if (type == Type.NULL)
                    continue;

                int biomeID = subChunk.getBiome(aX, aY, aZ);

                for (int directionIndex = 0; directionIndex < 2; directionIndex++) {

                    Direction3Int direction = axis.getDirection(directionIndex);

                    assembleFace(
                            chunk,
                            subChunk,
                            subChunkIndex,
                            aX, aY, aZ,
                            axis, direction,
                            biomeID, blockID,
                            type);
                }
            }
        }

        batchedBlocks.clear();
    }

    private void assembleFace(
            Chunk chunk,
            SubChunk subChunk,
            int subChunkIndex,
            int aX, int aY, int aZ,
            Axis axis, Direction3Int direction,
            int biomeID, int blockID,
            Type type) {

        if (!blockFaceCheck(
                chunk,
                subChunkIndex,
                aX, aY, aZ,
                axis, direction,
                type))
            return;

        boolean checkA = true;
        boolean checkB = true;

        int sizeA = 1;
        int sizeB = 1;

        Direction3Int comparativeDirectionA = axis.getComparativeDirection(0);
        Direction3Int comparativeDirectionB = axis.getComparativeDirection(1);

        do {

            // expand along A
            if (checkA && sizeA <= CHUNK_SIZE) {

                int nextAX = aX + comparativeDirectionA.x * sizeA;
                int nextAY = aY + comparativeDirectionA.y * sizeA;
                int nextAZ = aZ + comparativeDirectionA.z * sizeA;

                if (coordinatesOutOfBounds(nextAX, nextAY, nextAZ)) {

                    checkA = false;
                    continue;
                }

                for (int b = 0; b < sizeB; b++) {

                    int checkX = nextAX + comparativeDirectionB.x * b;
                    int checkY = nextAY + comparativeDirectionB.y * b;
                    int checkZ = nextAZ + comparativeDirectionB.z * b;

                    int xyz = packedCoordinate3Int.pack(checkX, checkY, checkZ);

                    int comparativeBiomeID = subChunk.getBiome(checkX, checkY, checkZ);
                    int comparativeBlockID = subChunk.getBlock(checkX, checkY, checkZ);

                    if (biomeID != comparativeBiomeID ||
                            blockID != comparativeBlockID ||
                            batchedBlocks.get(xyz) ||
                            !blockFaceCheck(
                                    chunk,
                                    subChunkIndex,
                                    checkX, checkY, checkZ,
                                    axis, direction,
                                    type)) {

                        checkA = false;
                        break;
                    }

                    passedCoordinates.set(xyz);
                }

                if (checkA) {

                    batchedBlocks.or(passedCoordinates);
                    sizeA++;
                }

                passedCoordinates.clear();
            }

            // expand along B
            if (checkB && sizeB <= CHUNK_SIZE) {

                int nextBX = aX + comparativeDirectionB.x * sizeB;
                int nextBY = aY + comparativeDirectionB.y * sizeB;
                int nextBZ = aZ + comparativeDirectionB.z * sizeB;

                if (coordinatesOutOfBounds(nextBX, nextBY, nextBZ)) {

                    checkB = false;
                    continue;
                }

                for (int a = 0; a < sizeA; a++) {

                    int checkX = nextBX + comparativeDirectionA.x * a;
                    int checkY = nextBY + comparativeDirectionA.y * a;
                    int checkZ = nextBZ + comparativeDirectionA.z * a;

                    int xyz = packedCoordinate3Int.pack(checkX, checkY, checkZ);

                    int comparativeBiomeID = subChunk.getBiome(checkX, checkY, checkZ);
                    int comparativeBlockID = subChunk.getBlock(checkX, checkY, checkZ);

                    if (biomeID != comparativeBiomeID ||
                            blockID != comparativeBlockID ||
                            batchedBlocks.get(xyz) ||
                            !blockFaceCheck(
                                    chunk,
                                    subChunkIndex,
                                    checkX, checkY, checkZ,
                                    axis, direction,
                                    type)) {

                        checkB = false;
                        break;
                    }

                    passedCoordinates.set(xyz);
                }

                if (checkB) {

                    batchedBlocks.or(passedCoordinates);
                    sizeB++;
                }

                passedCoordinates.clear();
            }
        }

        while (checkA || checkB);

    }

    private boolean coordinatesOutOfBounds(int x, int y, int z) {

        return (x >= CHUNK_SIZE ||
                y >= CHUNK_SIZE ||
                z >= CHUNK_SIZE);
    }

    private boolean blockFaceCheck(
            Chunk chunk,
            int subChunkIndex,
            int aX, int aY, int aZ,
            Axis axis, Direction3Int direction,
            Type type) {

        int bX = packedCoordinate3Int.addAndWrapAxis(direction.x, aX);
        int bY = packedCoordinate3Int.addAndWrapAxis(direction.y, aY);
        int bZ = packedCoordinate3Int.addAndWrapAxis(direction.z, aZ);

        SubChunk comparativeSubChunk = getComparativeSubChunk(
                chunk,
                subChunkIndex,
                bX, bY, bZ,
                direction);

        int blockID = comparativeSubChunk.getBlock(bX, bY, bZ);
        Type comparativeType = worldSystem.getBlockType(blockID);

        return comparativeType != type;
    }

    private SubChunk getComparativeSubChunk(
            Chunk chunk,
            int subChunkIndex,
            int bX, int bY, int bZ,
            Direction3Int direction) {

        boolean isOverEdge = packedCoordinate3Int.isOverEdge(bX, bY, bZ, direction);

        if (!isOverEdge)
            return chunk.getSubChunk(subChunkIndex);

        if (direction == Direction3Int.UP || direction == Direction3Int.DOWN) {

            int outputSubChunk = subChunkIndex + direction.y;

            if (outputSubChunk > 0 && outputSubChunk < WORLD_HEIGHT)
                return chunk.getSubChunk(outputSubChunk);
            else
                return null;
        }

        else {

            Direction2Int direction2Int = direction.direction2Int;
            Chunk neighborChunk = chunk.getNeighborChunk(direction2Int);
            SubChunk outputSubChunk = neighborChunk.getSubChunk(subChunkIndex);

            return outputSubChunk;
        }
    }

    // Utility \\

    private enum Axis {

        X(Direction3Int.EAST, Direction3Int.WEST, Direction3Int.UP, Direction3Int.NORTH),
        Y(Direction3Int.UP, Direction3Int.DOWN, Direction3Int.NORTH, Direction3Int.EAST),
        Z(Direction3Int.NORTH, Direction3Int.SOUTH, Direction3Int.UP, Direction3Int.EAST);

        private final Direction3Int[] directions;
        private final Direction3Int[] comparativeDirections;

        Axis(Direction3Int pos, Direction3Int neg, Direction3Int a, Direction3Int b) {

            this.directions = new Direction3Int[] { pos, neg };
            this.comparativeDirections = new Direction3Int[] { a, b };
        }

        public Direction3Int getDirection(int directionIndex) {
            return directions[directionIndex];
        }

        public Direction3Int getComparativeDirection(int index) {
            return comparativeDirections[index];
        }

        public static final Axis[] VALUES = { X, Y, Z };
    }
}
