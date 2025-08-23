package com.AdventureRPG.WorldSystem.Chunks;

import com.badlogic.gdx.utils.IntArray;

public final class ChunkBuilder {

    public static void build(Chunk chunk, int subChunk) {

        final IntArray biomes = chunk.getBiomes(subChunk);
        final IntArray blocks = chunk.getBlocks(subChunk);

        if (blocks == null)
            return;

        final int CHUNK_SIZE = chunk.CHUNK_SIZE;

        final ChunkModel model = chunk.chunkModel;
        final Chunk northNeighbor = model.getNorthNeighbor();
        final Chunk southNeighbor = model.getSouthNeighbor();
        final Chunk eastNeighbor = model.getEastNeighbor();
        final Chunk westNeighbor = model.getWestNeighbor();

        IntArray quads = new IntArray(chunk.settings.CHUNK_VERT_BUFFER);

        int[] blockMaskX = new int[CHUNK_SIZE];
        boolean[] drawMaskX = new boolean[CHUNK_SIZE];

        int[] blockMaskY = new int[CHUNK_SIZE];
        boolean[] drawMaskY = new boolean[CHUNK_SIZE];

        int[] blockMaskZ = new int[CHUNK_SIZE];
        boolean[] drawMaskZ = new boolean[CHUNK_SIZE];

        buildAxis(subChunk, 0, blockMaskX, drawMaskX, northNeighbor, southNeighbor, eastNeighbor, westNeighbor, quads);
        buildAxis(subChunk, 1, blockMaskY, drawMaskY, northNeighbor, southNeighbor, eastNeighbor, westNeighbor, quads);
        buildAxis(subChunk, 2, blockMaskZ, drawMaskZ, northNeighbor, southNeighbor, eastNeighbor, westNeighbor, quads);
    }

    private static void buildAxis(
            int subChunk,
            int axis,
            int[] blockMask,
            boolean[] drawMask,
            Chunk northNeighbor,
            Chunk southNeighbor,
            Chunk eastNeighbor,
            Chunk westNeighbor,
            IntArray qauds) {

        final int U = (axis == 0) ? 1 : 0;
        final int V = (axis == 2) ? 1 : 2;

        final IntArray northBlocks = northNeighbor.getBlocks(subChunk);
        final IntArray southBlocks = southNeighbor.getBlocks(subChunk);
        final IntArray eastBlocks = eastNeighbor.getBlocks(subChunk);
        final IntArray westBlocks = westNeighbor.getBlocks(subChunk);
    }
}
