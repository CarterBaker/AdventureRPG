package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.NoiseUtility;

public class WorldGenerationManager extends ManagerPackage {

    /*
     * Drives per-subchunk terrain generation. Takes no seed or world state of
     * its own — every call is handed the WorldHandle it's generating for, so
     * generation is always keyed off that world's own locked-in seed and PNG
     * map rather than any shared or mutable global state. Biome is resolved
     * once per chunk column through BiomeManager's world-map lookup and used
     * to fill that column's entire biome palette.
     */

    // Internal
    private BlockManager blockManager;
    private BiomeManager biomeManager;

    private int BIOME_SIZE;
    private int CHUNK_SIZE;

    // Blocks
    private short AIR_BLOCK_ID;
    private short GRASS_BLOCK_ID;

    // Base \\

    @Override
    protected void create() {
        this.BIOME_SIZE = EngineSetting.BIOME_SIZE;
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
        this.biomeManager = get(BiomeManager.class);
    }

    @Override
    protected void awake() {
        this.AIR_BLOCK_ID = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
        this.GRASS_BLOCK_ID = (short) blockManager.getBlockIDFromBlockName("TerraArcanaBlocks/Grass Block");
    }

    // Generator \\

    /*
     * Terrain is written straight through the block palette rather than
     * SubChunkInstance.setBlock() — a freshly generated subchunk holds no
     * liquid yet, so world generation has no reason to pay per-block liquid-
     * stability invalidation on every placed voxel.
     */
    public boolean generateSubChunk(WorldHandle worldHandle, long chunkCoordinate, SubChunkInstance subChunkInstance) {

        long seed = worldHandle.getSeed();

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        long offsetX = (long) chunkX * CHUNK_SIZE;
        long offsetZ = (long) chunkZ * CHUNK_SIZE;
        long offsetY = (long) subChunkInstance.getCoordinate() * CHUNK_SIZE;

        short biomeID = biomeManager.getBiomeIDFromChunkCoordinate(worldHandle, chunkCoordinate);

        BlockPaletteHandle biomes = subChunkInstance.getBiomePaletteHandle();
        BlockPaletteHandle blocks = subChunkInstance.getBlockPaletteHandle();

        double scale = 0.05;
        double amplitude = 4.0;
        double baseHeight = 12.0;

        // Assign the resolved biome to every biome cell in this subchunk
        int biomeAxisSize = CHUNK_SIZE / BIOME_SIZE;
        for (int bx = 0; bx < biomeAxisSize; bx++)
            for (int bz = 0; bz < biomeAxisSize; bz++)
                for (int by = 0; by < biomeAxisSize; by++)
                    biomes.setBlock(bx, by, bz, biomeID);

        // Generate terrain
        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {

                long worldX = localX + offsetX;
                long worldZ = localZ + offsetZ;

                double noise = NoiseUtility.noise2(seed, worldX * scale, worldZ * scale);
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