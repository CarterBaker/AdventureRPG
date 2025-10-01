package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.WorldSystem.Biomes.BiomeContainer;
import com.AdventureRPG.WorldSystem.Blocks.BlockContainer;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.Node;

public final class SubChunk {

    // Data
    public final Chunk chunk;
    public final int subChunkIndex;
    private final BiomeContainer biomes;
    private final BlockContainer blocks;

    // Mesh
    public final SubChunkMesh subChunkMesh;
    private Node node;

    // Utility
    private final int biomeShift;
    private boolean addedToModel;

    // Base \\

    public SubChunk(Chunk chunk, int subChunkIndex) {

        // Settings
        this.chunk = chunk;
        this.subChunkIndex = subChunkIndex;
        int CHUNK_SIZE = chunk.settings.CHUNK_SIZE;
        int BIOME_SIZE = chunk.settings.BIOME_SIZE;

        // Data
        this.biomes = new BiomeContainer(CHUNK_SIZE / BIOME_SIZE);
        this.blocks = new BlockContainer(CHUNK_SIZE);

        // Mesh
        this.subChunkMesh = new SubChunkMesh(chunk.worldSystem);
        this.node = new Node();
        this.node.id = "Chunk_[" + chunk.coordinateX + "_" + chunk.coordinateY + "]_subchunk_[" + subChunkIndex + "]";
        this.node.translation.set(0, subChunkIndex * chunk.settings.CHUNK_SIZE, 0);

        // Utility
        this.biomeShift = Integer.numberOfTrailingZeros(CHUNK_SIZE / BIOME_SIZE);
        this.addedToModel = false;
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

        buildMesh();

        if (!addedToModel) {

            model.nodes.add(node);
            addedToModel = true;
        }
    }

    public void buildMesh() {
        subChunkMesh.build(node);
    }
}
