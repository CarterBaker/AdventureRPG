package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.SaveSystem.UserData;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Coordinate3Int;
import com.AdventureRPG.WorldSystem.Biomes.BiomeSystem;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.Chunks.SubChunk;
import com.AdventureRPG.WorldSystem.GridSystem.GridSystem;

public class WorldGenerator {

    // Game Manager
    private final Settings settings;
    private final UserData userData;
    private final WorldSystem worldSystem;
    private final GridSystem gridSystem;
    private final BiomeSystem biomeSystem;
    private final PackedCoordinate3Int packedCoordinate3Int;

    // Settings
    private final int BIOME_SIZE;
    private final int CHUNK_SIZE;
    private final int WORLD_HEIGHT;

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
        this.packedCoordinate3Int = worldSystem.packedCoordinate3Int;

        // Settings
        this.BIOME_SIZE = settings.BIOME_SIZE;
        this.CHUNK_SIZE = settings.CHUNK_SIZE;
        this.WORLD_HEIGHT = settings.WORLD_HEIGHT;

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

        int offsetX = chunk.coordinateX * CHUNK_SIZE;
        int offsetZ = chunk.coordinateY * CHUNK_SIZE;

        int biomeSize = packedCoordinate3Int.biomeSize;
        int chunkSize = packedCoordinate3Int.chunkSize;

        // TODO: The first step of world generation will be to get the base biome
        WorldRegion WorldRegion = worldSystem.worldReader.worldRegionFromPosition(chunk.coordinate);

        // TODO: I want to blend the base biome around pixels

        // TODO: I want to get all the related biomes to base and mix them

        SubChunk[] subChunks = new SubChunk[WORLD_HEIGHT];

        for (int subChunkIndex = 0; subChunkIndex < WORLD_HEIGHT; subChunkIndex++) {

            SubChunk subChunk = subChunks[subChunkIndex] = new SubChunk(chunk, subChunkIndex);
            int offsetY = subChunkIndex * CHUNK_SIZE;

            for (int index = 0; index < biomeSize; index++) {

                int xyz = packedCoordinate3Int.getPackedBiomeCoordinates(index);

                int x = packedCoordinate3Int.unpackX(xyz);
                int y = packedCoordinate3Int.unpackY(xyz);
                int z = packedCoordinate3Int.unpackZ(xyz);

                int biomeX = x * (CHUNK_SIZE / BIOME_SIZE);
                int biomeY = y * (CHUNK_SIZE / BIOME_SIZE);
                int biomeZ = z * (CHUNK_SIZE / BIOME_SIZE);

                biomeX += offsetX;
                biomeY += offsetY;
                biomeZ += offsetZ;

                long biomeCoordinate = Coordinate3Int.pack(biomeX, biomeY, biomeZ);

                short biomeID = (short) generateBiome(biomeCoordinate);

                subChunk.setBiome(x, y, z, biomeID);
            }

            for (int index = 0; index < chunkSize; index++) {

                int xyz = packedCoordinate3Int.getPackedBlockCoordinate(index);

                int x = packedCoordinate3Int.unpackX(xyz);
                int y = packedCoordinate3Int.unpackY(xyz);
                int z = packedCoordinate3Int.unpackZ(xyz);

                x += offsetX;
                y += offsetY;
                z += offsetZ;

                long blockCoordinate = Coordinate3Int.pack(x, y, z);

                short blockID = (short) generateBlock(blockCoordinate, subChunkIndex);

                subChunk.setBlock(x, y, z, blockID);
            }
        }

        chunk.generate(subChunks);
    }

    // Biome \\

    // TODO: This will need a biome
    private int generateBiome(long biomeCoordinate) {
        return 0;
    }

    // Block \\

    // TODO: This will need a biome
    private int generateBlock(long blockCoordinate, int subChunkIndex) {

        int x = Coordinate3Int.unpackX(blockCoordinate);
        int y = Coordinate3Int.unpackY(blockCoordinate);
        int z = Coordinate3Int.unpackZ(blockCoordinate);

        if (y < 5)
            return LAVA_BLOCK.id;

        else
            return AIR_BLOCK.id;
    }
}
