package application.bootstrap.worldpipeline.worldgenerationmanager;

import java.util.concurrent.ConcurrentHashMap;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.TerrainShapeUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;

public class WorldGenerationManager extends ManagerPackage {

    /*
     * Drives per-chunk-column terrain generation. computeColumn() evaluates
     * the biome field once per macro grid point at that point's own world
     * position, derives ground height, detail response, ocean share, and
     * dressing blocks from the field there, and interpolates all of it down
     * to the chunk's 256 block columns — so biome influence varies inside a
     * chunk instead of being chosen for the whole chunk at its center, and a
     * grid point on a chunk edge resolves identically from either neighbor.
     * Whether a column floods below sea level is likewise a per-column
     * decision, thresholded against the blended ocean share rather than read
     * off one biome's flag, which is what lets a shoreline cut diagonally
     * across a chunk and lets a dry biome keep a below-sea-level valley.
     * generateSubChunk() then classifies each subchunk against that data
     * before any storage is realized, so a subchunk entirely above every
     * column's terrain stays knownEmpty and one entirely below the surface
     * layer stays uniformFill — neither ever allocates a palette — and only a
     * subchunk actually straddling a surface, coastline, or cliff realizes
     * real per-block storage. Every output is a pure function of (seed,
     * coordinate), so the whole pass is skipped when the caller's
     * GenerationCacheStruct already holds a result for this exact coordinate.
     * Chunks generate concurrently on separate worker threads, so the
     * surface-profile cache below is a ConcurrentHashMap rather than a locked
     * map. The probe resolves ground height and flooding for any single world
     * column through that column's own chunk grids on a separate thread-local
     * container, so structure placement can anchor across chunk borders and
     * always agree exactly with the terrain the neighbor generates.
     */

    // Internal
    private BlockManager blockManager;
    private BiomeManager biomeManager;

    private TerrainColumnAsyncContainer terrainColumnContainer;
    private TerrainColumnAsyncContainer probeColumnContainer;
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
        this.probeColumnContainer = create(TerrainColumnAsyncContainer.class);
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

    public void computeColumn(WorldHandle worldHandle, long chunkCoordinate, GenerationCacheStruct terrainCache) {

        TerrainColumnAsyncContainer column = terrainColumnContainer.getInstance();

        if (terrainCache.isValidFor(chunkCoordinate)) {
            applyCachedColumn(column, worldHandle, chunkCoordinate, terrainCache);
            return;
        }

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        long seed = worldHandle.getSeed();
        long worldOffsetX = (long) chunkX * CHUNK_SIZE;
        long worldOffsetZ = (long) chunkZ * CHUNK_SIZE;

        double worldWidthBlocks = worldHandle.getWorldScale().x;
        double worldHeightBlocks = worldHandle.getWorldScale().y;

        sampleMacroGrid(worldHandle, column, seed, worldOffsetX, worldOffsetZ, worldWidthBlocks, worldHeightBlocks);
        sampleDetailGrid(column, seed, worldOffsetX, worldOffsetZ, worldWidthBlocks, worldHeightBlocks);
        resolveBlockColumns(column, seed, worldOffsetX, worldOffsetZ);

        column.biomeID = column.macroBiomeIDGrid[TerrainColumnAsyncContainer.MACRO_CENTER_INDEX];
        column.allFillBlocksFullGeometry = resolveFillGeometryUniformity(column);

        column.computedWorldHandle = worldHandle;
        column.computedChunkCoordinate = chunkCoordinate;
        column.hasComputedColumn = true;

        terrainCache.store(
                chunkCoordinate,
                column.biomeID,
                column.groundHeightBlocks,
                column.columnSurfaceBlockID,
                column.columnSubsurfaceBlockID,
                column.columnUnderwaterBlockID,
                column.columnOceanWater,
                column.columnMinGroundHeightBlocks,
                column.columnMaxGroundHeightBlocks,
                column.columnTopBlocks,
                column.allOceanWater,
                column.allFillBlocksFullGeometry);
    }

