package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.TerrainShapeUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class WorldGenerationManager extends ManagerPackage {

    /*
     * Drives per-chunk-column terrain generation. computeColumn() resolves the
     * one biome that governs an entire chunk column (matching the same pixel
     * granularity the biome palette itself is written at) plus a ground-height
     * value for every one of its 256 block-columns, all cached in a per-thread
     * scratch buffer; generateSubChunk() is then called once per subchunk
     * (WORLD_HEIGHT times) purely to carve blocks out of that already-resolved
     * shape, so the expensive multi-octave height noise is only ever evaluated
     * once per chunk rather than once per subchunk. Height itself comes from
     * TerrainShapeUtility's continentalness/erosion/peaks-valleys noise stack,
     * fully independent of biome — biome only ever chooses which blocks dress
     * that shape (surface, subsurface, and underwater/beach), so painting a
     * new biome onto the world PNG can never introduce a height seam.
     */

    // Internal
    private BlockManager blockManager;
    private BiomeManager biomeManager;

    private TerrainColumnAsyncContainer terrainColumnContainer;
    private final Short2ObjectOpenHashMap<TerrainSurfaceProfile> surfaceProfileCache = new Short2ObjectOpenHashMap<>();

    private int CHUNK_SIZE;

    // Blocks
    private short airBlockId;
    private short stoneBlockId;
    private short waterBlockId;

    // Base \\

    @Override
    protected void create() {
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        this.terrainColumnContainer = create(TerrainColumnAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
        this.biomeManager = get(BiomeManager.class);
    }

    @Override
    protected void awake() {
        this.airBlockId = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
        this.stoneBlockId = (short) blockManager.getBlockIDFromBlockName(EngineSetting.DEFAULT_STONE_BLOCK_NAME);
        this.waterBlockId = (short) blockManager.getBlockIDFromBlockName(EngineSetting.DEFAULT_WATER_BLOCK_NAME);
    }

    // Column — once per chunk \\

    /*
     * Resolves this chunk column's biome and every one of its 256 ground
     * heights into the calling thread's scratch buffer. Must be called
     * exactly once, before the first generateSubChunk() call for this same
     * chunk coordinate.
     */
    public void computeColumn(WorldHandle worldHandle, long chunkCoordinate) {

        TerrainColumnAsyncContainer column = terrainColumnContainer.getInstance();

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        long seed = worldHandle.getSeed();
        long worldOffsetX = (long) chunkX * CHUNK_SIZE;
        long worldOffsetZ = (long) chunkZ * CHUNK_SIZE;

        double worldWidthBlocks = worldHandle.getWorldScale().x;
        double worldHeightBlocks = worldHandle.getWorldScale().y;

        BiomeHandle biomeHandle = biomeManager.getBiome(worldHandle, chunkCoordinate);
        TerrainSurfaceProfile profile = resolveSurfaceProfile(biomeHandle);

        column.biomeID = biomeHandle.getBiomeID();
        column.surfaceBlockID = profile.surfaceBlockID;
        column.subsurfaceBlockID = profile.subsurfaceBlockID;
        column.underwaterBlockID = profile.underwaterBlockID;

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {

                double worldX = worldOffsetX + localX;
                double worldZ = worldOffsetZ + localZ;

                column.groundHeightBlocks[localZ * CHUNK_SIZE + localX] = TerrainShapeUtility.computeGroundHeightBlocks(
                        seed, worldX, worldZ, worldWidthBlocks, worldHeightBlocks);
            }
        }

        column.computedChunkCoordinate = chunkCoordinate;
        column.hasComputedColumn = true;
    }

    private synchronized TerrainSurfaceProfile resolveSurfaceProfile(BiomeHandle biomeHandle) {

        short biomeID = biomeHandle.getBiomeID();
        TerrainSurfaceProfile cached = surfaceProfileCache.get(biomeID);

        if (cached != null)
            return cached;

        TerrainSurfaceProfile profile = new TerrainSurfaceProfile(
                (short) blockManager.getBlockIDFromBlockName(biomeHandle.getSurfaceBlockName()),
                (short) blockManager.getBlockIDFromBlockName(biomeHandle.getSubsurfaceBlockName()),
                (short) blockManager.getBlockIDFromBlockName(biomeHandle.getUnderwaterBlockName()));

        surfaceProfileCache.put(biomeID, profile);
        return profile;
    }

    // Generator — once per subchunk \\

    /*
     * Carves a single subchunk's blocks from the column data computeColumn()
     * already resolved for this chunk this frame. Terrain is written straight
     * through the block palette rather than SubChunkInstance.setBlock() — a
     * freshly generated subchunk holds no liquid yet, so world generation has
     * no reason to pay per-block liquid-stability invalidation on every
     * placed voxel — but ocean columns still write full liquid levels
     * directly into the liquid-level palette so FluidSimulationSystem finds
     * settled water rather than a phantom partial fill on first tick. The
     * biome palette is uniform for the whole column, so it is written with a
     * single fill() rather than looping every biome cell individually.
     */
    public boolean generateSubChunk(WorldHandle worldHandle, long chunkCoordinate, SubChunkInstance subChunkInstance) {

        TerrainColumnAsyncContainer column = terrainColumnContainer.getInstance();

        if (!column.hasComputedColumn || column.computedChunkCoordinate != chunkCoordinate)
            throwException("generateSubChunk() called for a chunk whose column data was never computed on this "
                    + "thread — computeColumn() must run once for this exact chunk coordinate first.");

        int offsetY = (int) subChunkInstance.getCoordinate() * CHUNK_SIZE;

        BlockPaletteHandle biomes = subChunkInstance.getBiomePaletteHandle();
        BlockPaletteHandle blocks = subChunkInstance.getBlockPaletteHandle();
        BlockPaletteHandle liquidLevels = subChunkInstance.getLiquidLevelPaletteHandle();

        biomes.fill(column.biomeID);

        int seaLevel = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS;
        int beachRange = EngineSetting.TERRAIN_BEACH_HEIGHT_RANGE_BLOCKS;
        int surfaceDepth = EngineSetting.TERRAIN_SURFACE_DEPTH_BLOCKS;

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {

                int groundHeight = column.groundHeightBlocks[localZ * CHUNK_SIZE + localX];

                boolean useUnderwaterBlocks = groundHeight <= seaLevel + beachRange;
                short topBlockID = useUnderwaterBlocks ? column.underwaterBlockID : column.surfaceBlockID;
                short fillBlockID = useUnderwaterBlocks ? column.underwaterBlockID : column.subsurfaceBlockID;

                int columnTop = Math.max(groundHeight, seaLevel);

                for (int localY = 0; localY < CHUNK_SIZE; localY++) {

                    int worldY = localY + offsetY;

                    if (worldY > columnTop)
                        continue;

                    if (worldY > groundHeight) {
                        blocks.setBlock(localX, localY, localZ, waterBlockId);
                        liquidLevels.setBlock(localX, localY, localZ, EngineSetting.LIQUID_LEVEL_MAX);
                        continue;
                    }

                    if (worldY == groundHeight)
                        blocks.setBlock(localX, localY, localZ, topBlockID);
                    else if (worldY > groundHeight - surfaceDepth)
                        blocks.setBlock(localX, localY, localZ, fillBlockID);
                    else
                        blocks.setBlock(localX, localY, localZ, stoneBlockId);
                }
            }
        }

        return true;
    }

    // Surface Profile \\

    private static final class TerrainSurfaceProfile {

        final short surfaceBlockID;
        final short subsurfaceBlockID;
        final short underwaterBlockID;

        TerrainSurfaceProfile(short surfaceBlockID, short subsurfaceBlockID, short underwaterBlockID) {
            this.surfaceBlockID = surfaceBlockID;
            this.subsurfaceBlockID = subsurfaceBlockID;
            this.underwaterBlockID = underwaterBlockID;
        }
    }
}