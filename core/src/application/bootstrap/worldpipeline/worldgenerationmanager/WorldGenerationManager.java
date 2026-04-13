package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.core.engine.ManagerPackage;
import application.core.settings.EngineSetting;
import application.core.util.OpenSimplex2;
import application.core.util.mathematics.extras.Coordinate2Long;

public class WorldGenerationManager extends ManagerPackage {

    // Internal
    private BlockManager blockManager;
    private BiomeManager biomeManager;

    private int BIOME_SIZE;
    private int CHUNK_SIZE;

    // Data
    private long seed;

    // Blocks
    private short AIR_BLOCK_ID;
    private short GRASS_BLOCK_ID;

    // Biomes
    private short DEFAULT_BIOME_ID;

    // Base \\

    @Override
    protected void create() {
        this.BIOME_SIZE = EngineSetting.BIOME_SIZE;
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        this.seed = 12345L;
    }

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
        this.biomeManager = get(BiomeManager.class);
    }

    @Override
    protected void awake() {
        this.AIR_BLOCK_ID = (short) blockManager.getBlockIDFromBlockName("TerraArcana/Air");
        this.GRASS_BLOCK_ID = (short) blockManager.getBlockIDFromBlockName("TerraArcana/Grass Block");
        this.DEFAULT_BIOME_ID = biomeManager.getBiomeIDFromBiomeName(EngineSetting.DEFAULT_BIOME_NAME);
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

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        long offsetX = (long) chunkX * CHUNK_SIZE;
        long offsetZ = (long) chunkZ * CHUNK_SIZE;
        long offsetY = (long) subChunkInstance.getCoordinate() * CHUNK_SIZE;

        BlockPaletteHandle biomes = subChunkInstance.getBiomePaletteHandle();
        BlockPaletteHandle blocks = subChunkInstance.getBlockPaletteHandle();

        double scale = 0.05;
        double amplitude = 4.0;
        double baseHeight = 12.0;

        // Assign biome to every biome cell in this subchunk
        int biomeAxisSize = CHUNK_SIZE / BIOME_SIZE;
        for (int bx = 0; bx < biomeAxisSize; bx++)
            for (int bz = 0; bz < biomeAxisSize; bz++)
                for (int by = 0; by < biomeAxisSize; by++)
                    biomes.setBlock(bx, by, bz, DEFAULT_BIOME_ID);

        // Generate terrain
        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {

                long worldX = localX + offsetX;
                long worldZ = localZ + offsetZ;

                double noise = OpenSimplex2.noise2(seed, worldX * scale, worldZ * scale);
                int groundHeight = (int) (baseHeight + noise * amplitude);

                for (int localY = 0; localY < CHUNK_SIZE; localY++) {
                    long worldY = localY + offsetY;
                    if (worldY > groundHeight)
                        continue;
                    blocks.setBlock(localX, localY, localZ, GRASS_BLOCK_ID);
                }
            }
        }

        return true;
    }
}