    /*
     * One biome field evaluation per macro grid point, and every biome-derived
     * value for that point taken from it. The grid is world-aligned and its
     * outermost samples sit exactly on the chunk boundary, so the neighboring
     * chunk evaluates the same world position and lands on the same result.
     */
    private void sampleMacroGrid(
            WorldHandle worldHandle,
            TerrainColumnAsyncContainer column,
            long seed,
            long worldOffsetX, long worldOffsetZ,
            double worldWidthBlocks, double worldHeightBlocks) {

        int stride = TerrainColumnAsyncContainer.MACRO_SAMPLE_STRIDE;
        int samplesPerAxis = TerrainColumnAsyncContainer.MACRO_SAMPLES_PER_AXIS;

        for (int gz = 0; gz < samplesPerAxis; gz++) {
            for (int gx = 0; gx < samplesPerAxis; gx++) {

                int index = gz * samplesPerAxis + gx;

                double sampleWorldX = worldOffsetX + gx * stride;
                double sampleWorldZ = worldOffsetZ + gz * stride;

                BiomeBlendStruct blend = column.macroBlend[index];

                biomeManager.sampleBiomeField(worldHandle, sampleWorldX, sampleWorldZ, blend);

                column.macroShapeGridBlocks[index] = TerrainShapeUtility.computeMacroShapeBlocks(
                        seed, sampleWorldX, sampleWorldZ, worldWidthBlocks, worldHeightBlocks, blend);

                column.macroDetailAmplitudeGrid[index] = TerrainShapeUtility.computeDetailAmplitudeBlocks(blend);
                column.macroDetailWavelengthGrid[index] = TerrainShapeUtility.computeDetailWavelengthBlocks(blend);
                column.macroOceanWeightGrid[index] = blend.getOceanWeight();

                BiomeHandle dominantBiome = blend.getDominantBiome();
                TerrainSurfaceProfile profile = resolveSurfaceProfile(dominantBiome);

                column.macroBiomeIDGrid[index] = dominantBiome.getBiomeID();
                column.macroSurfaceBlockIDGrid[index] = profile.surfaceBlockID;
                column.macroSubsurfaceBlockIDGrid[index] = profile.subsurfaceBlockID;
                column.macroUnderwaterBlockIDGrid[index] = profile.underwaterBlockID;
            }
        }
    }

    /*
     * Detail roughness on its own finer grid, sampled with the amplitude and
     * wavelength interpolated out of the macro grid rather than re-evaluating
     * the biome field at detail resolution — the field is expensive and its
     * influence on a few blocks of roughness is smooth enough that
     * interpolating the two scalars is indistinguishable from resolving them.
     */
    private void sampleDetailGrid(
            TerrainColumnAsyncContainer column,
            long seed,
            long worldOffsetX, long worldOffsetZ,
            double worldWidthBlocks, double worldHeightBlocks) {

        int stride = TerrainColumnAsyncContainer.DETAIL_SAMPLE_STRIDE;
        int samplesPerAxis = TerrainColumnAsyncContainer.DETAIL_SAMPLES_PER_AXIS;

        for (int gz = 0; gz < samplesPerAxis; gz++) {
            for (int gx = 0; gx < samplesPerAxis; gx++) {

                int localX = gx * stride;
                int localZ = gz * stride;

                float amplitude = sampleMacroBilinear(column.macroDetailAmplitudeGrid, localX, localZ);
                float wavelength = sampleMacroBilinear(column.macroDetailWavelengthGrid, localX, localZ);

                column.detailGridBlocks[gz * samplesPerAxis + gx] = TerrainShapeUtility.computeDetailBlocks(
                        seed, worldOffsetX + localX, worldOffsetZ + localZ,
                        worldWidthBlocks, worldHeightBlocks, wavelength, amplitude);
            }
        }
    }

