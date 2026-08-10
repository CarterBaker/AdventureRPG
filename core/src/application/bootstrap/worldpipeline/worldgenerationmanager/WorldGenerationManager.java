package application.bootstrap.worldpipeline.worldgenerationmanager;

import java.util.concurrent.ConcurrentHashMap;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
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

public class WorldGenerationManager extends ManagerPackage {

    /*
     * Drives per-chunk-column terrain generation. computeColumn() resolves the
     * one biome that governs an entire chunk column, a ground-height value for
     * every one of its 256 block-columns, and that column's min/max ground
     * height, all cached in a per-thread scratch buffer. Both the macro shape
     * and fine detail noise layers are sampled on a coarse world-aligned grid
     * and bilinearly interpolated per block rather than evaluated at full
     * per-block resolution — each layer's stride is sized well below its
     * finest octave's wavelength, so interpolation error stays far under one
     * block and the terrain reads identically while paying for a fraction of
     * the noise evaluations. generateSubChunk() then classifies each
     * subchunk against that data before touching any block palette: entirely
     * above every column's terrain is left as the default air (no write at
     * all) and flagged knownEmpty so the geometry pipeline can skip it
     * outright, entirely below every column's surface-dressing layer is
     * bulk-filled as solid stone in O(1), entirely below sea level and above
     * every column's ground is bulk-filled as water in O(1), and only a
     * subchunk that actually straddles a surface, coastline, or cliff runs
     * the precise per-block loop. Both O(1) bulk fills also mark the
     * subchunk uniformFill so GeometryBuildManager can later prove it's
     * fully enclosed by identical neighbors and skip its mesh scan too —
     * this is what keeps a tall mountain's buried interior from costing
     * anything at build time. Ground height itself comes from
     * TerrainShapeUtility and is fully independent of biome — biome only ever
     * chooses which blocks dress that shape. Chunks generate concurrently on
     * separate worker threads, so the surface-block cache below is a
     * ConcurrentHashMap rather than a locked map.
     */

    @FunctionalInterface
    private interface TerrainGridSampler {
        float sample(long seed, double worldX, double worldZ, double worldWidthBlocks, double worldHeightBlocks);
    }

    // Internal
    private BlockManager blockManager;
    private BiomeManager biomeManager;

    private TerrainColumnAsyncContainer terrainColumnContainer;
    private final ConcurrentHashMap<Short, TerrainSurfaceProfile> surfaceProfileCache = new ConcurrentHashMap<>();

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

        int macroStride = TerrainColumnAsyncContainer.MACRO_SAMPLE_STRIDE;
        int macroSamplesPerAxis = TerrainColumnAsyncContainer.MACRO_SAMPLES_PER_AXIS;
        int detailStride = TerrainColumnAsyncContainer.DETAIL_SAMPLE_STRIDE;
        int detailSamplesPerAxis = TerrainColumnAsyncContainer.DETAIL_SAMPLES_PER_AXIS;

        sampleGrid(column.macroShapeGridBlocks, TerrainShapeUtility::computeMacroShapeBlocks, seed,
                worldOffsetX, worldOffsetZ, worldWidthBlocks, worldHeightBlocks, macroStride, macroSamplesPerAxis);

        sampleGrid(column.detailGridBlocks, TerrainShapeUtility::computeDetailBlocks, seed,
                worldOffsetX, worldOffsetZ, worldWidthBlocks, worldHeightBlocks, detailStride, detailSamplesPerAxis);

