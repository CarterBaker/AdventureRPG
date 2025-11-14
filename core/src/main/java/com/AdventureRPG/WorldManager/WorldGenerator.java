package com.AdventureRPG.WorldManager;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.Util.GlobalConstant;
import com.AdventureRPG.Core.Util.OpenSimplex2;
import com.AdventureRPG.SaveManager.SaveManager;
import com.AdventureRPG.SaveManager.UserData;
import com.AdventureRPG.WorldManager.Blocks.Block;
import com.AdventureRPG.WorldManager.Blocks.BlockSystem;
import com.AdventureRPG.WorldManager.Chunks.Chunk;
import com.AdventureRPG.WorldManager.SubChunks.SubChunk;
import com.AdventureRPG.WorldManager.Util.PackedCoordinate3Int;

public class WorldGenerator extends SystemFrame {

    // Root
    private UserData userData;
    private WorldManager worldManager;

    // Settings
    private int BIOME_SIZE;
    private int CHUNK_SIZE;
    private int WORLD_HEIGHT;

    // World Manager
    private PackedCoordinate3Int packedCoordinate3Int;
    private BlockSystem blockSystem;

    // Default Blocks
    private Block AIR_BLOCK; // TODO: With 2D chunk coordinates this is
    private Block GRASS_BLOCK; // No longer possible

    // Data
    private long seed;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.BIOME_SIZE = GlobalConstant.BIOME_SIZE;
        this.CHUNK_SIZE = GlobalConstant.CHUNK_SIZE;
        this.WORLD_HEIGHT = GlobalConstant.WORLD_HEIGHT;
    }

    @Override
    protected void init() {

        // Root
        this.userData = engineManager.get(SaveManager.class).get(UserData.class);
        this.worldManager = engineManager.get(WorldManager.class);

        // World Manager
        this.packedCoordinate3Int = worldManager.get(PackedCoordinate3Int.class);
        this.blockSystem = worldManager.get(BlockSystem.class);

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

        int biomeSize = packedCoordinate3Int.biomeSize;
        int chunkSize = packedCoordinate3Int.chunkSize;

        // TODO: The first step of world generation will be to get the base biome
        WorldRegion WorldRegion = worldManager.worldReader.worldRegionFromPosition(chunk.coordinate);

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