    /*
     * Interpolates the grids down to the chunk's 256 block columns. Height and
     * ocean share interpolate as scalars so both stay smooth; dressing blocks
     * cannot interpolate, so each column draws its profile from one of the
     * four surrounding grid points chosen by a position-hashed roll against
     * their bilinear weights. Where those four agree — everywhere but within
     * one macro cell of a material border — the roll is a no-op, and where
     * they disagree it dithers the border into an interlocking edge instead
     * of a drawn line.
     */
    private void resolveBlockColumns(
            TerrainColumnAsyncContainer column,
            long seed,
            long worldOffsetX, long worldOffsetZ) {

        int seaLevel = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS;

        int maxGroundHeight = Integer.MIN_VALUE;
        int minGroundHeight = Integer.MAX_VALUE;
        int columnTop = Integer.MIN_VALUE;
        boolean allOceanWater = true;

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {

                int columnIndex = localZ * CHUNK_SIZE + localX;

                int groundHeight = resolveGroundHeight(column, localX, localZ);
                column.groundHeightBlocks[columnIndex] = groundHeight;

                boolean oceanWater = resolveOceanWater(column, localX, localZ);
                column.columnOceanWater[columnIndex] = oceanWater;

                int cornerIndex = pickMacroCorner(
                        seed, worldOffsetX + localX, worldOffsetZ + localZ, localX, localZ);

                column.columnSurfaceBlockID[columnIndex] = column.macroSurfaceBlockIDGrid[cornerIndex];
                column.columnSubsurfaceBlockID[columnIndex] = column.macroSubsurfaceBlockIDGrid[cornerIndex];
                column.columnUnderwaterBlockID[columnIndex] = column.macroUnderwaterBlockIDGrid[cornerIndex];

                int top = oceanWater ? Math.max(groundHeight, seaLevel) : groundHeight;

                if (groundHeight > maxGroundHeight)
                    maxGroundHeight = groundHeight;

                if (groundHeight < minGroundHeight)
                    minGroundHeight = groundHeight;

                if (top > columnTop)
                    columnTop = top;

                if (!oceanWater)
                    allOceanWater = false;
            }
        }

