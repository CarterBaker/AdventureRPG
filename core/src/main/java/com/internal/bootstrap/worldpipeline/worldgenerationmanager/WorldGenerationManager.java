package com.internal.bootstrap.worldpipeline.worldgenerationmanager;

import com.internal.bootstrap.worldpipeline.subchunk.BlockPaletteHandle;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.OpenSimplex2;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

public class WorldGenerationManager extends ManagerPackage {

    // Settings
    private int CHUNK_SIZE;

    // Data
    private long seed;

    // Block IDs (temporary - should come from BlockSystem later)
    private short AIR_BLOCK_ID = 0;
    private short GRASS_BLOCK_ID = 1;

    // Base \\

    @Override
    protected void create() {
        // Settings
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;

        // Default seed (can be set later)
        this.seed = 12345L;
    }

    // Data \\

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public long getSeed() {
        return seed;
    }

    // Generator \\

    /**
     * Generates blocks for a subchunk using simple terrain noise.
     * 
     * @param chunkCoordinate  2D chunk coordinate (packed long)
     * @param subChunkInstance The subchunk instance to fill with blocks
     * @param subChunkIndex    SubChunk Y index (0 to WORLD_HEIGHT-1)
     * @return true if generation was successful
     */
    public boolean generateSubChunk(long chunkCoordinate, SubChunkInstance subChunkInstance, int subChunkIndex) {

        // Unpack chunk coordinates
        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        // Calculate world offsets
        long offsetX = (long) chunkX * CHUNK_SIZE;
        long offsetZ = (long) chunkZ * CHUNK_SIZE;
        long offsetY = (long) subChunkIndex * CHUNK_SIZE;

        // Get block palette
        BlockPaletteHandle blocks = subChunkInstance.getBlockPaletteHandle();

        // Noise parameters
        double scale = 0.05; // Lower = bigger hills
        double amplitude = 4.0; // Height variation (±4 blocks)
        double baseHeight = 12.0; // Average terrain height (around y=12)

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
                    if (worldY <= groundHeight) {
                        blockID = GRASS_BLOCK_ID;
                    } else {
                        blockID = AIR_BLOCK_ID;
                    }

                    blocks.setBlock(localX, localY, localZ, blockID);
                }
            }
        }

        return true;
    }
}