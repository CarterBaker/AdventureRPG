package com.AdventureRPG.WorldPipeline;

import com.AdventureRPG.WorldPipeline.blocks.Block;
import com.AdventureRPG.WorldPipeline.blocks.BlockManager;
import com.AdventureRPG.WorldPipeline.chunks.Chunk;
import com.AdventureRPG.WorldPipeline.subchunks.SubChunk;
import com.AdventureRPG.WorldPipeline.util.SubChunkCoordinateUtility;
import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;
import com.AdventureRPG.core.util.OpenSimplex2;
import com.AdventureRPG.savemanager.SaveManager;
import com.AdventureRPG.savemanager.UserData;

public class WorldGenerator extends SystemPackage {

    // Root
    private UserData userData;
    private WorldPipeline worldPipeline;

    // Settings
    private int BIOME_SIZE;
    private int CHUNK_SIZE;
    private int WORLD_HEIGHT;

    // World Manager
    private BlockManager blockSystem;

    // Default Blocks
    private Block AIR_BLOCK; // TODO: With 2D chunk coordinates this is
    private Block GRASS_BLOCK; // No longer possible

    // Data
    private long seed;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.BIOME_SIZE = EngineSetting.BIOME_SIZE;
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        this.WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT;
    }

    @Override
    protected void get() {

        // Root
        this.userData = get(UserData.class);
        this.worldPipeline = get(WorldPipeline.class);

        // World Manager
        this.blockSystem = get(BlockManager.class);

        // Default Blocks
        this.AIR_BLOCK = blockSystem.getBlockByName("Air");
        this.GRASS_BLOCK = blockSystem.getBlockByName("Grass Block");

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

        long offsetX = chunk.coordinateX * CHUNK_SIZE;
        long offsetZ = chunk.coordinateY * CHUNK_SIZE;

        int biomeSize = SubChunkCoordinateUtility.BIOME_BOCK_COUNT;
        int chunkSize = SubChunkCoordinateUtility.CHUNK_BLOCK_COUNT;

        // TODO: The first step of world generation will be to get the base biome
        WorldRegion WorldRegion = worldPipeline.worldReader.worldRegionFromPosition(chunk.coordinate);

        // TODO: I want to blend the base biome around pixels

        // TODO: I want to get all the related biomes to base and mix them

        SubChunk[] subChunks = new SubChunk[WORLD_HEIGHT];

        for (int subChunkIndex = 0; subChunkIndex < WORLD_HEIGHT; subChunkIndex++) {

            long offsetY = subChunkIndex * CHUNK_SIZE;

            SubChunk subChunk = subChunks[subChunkIndex] = new SubChunk(chunk, subChunkIndex);

            for (int index = 0; index < biomeSize; index++) {

                int xyz = SubChunkCoordinateUtility.getPackedBiomeCoordinate(index);

                int x = SubChunkCoordinateUtility.unpackX(xyz);
                int y = SubChunkCoordinateUtility.unpackY(xyz);
                int z = SubChunkCoordinateUtility.unpackZ(xyz);

                int convertedX = x * (CHUNK_SIZE / BIOME_SIZE);
                int convertedY = y * (CHUNK_SIZE / BIOME_SIZE);
                int convertedZ = z * (CHUNK_SIZE / BIOME_SIZE);

                long worldX = convertedX + offsetX;
                long worldY = convertedY + offsetY;
                long worldZ = convertedZ + offsetZ;

                short biomeID = (short) generateBiome(worldX, worldY, worldZ);

                subChunk.setBiome(x, y, z, biomeID);
            }

            for (int index = 0; index < chunkSize; index++) {

                int xyz = SubChunkCoordinateUtility.getPackedBlockCoordinate(index);

                int localX = SubChunkCoordinateUtility.unpackX(xyz);
                int localY = SubChunkCoordinateUtility.unpackY(xyz);
                int localZ = SubChunkCoordinateUtility.unpackZ(xyz);

                long worldX = localX + offsetX;
                long worldY = localY + offsetY;
                long worldZ = localZ + offsetZ;

                short blockID = (short) generateBlock(worldX, worldY, worldZ);

                subChunk.setBlock(localX, localY, localZ, blockID);
            }
        }

        chunk.generate(subChunks);
    }

    // Biome \\

    // TODO: This will need a biome
    private int generateBiome(long x, long y, long z) {
        return 0;
    }

    // Block \\

    // TODO: This will need a biome
    private int generateBlock(long x, long y, long z) {
        // Noise setup
        double scale = 0.05; // lower = bigger hills
        double amplitude = 8.0; // height variation
        double baseHeight = 8.0; // average terrain height
        // TODO: This is a bug if this excceeds the size of one chunk it does not work.

        // Get 2D noise based on X/Z
        double n = OpenSimplex2.noise2(this.seed, x * scale, z * scale);
        int groundHeight = (int) (baseHeight + n * amplitude);

        // Clamp to 0–16 for grass test
        groundHeight = Math.max(0, Math.min(16, groundHeight));

        // Block decision
        if (y <= groundHeight) {
            return GRASS_BLOCK.id;
        } else {
            return AIR_BLOCK.id;
        }
    }
}
