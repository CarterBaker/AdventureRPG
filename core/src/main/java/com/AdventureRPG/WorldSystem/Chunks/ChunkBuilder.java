package com.AdventureRPG.WorldSystem.Chunks;

import java.util.BitSet;

import com.AdventureRPG.Util.Direction3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Blocks.Type;
import com.badlogic.gdx.utils.IntArray;

public final class ChunkBuilder {

    public static void build(Chunk chunk, int subChunk) {

        // Game Manager
        final WorldSystem worldSystem = chunk.worldSystem;
        final ChunkCoordinates chunkCoordinates = chunk.chunkCoordinates;
        final ChunkModel model = chunk.chunkModel;

        // Settings
        final int CHUNK_SIZE = chunk.CHUNK_SIZE;

        // Data
        IntArray quads = new IntArray(chunk.settings.CHUNK_VERT_BUFFER);
        BitSet negativeBlocks = new BitSet(chunkCoordinates.subChunkSize);
        BitSet positiveBlocks = new BitSet(chunkCoordinates.subChunkSize);

        // Base \\

        for (Axis axis : Axis.VALUES) {

            for (int index = 0; index < chunkCoordinates.subChunkSize; index++) {

                int xyz = chunkCoordinates.getSubCoordinates(index, subChunk);
                int blockID = model.getBlock(xyz);
                Type type = worldSystem.getBlockType(blockID);

                if (type == Type.NULL)
                    continue;

                int biomeID = model.getBiome(xyz);

                int x = chunkCoordinates.getX(xyz);
                int y = chunkCoordinates.getY(xyz);
                int z = chunkCoordinates.getZ(xyz);

                // Negative Pass

                for (Direction3Int direction : axis.getDirections()) {

                    int neighborXYZ = chunkCoordinates.addAndWrap(x, y, z, direction);
                    Chunk neighborChunk = neighborChunk(direction, chunk, x, y, z);

                    if (neighborChunk == null) {

                        emitQuad(quads, x, y, z, direction, blockID, biomeID);
                        continue;
                    }

                    int neighborBlockID = neighborChunk.chunkModel.getBlock(neighborXYZ);
                    Type neighborType = worldSystem.getBlockType(neighborBlockID);

                    if (neighborType != type)
                        emitQuad(quads, x, y, z, direction, blockID, biomeID);
                }

                // Clear the cache before moving to the next axis
                negativeBlocks.clear();
                positiveBlocks.clear();
            }
        }
    }

    private static void emitQuad(IntArray quads, int x, int y, int z,
            Direction3Int dir, int blockID, int biomeID) {

        int texID = blockID;
        int matID = 0;

        int c0 = 0xFFFFFFFF;
        int c1 = 0xFFFFFFFF;
        int c2 = 0xFFFFFFFF;
        int c3 = 0xFFFFFFFF;

        // Write quad data (12 ints)
        quads.add(x);
        quads.add(y);
        quads.add(z);
        quads.add(1);
        quads.add(1);
        quads.add(dir.ordinal());
        quads.add(texID);
        quads.add(matID);
        quads.add(c0);
        quads.add(c1);
        quads.add(c2);
        quads.add(c3);
    }

    private static Chunk neighborChunk(Direction3Int direction, Chunk chunk, int x, int y, int z) {

        boolean atEdge = chunk.chunkCoordinates.isAtEdge(x, y, z, direction);
        if (!atEdge)
            return chunk;

        switch (direction) {
            case UP:
                return null;
            case DOWN:
                return null;
            case NORTH:
                return chunk.chunkModel.getNorthNeighbor();
            case SOUTH:
                return chunk.chunkModel.getSouthNeighbor();
            case EAST:
                return chunk.chunkModel.getEastNeighbor();
            case WEST:
                return chunk.chunkModel.getWestNeighbor();
            default:
                return chunk;
        }
    }

    // Utility \\

    private static enum Axis {
        X(0, Direction3Int.EAST, Direction3Int.WEST),
        Y(1, Direction3Int.UP, Direction3Int.DOWN),
        Z(2, Direction3Int.NORTH, Direction3Int.SOUTH);

        private final Direction3Int[] directions;

        Axis(int index, Direction3Int pos, Direction3Int neg) {
            this.directions = new Direction3Int[] { pos, neg };
        }

        public Direction3Int[] getDirections() {
            return directions;
        }

        public static final Axis[] VALUES = { X, Y, Z };
    }
}
