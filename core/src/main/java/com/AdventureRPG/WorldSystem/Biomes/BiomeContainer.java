package com.AdventureRPG.WorldSystem.Biomes;

import com.AdventureRPG.WorldSystem.Chunks.ChunkPalette;

public final class BiomeContainer {

    private final int dim;
    private final ChunkPalette container;

    public BiomeContainer(int dim) {
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
