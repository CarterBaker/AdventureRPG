package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.WorldSystem.Biomes.BiomeContainer;
import com.AdventureRPG.WorldSystem.Blocks.BlockContainer;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.Node;

public final class SubChunk {

    // Data
    private final BiomeContainer biomes;
    private final BlockContainer blocks;

    // Mesh
    public final SubChunkMesh subChunkMesh;
    private Node node;

    // Utility
    private final int biomeShift;

    // Base \\

    public SubChunk(Chunk chunk) {

        // Settings
        int CHUNK_SIZE = chunk.settings.CHUNK_SIZE;
        int BIOME_SIZE = chunk.settings.BIOME_SIZE;

        // Data
        this.biomes = new BiomeContainer(CHUNK_SIZE / BIOME_SIZE);
        this.blocks = new BlockContainer(CHUNK_SIZE);

        // Mesh
        this.subChunkMesh = new SubChunkMesh(chunk.worldSystem);
        this.node = new Node();

        // Utility
        this.biomeShift = Integer.numberOfTrailingZeros(CHUNK_SIZE / BIOME_SIZE);
    }

    public void dispose() {
        subChunkMesh.dispose();
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

    // Mesh \\

    public void build(Model model) {

        rebuild();
        model.nodes.add(node);
    }

    public void rebuild() {
        subChunkMesh.build(node);
    }
}
