package com.AdventureRPG.WorldPipeline.blocks;

import com.AdventureRPG.WorldPipeline.chunks.ChunkPalette;

public final class BlockContainer {

    private final int dim;
    private final ChunkPalette container;

    public BlockContainer(int dim) {
        this.dim = dim;
        this.container = new ChunkPalette(dim * dim * dim, 512);
    }

    public short get(int x, int y, int z) {
        return container.get(x, y, z, dim);
    }

    public void set(int x, int y, int z, short id) {
        container.set(x, y, z, id, dim);
    }
}
