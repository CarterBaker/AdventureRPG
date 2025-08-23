package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.Direction2Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Biomes.Biome;
import com.AdventureRPG.WorldSystem.Biomes.BiomeSystem;
import com.AdventureRPG.WorldSystem.GridSystem.GridSystem;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.utils.IntArray;

public class Chunk {

    // Settings
    public final Settings settings;
    public final int CHUNK_SIZE;
    public final int WORLD_HEIGHT;

    // Chunk
    private final WorldSystem worldSystem;
    private final GridSystem gridSystem;
    private final BiomeSystem biomeSystem;

    public final long coordinate;
    public final int coordinateX, coordinateY;

    // Neighbors
    public final long north, south, east, west;

    // Data
    private boolean hasData;
    private IntArray[] biomes, blocks;

    // Position
    public long position;
    public int positionX, positionY;

    // Mesh
    public final ChunkModel chunkModel;

    // Base \\

    public Chunk(WorldSystem worldSystem, long coordinate) {

        // Settings
        this.settings = worldSystem.settings;
        this.CHUNK_SIZE = worldSystem.settings.CHUNK_SIZE;
        this.WORLD_HEIGHT = worldSystem.settings.WORLD_HEIGHT;

        // Chunk
        this.worldSystem = worldSystem;
        this.gridSystem = worldSystem.gridSystem;
        this.biomeSystem = worldSystem.biomeSystem;

        this.coordinate = coordinate;
        this.coordinateX = Coordinate2Int.unpackX(coordinate);
        this.coordinateY = Coordinate2Int.unpackY(coordinate);

        // Neighbors
        this.north = Coordinate2Int.add(coordinate, Direction2Int.NORTH.packed);
        worldSystem.wrapAroundWorld(north);
        this.south = Coordinate2Int.add(coordinate, Direction2Int.SOUTH.packed);
        worldSystem.wrapAroundWorld(south);
        this.east = Coordinate2Int.add(coordinate, Direction2Int.EAST.packed);
        worldSystem.wrapAroundWorld(east);
        this.west = Coordinate2Int.add(coordinate, Direction2Int.WEST.packed);
        worldSystem.wrapAroundWorld(west);

        // Mesh
        this.chunkModel = new ChunkModel(worldSystem.settings);
    }

    public void dispose() {

        chunkModel.dispose();
    }

    // Data \\

    public boolean hasData() {
        return hasData;
    }

    public void generate(int[][][] biomes, int[][][] blocks) {

        this.hasData = true;
        this.biomes = new IntArray[WORLD_HEIGHT];
        this.blocks = new IntArray[WORLD_HEIGHT];

        for (int subChunk = 0; subChunk < WORLD_HEIGHT; subChunk++) {

            this.biomes[subChunk] = new IntArray();
            this.blocks[subChunk] = new IntArray();

            int subChunkOffset = subChunk * CHUNK_SIZE;

            for (int x = 0; x < CHUNK_SIZE; x++) {

                for (int y = subChunkOffset; y < subChunkOffset + CHUNK_SIZE; y++) {

                    for (int z = 0; z < CHUNK_SIZE; z++) {

                        this.biomes[subChunk].add(biomes[x][y][z]);
                        this.blocks[subChunk].add(blocks[x][y][z]);
                    }
                }
            }
        }
    }

    // Position \\

    public void moveTo(long position) {

        this.position = position;
        this.positionX = Coordinate2Int.unpackX(position);
        this.positionY = Coordinate2Int.unpackY(position);
    }

    // Mesh \\

    public void rebuildModel(Model model) {
        chunkModel.rebuildModel(model);
    }

    public void assignNeighbors(Chunk[] neighbors) {
        chunkModel.assignNeighbors(neighbors);
    }

    public void build() {

        for (int i = 0; i < WORLD_HEIGHT; i++)
            ChunkBuilder.build(this, i);
    }

    public void build(int subChunk) {
        ChunkBuilder.build(this, subChunk);
    }

    public IntArray getBiomes(int subChunk) {
        return biomes[subChunk];
    }

    public IntArray getBlocks(int subChunk) {
        return blocks[subChunk];
    }

    // Gameplay \\

    public void placeBlock(int x, int y, int z, int blockID) {

        int subChunk = worldSystem.getSubChunkFromAxis(y);
        int index = worldSystem.getFlattenedArrayIndex(x, y, z);

        blocks[subChunk].set(index, blockID);

        build(subChunk);

        gridSystem.rebuildModel(position);
    }

    public void breakBlock(int x, int y, int z) {

        int subChunk = worldSystem.getSubChunkFromAxis(y);
        int index = worldSystem.getFlattenedArrayIndex(x, y, z);

        int biomeID = biomes[subChunk].get(index);
        Biome biome = biomeSystem.getBiomeByID(biomeID);

        blocks[subChunk].set(index, biome.airBlock);

        build(subChunk);

        gridSystem.rebuildModel(position);
    }
}