        int maxGroundHeight = Integer.MIN_VALUE;
        int minGroundHeight = Integer.MAX_VALUE;

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {

                float macroShape = sampleGridBilinear(
                        column.macroShapeGridBlocks, localX, localZ, macroStride, macroSamplesPerAxis);
                float detail = sampleGridBilinear(
                        column.detailGridBlocks, localX, localZ, detailStride, detailSamplesPerAxis);

                int groundHeight = TerrainShapeUtility.finalizeGroundHeightBlocks(macroShape, detail);
                column.groundHeightBlocks[localZ * CHUNK_SIZE + localX] = groundHeight;

                if (groundHeight > maxGroundHeight)
                    maxGroundHeight = groundHeight;
                if (groundHeight < minGroundHeight)
                    minGroundHeight = groundHeight;
            }
        }

        column.columnMaxGroundHeightBlocks = maxGroundHeight;
        column.columnMinGroundHeightBlocks = minGroundHeight;
        column.columnTopBlocks = Math.max(maxGroundHeight, EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS);

        column.computedChunkCoordinate = chunkCoordinate;
        column.hasComputedColumn = true;
    }

    private void sampleGrid(
            float[] grid,
            TerrainGridSampler sampler,
            long seed,
            long worldOffsetX,
            long worldOffsetZ,
            double worldWidthBlocks,
            double worldHeightBlocks,
            int stride,
            int samplesPerAxis) {

        for (int gz = 0; gz < samplesPerAxis; gz++) {
            for (int gx = 0; gx < samplesPerAxis; gx++) {

                double sampleWorldX = worldOffsetX + gx * stride;
                double sampleWorldZ = worldOffsetZ + gz * stride;

                grid[gz * samplesPerAxis + gx] = sampler.sample(
                        seed, sampleWorldX, sampleWorldZ, worldWidthBlocks, worldHeightBlocks);
            }
        }
    }

    private float sampleGridBilinear(float[] grid, int localX, int localZ, int stride, int samplesPerAxis) {

        int cellX = localX / stride;
        int cellZ = localZ / stride;

        float tx = (localX % stride) / (float) stride;
        float tz = (localZ % stride) / (float) stride;

        float v00 = grid[cellZ * samplesPerAxis + cellX];
        float v10 = grid[cellZ * samplesPerAxis + (cellX + 1)];
        float v01 = grid[(cellZ + 1) * samplesPerAxis + cellX];
        float v11 = grid[(cellZ + 1) * samplesPerAxis + (cellX + 1)];

        float top = v00 + (v10 - v00) * tx;
        float bottom = v01 + (v11 - v01) * tx;

        return top + (bottom - top) * tz;
    }

    private TerrainSurfaceProfile resolveSurfaceProfile(BiomeHandle biomeHandle) {
        return surfaceProfileCache.computeIfAbsent(biomeHandle.getBiomeID(), id -> new TerrainSurfaceProfile(
                (short) blockManager.getBlockIDFromBlockName(biomeHandle.getSurfaceBlockName()),
                (short) blockManager.getBlockIDFromBlockName(biomeHandle.getSubsurfaceBlockName()),
                (short) blockManager.getBlockIDFromBlockName(biomeHandle.getUnderwaterBlockName())));
    }

    // Generator — once per subchunk \\

    public boolean generateSubChunk(WorldHandle worldHandle, long chunkCoordinate, SubChunkInstance subChunkInstance) {

        TerrainColumnAsyncContainer column = terrainColumnContainer.getInstance();

        if (!column.hasComputedColumn || column.computedChunkCoordinate != chunkCoordinate)
            throwException("generateSubChunk() called for a chunk whose column data was never computed on this "
                    + "thread — computeColumn() must run once for this exact chunk coordinate first.");

        subChunkInstance.clearKnownEmpty();
        subChunkInstance.clearUniformFill();

        BlockPaletteHandle biomes = subChunkInstance.getBiomePaletteHandle();
        biomes.fill(column.biomeID);

        int offsetY = (int) subChunkInstance.getCoordinate() * CHUNK_SIZE;

        if (offsetY > column.columnTopBlocks) {
            subChunkInstance.markKnownEmpty();
            return true;
        }

        int surfaceDepth = EngineSetting.TERRAIN_SURFACE_DEPTH_BLOCKS;
        int seaLevel = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS;
        int subChunkTopY = offsetY + CHUNK_SIZE - 1;

        BlockPaletteHandle blocks = subChunkInstance.getBlockPaletteHandle();
        BlockPaletteHandle liquidLevels = subChunkInstance.getLiquidLevelPaletteHandle();

        if (subChunkTopY + surfaceDepth <= column.columnMinGroundHeightBlocks) {
            blocks.fill(stoneBlockId);
            subChunkInstance.markUniformFill(DynamicGeometryType.FULL, stoneBlockId);
            return true;
        }

        if (offsetY > column.columnMaxGroundHeightBlocks && subChunkTopY <= seaLevel) {
            blocks.fill(waterBlockId);
            liquidLevels.fill(EngineSetting.LIQUID_LEVEL_MAX);
            subChunkInstance.markUniformFill(DynamicGeometryType.LIQUID, waterBlockId);
            return true;
        }

        int beachRange = EngineSetting.TERRAIN_BEACH_HEIGHT_RANGE_BLOCKS;

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {

                int groundHeight = column.groundHeightBlocks[localZ * CHUNK_SIZE + localX];
                int columnTop = Math.max(groundHeight, seaLevel);

                if (offsetY > columnTop)
                    continue;

                boolean useUnderwaterBlocks = groundHeight <= seaLevel + beachRange;
                short topBlockID = useUnderwaterBlocks ? column.underwaterBlockID : column.surfaceBlockID;
                short fillBlockID = useUnderwaterBlocks ? column.underwaterBlockID : column.subsurfaceBlockID;

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