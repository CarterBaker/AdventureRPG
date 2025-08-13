package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.SaveSystem.UserData;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.Biomes.Biome;
import com.AdventureRPG.WorldSystem.Biomes.BiomeSystem;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Blocks.BlockData;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.Chunks.ChunkSystem;

public class WorldGenerator {

    // Game Manager
    public final Settings settings;
    public final UserData userData;
    public final WorldSystem worldSystem;
    public final ChunkSystem chunkSystem;
    public final BiomeSystem biomeSystem;

    // Settings
    private final int CHUNK_SIZE;
    private final int WORLD_HEIGHT;

    // Default Blocks
    private final Block VACUUM_BLOCK;
    private final Block LAVA_BLOCK;

    // Data
    private long seed;

    // Base \\

    public WorldGenerator(WorldSystem worldSystem) {

        // Game Manager
        this.settings = worldSystem.settings;
        this.userData = worldSystem.saveSystem.userData;
        this.worldSystem = worldSystem;
        this.chunkSystem = worldSystem.chunkSystem;
        this.biomeSystem = worldSystem.biomeSystem;

        // Settings
        this.CHUNK_SIZE = settings.CHUNK_SIZE;
        this.WORLD_HEIGHT = settings.WORLD_HEIGHT;

        // Default Blocks
        this.VACUUM_BLOCK = worldSystem.getBlockByName("vacuum");
        this.LAVA_BLOCK = worldSystem.getBlockByName("lava");

        // Data
        this.seed = userData.getSeed();
    }

    // Data \\

    public long getSeed() {
        return seed;
    }

    public void setSeed(long Seed) {
        this.seed = Seed;
    }

    // Generator \\

    // Chunk \\

    public Chunk generateChunk(long coordinate) {

        int coordinateX = Coordinate2Int.unpackX(coordinate);
        int coordinateY = Coordinate2Int.unpackY(coordinate);

        WorldRegion WorldRegion = worldSystem.worldReader.worldRegionFromPosition(coordinate);
        Chunk chunk = new Chunk(coordinate);

        BlockData[][][] blocks = new BlockData[CHUNK_SIZE][WORLD_HEIGHT][CHUNK_SIZE];

        for (int x = 0; x < CHUNK_SIZE; x++) { // TODO: I should preallocate this array

            for (int y = 0; y < CHUNK_SIZE; y++) { // to eliminate nested loops

                for (int z = 0; z < CHUNK_SIZE; z++) {

                    Vector3Int blockPos = new Vector3Int(x, y, z).add(coordinate);
                    int blockID = generateBlock(WorldRegion, blockPos);

                    blocks[x][y][z] = new BlockData(worldSystem, generateBiome(WorldRegion, blockPos));
                    blocks[x][y][z].PlaceBlock(blockID);
                }
            }
        }

        chunk.generate(blocks);

        return chunk;
    }

    // Biome \\

    private Biome generateBiome(WorldRegion region, Vector3Int position) {
        return biomeSystem.getBiomeByID(0);
    }

    // Block \\

    private int generateBlock(WorldRegion region, Vector3Int position) {

        if (position.y < 5) {
            return LAVA_BLOCK.id;
        } else
            return VACUUM_BLOCK.id;
    }
}