        column.columnMaxGroundHeightBlocks = maxGroundHeight;
        column.columnMinGroundHeightBlocks = minGroundHeight;
        column.columnTopBlocks = columnTop;
        column.allOceanWater = allOceanWater;
    }

    private int resolveGroundHeight(TerrainColumnAsyncContainer column, int localX, int localZ) {

        float macroShape = sampleMacroBilinear(column.macroShapeGridBlocks, localX, localZ);
        float detail = sampleDetailBilinear(column.detailGridBlocks, localX, localZ);

        return TerrainShapeUtility.finalizeGroundHeightBlocks(macroShape, detail);
    }

    private boolean resolveOceanWater(TerrainColumnAsyncContainer column, int localX, int localZ) {
        return sampleMacroBilinear(column.macroOceanWeightGrid, localX, localZ)
                > EngineSetting.BIOME_OCEAN_FLOOD_THRESHOLD;
    }

    private void applyCachedColumn(
            TerrainColumnAsyncContainer column,
            WorldHandle worldHandle,
            long chunkCoordinate,
            GenerationCacheStruct terrainCache) {

        column.biomeID = terrainCache.getBiomeID();

        terrainCache.copyGroundHeightsInto(column.groundHeightBlocks);
        terrainCache.copySurfaceBlockIDsInto(column.columnSurfaceBlockID);
        terrainCache.copySubsurfaceBlockIDsInto(column.columnSubsurfaceBlockID);
        terrainCache.copyUnderwaterBlockIDsInto(column.columnUnderwaterBlockID);
        terrainCache.copyOceanWaterInto(column.columnOceanWater);

        column.columnMinGroundHeightBlocks = terrainCache.getColumnMinGroundHeightBlocks();
        column.columnMaxGroundHeightBlocks = terrainCache.getColumnMaxGroundHeightBlocks();
        column.columnTopBlocks = terrainCache.getColumnTopBlocks();

        column.allOceanWater = terrainCache.hasAllOceanWater();
        column.allFillBlocksFullGeometry = terrainCache.hasAllFillBlocksFullGeometry();

        column.computedWorldHandle = worldHandle;
        column.computedChunkCoordinate = chunkCoordinate;
        column.hasComputedColumn = true;
    }

    // Probe — any single block column \\

    public int probeGroundHeight(WorldHandle worldHandle, long worldX, long worldZ) {

        long wrappedX = WorldWrapUtility.wrapBlockX(worldHandle, worldX);
        long wrappedZ = WorldWrapUtility.wrapBlockZ(worldHandle, worldZ);

        TerrainColumnAsyncContainer probe = resolveProbeColumn(worldHandle, wrappedX, wrappedZ);

        return resolveGroundHeight(probe, (int) (wrappedX % CHUNK_SIZE), (int) (wrappedZ % CHUNK_SIZE));
    }

    public boolean probeFlooded(WorldHandle worldHandle, long worldX, long worldZ) {

        long wrappedX = WorldWrapUtility.wrapBlockX(worldHandle, worldX);
        long wrappedZ = WorldWrapUtility.wrapBlockZ(worldHandle, worldZ);

        TerrainColumnAsyncContainer probe = resolveProbeColumn(worldHandle, wrappedX, wrappedZ);

        int localX = (int) (wrappedX % CHUNK_SIZE);
        int localZ = (int) (wrappedZ % CHUNK_SIZE);

        return resolveOceanWater(probe, localX, localZ)
                && resolveGroundHeight(probe, localX, localZ) < EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS;
    }

    private TerrainColumnAsyncContainer resolveProbeColumn(WorldHandle worldHandle, long wrappedX, long wrappedZ) {

        TerrainColumnAsyncContainer probe = probeColumnContainer.getInstance();

        int chunkX = (int) (wrappedX / CHUNK_SIZE);
        int chunkZ = (int) (wrappedZ / CHUNK_SIZE);
        long chunkCoordinate = Coordinate2Long.pack(chunkX, chunkZ);

        if (probe.hasComputedColumn
                && probe.computedWorldHandle == worldHandle
                && probe.computedChunkCoordinate == chunkCoordinate)
            return probe;

        long seed = worldHandle.getSeed();
        long worldOffsetX = (long) chunkX * CHUNK_SIZE;
        long worldOffsetZ = (long) chunkZ * CHUNK_SIZE;

        double worldWidthBlocks = worldHandle.getWorldScale().x;
        double worldHeightBlocks = worldHandle.getWorldScale().y;

        sampleMacroGrid(worldHandle, probe, seed, worldOffsetX, worldOffsetZ, worldWidthBlocks, worldHeightBlocks);
        sampleDetailGrid(probe, seed, worldOffsetX, worldOffsetZ, worldWidthBlocks, worldHeightBlocks);

        probe.computedWorldHandle = worldHandle;
        probe.computedChunkCoordinate = chunkCoordinate;
        probe.hasComputedColumn = true;

        return probe;
    }

    // Grid Interpolation \\

    private float sampleMacroBilinear(float[] grid, int localX, int localZ) {
        return sampleGridBilinear(
                grid, localX, localZ,
                TerrainColumnAsyncContainer.MACRO_SAMPLE_STRIDE,
                TerrainColumnAsyncContainer.MACRO_SAMPLES_PER_AXIS);
    }

    private float sampleDetailBilinear(float[] grid, int localX, int localZ) {
        return sampleGridBilinear(
                grid, localX, localZ,
                TerrainColumnAsyncContainer.DETAIL_SAMPLE_STRIDE,
                TerrainColumnAsyncContainer.DETAIL_SAMPLES_PER_AXIS);
    }

    /*
     * Cell index is clamped one short of the last sample so a coordinate
     * landing exactly on the far edge of the grid — which the detail grid does
     * at local 16 — interpolates against the final pair rather than reading
     * past the end.
     */
    private float sampleGridBilinear(float[] grid, int localX, int localZ, int stride, int samplesPerAxis) {

        int maxCell = samplesPerAxis - 2;

        int cellX = Math.min(localX / stride, maxCell);
        int cellZ = Math.min(localZ / stride, maxCell);

        float tx = (localX - cellX * stride) / (float) stride;
        float tz = (localZ - cellZ * stride) / (float) stride;

        float v00 = grid[cellZ * samplesPerAxis + cellX];
        float v10 = grid[cellZ * samplesPerAxis + (cellX + 1)];
        float v01 = grid[(cellZ + 1) * samplesPerAxis + cellX];
        float v11 = grid[(cellZ + 1) * samplesPerAxis + (cellX + 1)];

        float top = v00 + (v10 - v00) * tx;
        float bottom = v01 + (v11 - v01) * tx;

        return top + (bottom - top) * tz;
    }

    private int pickMacroCorner(long seed, long worldX, long worldZ, int localX, int localZ) {

        int samplesPerAxis = TerrainColumnAsyncContainer.MACRO_SAMPLES_PER_AXIS;
        int stride = TerrainColumnAsyncContainer.MACRO_SAMPLE_STRIDE;
        int maxCell = samplesPerAxis - 2;

        int cellX = Math.min(localX / stride, maxCell);
        int cellZ = Math.min(localZ / stride, maxCell);

        float tx = (localX - cellX * stride) / (float) stride;
        float tz = (localZ - cellZ * stride) / (float) stride;

        int index00 = cellZ * samplesPerAxis + cellX;
        int index10 = index00 + 1;
        int index01 = index00 + samplesPerAxis;
        int index11 = index01 + 1;

        float weight00 = (1f - tx) * (1f - tz);
        float weight10 = tx * (1f - tz);
        float weight01 = (1f - tx) * tz;

        float roll = BiomeFieldUtility.hash01(
                seed ^ EngineSetting.BIOME_MATERIAL_DITHER_SEED
                        ^ (worldX * EngineSetting.HASH_FINALIZER_MULTIPLIER_1)
                        ^ (worldZ * EngineSetting.HASH_FINALIZER_MULTIPLIER_2));

        if (roll < weight00)
            return index00;

        roll -= weight00;

        if (roll < weight10)
            return index10;

        roll -= weight10;

        if (roll < weight01)
            return index01;

        return index11;
    }

    private TerrainSurfaceProfile resolveSurfaceProfile(BiomeHandle biomeHandle) {
        return surfaceProfileCache.computeIfAbsent(biomeHandle.getBiomeID(), id -> new TerrainSurfaceProfile(
                (short) blockManager.getBlockIDFromBlockName(biomeHandle.getSurfaceBlockName()),
                (short) blockManager.getBlockIDFromBlockName(biomeHandle.getSubsurfaceBlockName()),
                (short) blockManager.getBlockIDFromBlockName(biomeHandle.getUnderwaterBlockName())));
    }

    /*
     * Confirms every solid fill block any column of this chunk can possibly
     * have written — every macro grid point's surface, subsurface, and
     * underwater block, plus stone — is FULL geometry before a subchunk with
     * no air or liquid is allowed to skip geometry building as opaque
     * interior. Almost always true; only a biome authored with a non-FULL
     * surface block (a decorative slab-shaped ground cover, say) would fail
     * it, in which case the subchunk simply falls back to the general
     * per-block walk like it always has.
     */
    private boolean resolveFillGeometryUniformity(TerrainColumnAsyncContainer column) {

        if (!isFullGeometry(stoneBlockId))
            return false;

        for (int i = 0; i < TerrainColumnAsyncContainer.MACRO_SAMPLE_COUNT; i++)
            if (!isFullGeometry(column.macroSurfaceBlockIDGrid[i])
                    || !isFullGeometry(column.macroSubsurfaceBlockIDGrid[i])
                    || !isFullGeometry(column.macroUnderwaterBlockIDGrid[i]))
                return false;

        return true;
    }

    private boolean isFullGeometry(short blockID) {
        return blockManager.getBlockHandleFromBlockID(blockID).getGeometry() == DynamicGeometryType.FULL;
    }

    // Generator — once per subchunk \\

    public boolean generateSubChunk(WorldHandle worldHandle, long chunkCoordinate, SubChunkInstance subChunkInstance) {

        TerrainColumnAsyncContainer column = requireComputedColumn(chunkCoordinate);

        subChunkInstance.beginGeneration(column.biomeID);

        int offsetY = (int) subChunkInstance.getCoordinate() * CHUNK_SIZE;

        if (offsetY > column.columnTopBlocks) {
            subChunkInstance.markKnownEmpty();
            return true;
        }

        int surfaceDepth = EngineSetting.TERRAIN_SURFACE_DEPTH_BLOCKS;
        int seaLevel = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS;
        int subChunkTopY = offsetY + CHUNK_SIZE - 1;

        if (subChunkTopY + surfaceDepth <= column.columnMinGroundHeightBlocks) {
            subChunkInstance.markUniformFill(DynamicGeometryType.FULL, stoneBlockId);
            return true;
        }

        if (column.allOceanWater && offsetY > column.columnMaxGroundHeightBlocks && subChunkTopY <= seaLevel) {
            subChunkInstance.markUniformFill(DynamicGeometryType.LIQUID, waterBlockId);
            return true;
        }

        BlockPaletteHandle blocks = subChunkInstance.getBlockPaletteHandle();
        BlockPaletteHandle liquidLevels = subChunkInstance.getLiquidLevelPaletteHandle();

        int beachRange = EngineSetting.TERRAIN_BEACH_HEIGHT_RANGE_BLOCKS;

        short uniformBlockID = airBlockId;
        boolean uniformKnown = false;
        boolean isUniform = true;
        boolean hasAirOrWater = false;

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {

                int columnIndex = localZ * CHUNK_SIZE + localX;

                int groundHeight = column.groundHeightBlocks[columnIndex];
                boolean oceanWater = column.columnOceanWater[columnIndex];
                int columnTop = oceanWater ? Math.max(groundHeight, seaLevel) : groundHeight;

                if (offsetY > columnTop) {
                    hasAirOrWater = true;
                    if (isUniform) {
                        if (!uniformKnown) {
                            uniformBlockID = airBlockId;
                            uniformKnown = true;
                        } else if (uniformBlockID != airBlockId) {
                            isUniform = false;
                        }
                    }
                    continue;
                }

                boolean useUnderwaterBlocks = oceanWater && groundHeight <= seaLevel + beachRange;

                short topBlockID = useUnderwaterBlocks
                        ? column.columnUnderwaterBlockID[columnIndex]
                        : column.columnSurfaceBlockID[columnIndex];

                short fillBlockID = useUnderwaterBlocks
                        ? column.columnUnderwaterBlockID[columnIndex]
                        : column.columnSubsurfaceBlockID[columnIndex];

                for (int localY = 0; localY < CHUNK_SIZE; localY++) {

                    int worldY = localY + offsetY;
                    short resultBlockID;

                    if (worldY > columnTop) {
                        resultBlockID = airBlockId;
                        hasAirOrWater = true;
                    } else if (worldY > groundHeight) {
                        resultBlockID = waterBlockId;
                        hasAirOrWater = true;
                        blocks.setBlock(localX, localY, localZ, waterBlockId);
                        liquidLevels.setBlock(localX, localY, localZ, EngineSetting.LIQUID_LEVEL_MAX);
                    } else if (worldY == groundHeight) {
                        resultBlockID = topBlockID;
                        blocks.setBlock(localX, localY, localZ, topBlockID);
                    } else if (worldY > groundHeight - surfaceDepth) {
                        resultBlockID = fillBlockID;
                        blocks.setBlock(localX, localY, localZ, fillBlockID);
                    } else {
                        resultBlockID = stoneBlockId;
                        blocks.setBlock(localX, localY, localZ, stoneBlockId);
                    }

                    if (isUniform) {
                        if (!uniformKnown) {
                            uniformBlockID = resultBlockID;
                            uniformKnown = true;
                        } else if (uniformBlockID != resultBlockID) {
                            isUniform = false;
                        }
                    }
                }
            }
        }

        if (isUniform) {
            DynamicGeometryType uniformGeometry = uniformBlockID == airBlockId
                    ? DynamicGeometryType.NONE
                    : blockManager.getBlockHandleFromBlockID(uniformBlockID).getGeometry();
            subChunkInstance.collapseGeneratedUniform(uniformBlockID, uniformGeometry);
        } else if (!hasAirOrWater && column.allFillBlocksFullGeometry) {
            subChunkInstance.markOpaqueInterior();
        }

        subChunkInstance.setLiquidStable(true);

        return true;
    }

    private TerrainColumnAsyncContainer requireComputedColumn(long chunkCoordinate) {

        TerrainColumnAsyncContainer column = terrainColumnContainer.getInstance();

        if (!column.hasComputedColumn || column.computedChunkCoordinate != chunkCoordinate)
            throwException("Column data was read for a chunk whose column was never computed on this thread — "
                    + "computeColumn() must run once for this exact chunk coordinate first.");

        return column;
    }

    // Accessible \\

    public int getColumnGroundHeight(long chunkCoordinate, int localX, int localZ) {
        return requireComputedColumn(chunkCoordinate).groundHeightBlocks[localZ * CHUNK_SIZE + localX];
    }

    // Surface Profile \\

    private static final class TerrainSurfaceProfile {

        final short surfaceBlockID;
        final short subsurfaceBlockID;
        final short underwaterBlockID;

        TerrainSurfaceProfile(
                short surfaceBlockID,
                short subsurfaceBlockID,
                short underwaterBlockID) {
            this.surfaceBlockID = surfaceBlockID;
            this.subsurfaceBlockID = subsurfaceBlockID;
            this.underwaterBlockID = underwaterBlockID;
        }
    }
}