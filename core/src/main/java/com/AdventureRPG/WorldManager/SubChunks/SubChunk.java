package com.AdventureRPG.worldmanager.subchunks;

import com.AdventureRPG.core.kernel.EngineSetting;
import com.AdventureRPG.worldmanager.biomes.BiomeContainer;
import com.AdventureRPG.worldmanager.blocks.BlockContainer;
import com.AdventureRPG.worldmanager.chunks.Chunk;

public final class SubChunk {

    // Data
    public final Chunk chunk;
    public final int subChunkIndex;
    private final BiomeContainer biomes;
    private final BlockContainer blocks;

    // Build
    public final SubChunkMesh subChunkMesh;

    // Utility
    private final int biomeShift;

    // Base \\

    public SubChunk(Chunk chunk, int subChunkIndex) {

        // Settings
        this.chunk = chunk;
        this.subChunkIndex = subChunkIndex;
        int CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        int BIOME_SIZE = EngineSetting.BIOME_SIZE;

        // Data
        this.biomes = new BiomeContainer(CHUNK_SIZE / BIOME_SIZE);
        this.blocks = new BlockContainer(CHUNK_SIZE);

        // Build
        this.subChunkMesh = new SubChunkMesh(this, subChunkIndex);

        // Utility
        this.biomeShift = Integer.numberOfTrailingZeros(CHUNK_SIZE / BIOME_SIZE);
    }

    public void dispose() {

    }

    // Data\\

    // Block access
    public short getBlock(int x, int y, int z) {
        return blocks.get(x, y, z);
    }

    public void setBlock(int x, int y, int z, short id) {
        blocks.set(x, y, z, id);
    }

    // Biome access
    public short getBiome(int x, int y, int z) {
        return biomes.get(x >> biomeShift, y >> biomeShift, z >> biomeShift);
    }

    public void setBiome(int x, int y, int z, short id) {
        biomes.set(x, y, z, id);
    }
}
