package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.SaveSystem.UserData;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Coordinate3Int;
import com.AdventureRPG.WorldSystem.Biomes.BiomeSystem;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.Chunks.ChunkCoordinates;
import com.AdventureRPG.WorldSystem.GridSystem.GridSystem;

public class WorldGenerator {

    // Game Manager
    public final Settings settings;
    public final UserData userData;
    public final WorldSystem worldSystem;
    public final GridSystem gridSystem;
    public final BiomeSystem biomeSystem;

    // Settings
    private final int CHUNK_SIZE;
    private final int WORLD_HEIGHT;

    // Block Management
    private final ChunkCoordinates chunkCoordinates;

    // Default Blocks
    private final Block AIR_BLOCK; // TODO: With 2D chunk coordinates this is
    private final Block LAVA_BLOCK; // No longer possible

    // Data
    private long seed;

    // Base \\

    public WorldGenerator(WorldSystem worldSystem) {

        // Game Manager
        this.settings = worldSystem.settings;
        this.userData = worldSystem.saveSystem.userData;
        this.worldSystem = worldSystem;
        this.gridSystem = worldSystem.gridSystem;
        this.biomeSystem = worldSystem.biomeSystem;

        // Settings
        this.CHUNK_SIZE = settings.CHUNK_SIZE;
        this.WORLD_HEIGHT = settings.WORLD_HEIGHT;

        // Block Management
        this.chunkCoordinates = worldSystem.chunkCoordinates;

        // Default Blocks
        this.AIR_BLOCK = worldSystem.getBlockByName("air");
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

    public void generateChunk(Chunk chunk) {

        long chunkCoordinate = Coordinate3Int.pack(chunk.coordinateX, 0, chunk.coordinateY);

        int chunkSize = chunkCoordinates.chunkSize;
        int[] biomes = new int[chunkSize];
        int[] blocks = new int[chunkSize];

        // TODO: The first step of world generation will be to get the base biome
        WorldRegion WorldRegion = worldSystem.worldReader.worldRegionFromPosition(chunk.coordinate);

        // TODO: I want to blend the base biome around pixels

        // TODO: I want to get all the related biomes to base and mix them

        for (int index = 0; index < chunkSize; index++) {

            int xyz = chunkCoordinates.getCoordinates(index);
            long blockOffset = chunkCoordinates.convertToCoordinate3Int(xyz);
            long blockCoordinate = Coordinate3Int.add(chunkCoordinate, blockOffset);

            int biomeID = generateBiome(blockCoordinate);
            int blockID = generateBlock(blockCoordinate);

            biomes[xyz] = biomeID;
            blocks[xyz] = blockID;

            chunk.generate(biomes, blocks);
        }
    }

    // Biome \\

    // TODO: This will need a biome
    private int generateBiome(long blockCoordinate) {
        return 0;
    }

    // Block \\

    // TODO: This will need a biome
    private int generateBlock(long blockCoordinate) {

        int y = Coordinate3Int.unpackY(blockCoordinate);

        if (y < 5) {
            return LAVA_BLOCK.id;
        } else
            return AIR_BLOCK.id;
    }
}
