package com.AdventureRPG.WorldSystem.Chunks;

import java.util.List;

import com.AdventureRPG.SettingsSystem.Settings;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.utils.IntArray;

public class ChunkModel {

    // Data
    int[] biomes, blocks;

    // Neighbors
    private Chunk north, south, east, west;
    public int[] northBiomes, southBiomes, eastBiomes, westBiomes;
    public int[] northBlocks, southBlocks, eastBlocks, westBlocks;

    // Mesh
    private List<MeshPart> meshPart;
    private List<Mesh> mesh;
    private List<Material> material;

    // Qauds
    private volatile IntArray[] quadData;

    // Base \\

    public ChunkModel(Settings settings) {

        // Qauds
        this.quadData = new IntArray[settings.WORLD_HEIGHT];
    }

    public void dispose() {

    }

    // Data \\

    public int getBiome(int xyz) {
        return biomes[xyz];
    }

    public int getBlock(int xyz) {
        return blocks[xyz];
    }

    // Neighbors \\

    public void assignData(int[] biomes, int[] blocks) {
        this.biomes = biomes;
        this.blocks = blocks;
    }

    public void assignNeighbors(Chunk[] neighbors) {

        north = neighbors[0];
        south = neighbors[1];
        east = neighbors[2];
        west = neighbors[3];

        northBiomes = north.getBiomes();
        southBiomes = south.getBiomes();
        eastBiomes = east.getBiomes();
        westBiomes = west.getBiomes();

        northBlocks = north.getBlocks();
        southBlocks = south.getBlocks();
        eastBlocks = east.getBlocks();
        westBlocks = west.getBlocks();
    }

    Chunk getNorthNeighbor() {
        return north;
    }

    Chunk getSouthNeighbor() {
        return south;
    }

    Chunk getEastNeighbor() {
        return east;
    }

    Chunk getWestNeighbor() {
        return west;
    }

    // Mesh \\

    void setQuadData(int subChunk, IntArray data) {
        this.quadData[subChunk] = data;
    }

    public void rebuildModel(Model model) {

        clearModel(model);
    }

    private void clearModel(Model model) {

        model.meshes.clear();
        model.meshParts.clear();
        model.materials.clear();
    }
}
