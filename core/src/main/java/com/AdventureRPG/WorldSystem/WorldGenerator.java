package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.SaveSystem.UserData;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.SubChunks.SubChunk;
import com.AdventureRPG.WorldSystem.Util.PackedCoordinate3Int;
import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.Util.OpenSimplex2;

public class WorldGenerator {

    // Game Manager
    private final UserData userData;
    private final WorldSystem worldSystem;
    private final PackedCoordinate3Int packedCoordinate3Int;

    // Settings
    private final int BIOME_SIZE;
    private final int CHUNK_SIZE;
    private final int WORLD_HEIGHT;

    // Default Blocks
    private final Block AIR_BLOCK; // TODO: With 2D chunk coordinates this is
    private final Block GRASS_BLOCK; // No longer possible

    // Data
    private long seed;

    // Base \\

    public WorldGenerator(WorldSystem worldSystem) {

        // Game Manager
        this.userData = worldSystem.saveSystem.userData;
        this.worldSystem = worldSystem;
        this.packedCoordinate3Int = worldSystem.packedCoordinate3Int;

        // Settings
        this.BIOME_SIZE = GlobalConstant.BIOME_SIZE;
        this.CHUNK_SIZE = GlobalConstant.CHUNK_SIZE;
        this.WORLD_HEIGHT = GlobalConstant.WORLD_HEIGHT;

        // Default Blocks
        this.AIR_BLOCK = worldSystem.getBlockByName("Air");
        this.GRASS_BLOCK = worldSystem.getBlockByName("Grass Block");

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

        int biomeSize = packedCoordinate3Int.biomeSize;
        int chunkSize = packedCoordinate3Int.chunkSize;

        // TODO: The first step of world generation will be to get the base biome
        WorldRegion WorldRegion = worldSystem.worldReader.worldRegionFromPosition(chunk.coordinate);

        // TODO: I want to blend the base biome around pixels

        // TODO: I want to get all the related biomes to base and mix them

        SubChunk[] subChunks = new SubChunk[WORLD_HEIGHT];

        for (int subChunkIndex = 0; subChunkIndex < WORLD_HEIGHT; subChunkIndex++) {

            long offsetY = subChunkIndex * CHUNK_SIZE;

            SubChunk subChunk = subChunks[subChunkIndex] = new SubChunk(chunk, subChunkIndex);

            for (int index = 0; index < biomeSize; index++) {

                int xyz = packedCoordinate3Int.getPackedBiomeCoordinates(index);

                int x = packedCoordinate3Int.unpackX(xyz);
                int y = packedCoordinate3Int.unpackY(xyz);
                int z = packedCoordinate3Int.unpackZ(xyz);

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

                int xyz = packedCoordinate3Int.getPackedBlockCoordinate(index);

                int localX = packedCoordinate3Int.unpackX(xyz);
                int localY = packedCoordinate3Int.unpackY(xyz);
                int localZ = packedCoordinate3Int.unpackZ(xyz);

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
