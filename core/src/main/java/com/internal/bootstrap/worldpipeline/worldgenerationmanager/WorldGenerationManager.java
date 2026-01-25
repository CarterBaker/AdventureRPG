package com.internal.bootstrap.worldpipeline.worldgenerationmanager;

import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.block.BlockPaletteHandle;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.OpenSimplex2;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

public class WorldGenerationManager extends ManagerPackage {

    // Internal
    private BlockManager blockManager;
    private int BIOME_SIZE;
    private int CHUNK_SIZE;

    // Data
    private long seed;

    // Blocks
    private short AIR_BLOCK_ID;
    private short GRASS_BLOCK_ID;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.BIOME_SIZE = EngineSetting.BIOME_SIZE;
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;

        // Default seed (can be set later)
        this.seed = 12345L;
    }

    @Override
    protected void get() {

        // Internal
        this.blockManager = get(BlockManager.class);
    }

    @Override
    protected void awake() {

        // Blocks
        this.AIR_BLOCK_ID = (short) blockManager.getBlockIDFromBlockName("Air");
        this.GRASS_BLOCK_ID = (short) blockManager.getBlockIDFromBlockName("Grass Block");
    }

    // Data \\

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public long getSeed() {
        return seed;
    }

    // Generator \\

    public boolean generateSubChunk(long chunkCoordinate, SubChunkInstance subChunkInstance) {

        // Unpack chunk coordinates
        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        // Calculate world offsets
        long offsetX = (long) chunkX * CHUNK_SIZE;
        long offsetZ = (long) chunkZ * CHUNK_SIZE;
        long offsetY = (long) subChunkInstance.getCoordinate() * CHUNK_SIZE;

        // Get block palette
        BlockPaletteHandle biomes = subChunkInstance.getBiomePaletteHandle();
        BlockPaletteHandle blocks = subChunkInstance.getBlockPaletteHandle();

        // Noise parameters
        double scale = 0.05; // Lower = bigger hills
        double amplitude = 4.0; // Height variation (±4 blocks)
        double baseHeight = 12.0; // Average terrain height (around y=12)

        // Initialize all biomes to 0
        int biomeSize = CHUNK_SIZE / BIOME_SIZE;
        for (int bx = 0; bx < biomeSize; bx++)
            for (int bz = 0; bz < biomeSize; bz++)
                for (int by = 0; by < biomeSize; by++)
                    biomes.setBlock(bx, by, bz, (short) 0);

        // Generate each block in the subchunk
        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {

                // World coordinates
                long worldX = localX + offsetX;
                long worldZ = localZ + offsetZ;

                // Get terrain height using 2D noise
                double noise = OpenSimplex2.noise2(seed, worldX * scale, worldZ * scale);
                int groundHeight = (int) (baseHeight + noise * amplitude);

                // Generate vertical column
                for (int localY = 0; localY < CHUNK_SIZE; localY++) {
                    long worldY = localY + offsetY;

                    short blockID;

                    if (worldY > groundHeight)
                        continue;

                    blockID = GRASS_BLOCK_ID;
                    blocks.setBlock(localX, localY, localZ, blockID);
                }
            }
        }

        return true;
    }
}