package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.oceanpipeline.tidemanager.TideManager;
import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.biome.BiomeCoveringStruct;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.BiomeVeinStruct;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.coveringmanager.CoveringManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.util.TerrainCarveUtility;
import application.bootstrap.worldpipeline.util.TerrainFeatureStruct;
import application.bootstrap.worldpipeline.util.TerrainShapeUtility;
import application.bootstrap.worldpipeline.util.TideUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.graphics.color.Color;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class WorldGenerationManager extends ManagerPackage {

    /*
     * Generates terrain per chunk column. computeColumn() samples the biome
     * field on a macro grid, shapes the ground through cliffs, sea cliffs and
     * still water, and resolves every block column's ground, dressing, water,
     * the band its caves and sea caves may hollow, its veins and the lowest
     * cell the tide reaches. A column's top block takes the coverage its
     * biome lays over that ground, thinned per column by the covering's
     * variance. generateSubChunk() fills subchunks, hollowing and
     * threading them through world-aligned noise lattices, and leaves fully
     * empty or uniform ones unrealized. The sea stands at sea level and rides
     * the tide, carried into caves that open on it; still water keeps the
     * level its biome declares. sampleSurface() gives the ground, the water
     * over it and its colors at any point for distant macro terrain and maps,
     * and sampleOpenWater() whether the sea covers it. Output is a pure
     * function of seed and coordinate, so it is cached per chunk and agrees
     * across chunk borders. Surface profiles are cached per biome and dropped
     * whenever a biome is rebuilt live.
     */

    // Internal
    private BlockManager blockManager;
    private BiomeManager biomeManager;
    private CoveringManager coveringManager;
    private TideManager tideManager;

    private TerrainColumnAsyncContainer terrainColumnContainer;
    private TerrainColumnAsyncContainer probeColumnContainer;
    private volatile Short2ObjectOpenHashMap<TerrainSurfaceProfileStruct> biomeID2SurfaceProfile =
            new Short2ObjectOpenHashMap<>();
    private volatile int surfaceProfileRevision;

    private int CHUNK_SIZE;
    private int seaLevel;
    private int seaFeatureFloorY;
    private int seaFeatureCeilingY;

    // Blocks
    private short airBlockId;
    private short stoneBlockId;
    private short waterBlockId;

    // Base \\

    @Override
    protected void create() {
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        this.seaLevel = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS;
        this.seaFeatureFloorY = TerrainCarveUtility.getSeaFeatureFloorY();
        this.seaFeatureCeilingY = TerrainCarveUtility.getSeaFeatureCeilingY();
        this.terrainColumnContainer = create(TerrainColumnAsyncContainer.class);
        this.probeColumnContainer = create(TerrainColumnAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.coveringManager = get(CoveringManager.class);
        this.tideManager = get(TideManager.class);
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
        column.tideSurfaceLevels = tideManager.getSurfaceLevels();

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
        resolveCornerHeights(column);
        resolveBlockColumns(column, seed, worldOffsetX, worldOffsetZ);
        resolveVeinPalette(column);
        resolveTideFloors(column, seed, worldOffsetX, worldOffsetZ, worldWidthBlocks, worldHeightBlocks);

        column.biomeID = column.macroBiomeIDGrid[TerrainColumnAsyncContainer.MACRO_CENTER_INDEX];
        column.allFillBlocksFullGeometry = resolveFillGeometryUniformity(column);

        column.computedWorldHandle = worldHandle;
        column.computedChunkCoordinate = chunkCoordinate;
        column.hasComputedColumn = true;

        terrainCache.store(column);
    }

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

                TerrainShapeUtility.resolveFeatures(
                        seed, sampleWorldX, sampleWorldZ, worldWidthBlocks, worldHeightBlocks,
                        blend, column.macroFeatures[index]);

                column.macroShapeGridBlocks[index] = TerrainShapeUtility.computeMacroShapeBlocks(
                        seed, sampleWorldX, sampleWorldZ, worldWidthBlocks, worldHeightBlocks, blend);

                BiomeHandle dominantBiome = blend.getDominantBiome();

                column.macroBiomeIDGrid[index] = dominantBiome.getBiomeID();
                column.macroProfile[index] = resolveSurfaceProfile(dominantBiome);
                column.macroInlandProfile[index] = resolveSurfaceProfile(blend.getDominantLandBiome());
            }
        }
    }

    private void sampleDetailGrid(
            TerrainColumnAsyncContainer column,
            long seed,
            long worldOffsetX, long worldOffsetZ,
            double worldWidthBlocks, double worldHeightBlocks) {

        int stride = TerrainColumnAsyncContainer.DETAIL_SAMPLE_STRIDE;
        int samplesPerAxis = TerrainColumnAsyncContainer.DETAIL_SAMPLES_PER_AXIS;
        TerrainFeatureStruct features = column.features;

        for (int gz = 0; gz < samplesPerAxis; gz++) {
            for (int gx = 0; gx < samplesPerAxis; gx++) {

                int localX = gx * stride;
                int localZ = gz * stride;

                blendFeatures(column, localX, localZ);

                int index = gz * samplesPerAxis + gx;
                long worldX = worldOffsetX + localX;
                long worldZ = worldOffsetZ + localZ;

                column.detailGridBlocks[index] = TerrainShapeUtility.computeDetailBlocks(
                        seed, worldX, worldZ, worldWidthBlocks, worldHeightBlocks, features);
                column.ridgeGridBlocks[index] = TerrainShapeUtility.computeRidgeBlocks(
                        seed, worldX, worldZ, worldWidthBlocks, worldHeightBlocks, features);
            }
        }
    }

    private void resolveCornerHeights(TerrainColumnAsyncContainer column) {

        int cornersPerAxis = TerrainColumnAsyncContainer.CORNERS_PER_AXIS;

        for (int cornerZ = 0; cornerZ < cornersPerAxis; cornerZ++) {
            for (int cornerX = 0; cornerX < cornersPerAxis; cornerX++) {

                int cornerIndex = cornerZ * cornersPerAxis + cornerX;
                float macroShape = sampleMacroBilinear(column.macroShapeGridBlocks, cornerX, cornerZ);
                float ridge = sampleDetailBilinear(column.ridgeGridBlocks, cornerX, cornerZ);
                float detail = sampleDetailBilinear(column.detailGridBlocks, cornerX, cornerZ);

                blendFeatures(column, cornerX, cornerZ);

                column.cornerRawHeightBlocks[cornerIndex] = macroShape + ridge;
                column.cornerHeightBlocks[cornerIndex] = TerrainShapeUtility.shapeGroundHeightBlocks(
                        macroShape, ridge, detail, column.features);
            }
        }
    }

    // Leaves the position's blended features in column.features
    private float resolveShapedHeight(TerrainColumnAsyncContainer column, int localX, int localZ) {

        blendFeatures(column, localX, localZ);

        return TerrainShapeUtility.shapeGroundHeightBlocks(
                sampleMacroBilinear(column.macroShapeGridBlocks, localX, localZ),
                sampleDetailBilinear(column.ridgeGridBlocks, localX, localZ),
                sampleDetailBilinear(column.detailGridBlocks, localX, localZ),
                column.features);
    }

    private void resolveBlockColumns(
            TerrainColumnAsyncContainer column,
            long seed,
            long worldOffsetX, long worldOffsetZ) {

        int maxGroundHeight = Integer.MIN_VALUE;
        int minGroundHeight = Integer.MAX_VALUE;
        int columnTop = Integer.MIN_VALUE;
        boolean allOceanWater = true;

        column.carveMinY = Integer.MAX_VALUE;
        column.carveMaxY = Integer.MIN_VALUE;
        column.hasSeaFeatures = false;
        column.veinMinY = Integer.MAX_VALUE;
        column.veinMaxY = Integer.MIN_VALUE;

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {

                int columnIndex = localZ * CHUNK_SIZE + localX;
                int cornerIndex = localZ * TerrainColumnAsyncContainer.CORNERS_PER_AXIS + localX;

                blendFeatures(column, localX, localZ);

                TerrainFeatureStruct features = column.features;
                int groundHeight = TerrainShapeUtility.finalizeGroundHeightBlocks(
                        column.cornerHeightBlocks[cornerIndex]);
                boolean oceanWater = TerrainShapeUtility.isOceanReached(features);
                boolean lakeWater = TerrainShapeUtility.isLakeCovered(features, groundHeight);

                column.groundHeightBlocks[columnIndex] = groundHeight;
                column.columnOceanWater[columnIndex] = oceanWater;
                column.columnLakeLevelBlocks[columnIndex] = lakeWater
                        ? features.getLakeLevelBlocks()
                        : TerrainFeatureStruct.LAKE_LEVEL_UNDEFINED;

                int macroCorner = pickMacroCorner(
                        seed, worldOffsetX + localX, worldOffsetZ + localZ, localX, localZ);

                resolveColumnDressing(
                        column, seed, worldOffsetX + localX, worldOffsetZ + localZ,
                        localX, localZ, columnIndex, macroCorner, features);
                resolveColumnSmoothing(column, localX, localZ, columnIndex, features);
                resolveColumnCarving(column, localX, localZ, columnIndex, features);
                resolveColumnVeinRange(column, columnIndex);

                int top = resolveColumnTop(column, columnIndex, TideUtility.BAND_MAX_Y);

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

    // Steep faces bare their rock; sand only lies under water, all the high tide floods included, and on gentle
    // ground just above it. Whichever ground the top takes, it takes the coverage its biome lays over that ground
    private void resolveColumnDressing(
            TerrainColumnAsyncContainer column,
            long seed,
            long worldX, long worldZ,
            int localX, int localZ,
            int columnIndex,
            int macroCorner,
            TerrainFeatureStruct features) {

        int groundHeight = column.groundHeightBlocks[columnIndex];
        float slope = computeColumnSlope(column.cornerHeightBlocks, localX, localZ);
        boolean oceanWater = column.columnOceanWater[columnIndex];

        int waterLevel = TerrainFeatureStruct.LAKE_LEVEL_UNDEFINED;

        if (oceanWater)
            waterLevel = TideUtility.BAND_MAX_Y;
        else if (features.hasLake() && features.getLakeWeight() >= EngineSetting.LAKE_RIM_FULL_WEIGHT)
            waterLevel = features.getLakeLevelBlocks();

        boolean submerged = (oceanWater && groundHeight < TideUtility.BAND_MAX_Y)
                || column.columnLakeLevelBlocks[columnIndex] != TerrainFeatureStruct.LAKE_LEVEL_UNDEFINED;
        boolean shore = waterLevel != TerrainFeatureStruct.LAKE_LEVEL_UNDEFINED
                && groundHeight <= waterLevel + EngineSetting.TERRAIN_BEACH_HEIGHT_RANGE_BLOCKS
                && slope <= EngineSetting.TERRAIN_BEACH_MAX_SLOPE;

        TerrainSurfaceProfileStruct waterProfile = column.macroProfile[macroCorner];
        TerrainSurfaceProfileStruct inlandProfile = column.macroInlandProfile[macroCorner];
        TerrainSurfaceProfileStruct profile = submerged || shore ? waterProfile : inlandProfile;

        if (slope >= profile.rockSlope) {
            column.columnTopBlockID[columnIndex] = profile.rockBlockID;
            column.columnFillBlockID[columnIndex] = profile.rockBlockID;
            column.columnTopCoverage[columnIndex] = resolveColumnCoverage(
                    profile.rockCoverage, profile.rockCoverageVariance, seed, worldX, worldZ);
        } else if (submerged || shore) {
            column.columnTopBlockID[columnIndex] = waterProfile.underwaterBlockID;
            column.columnFillBlockID[columnIndex] = waterProfile.underwaterBlockID;
            column.columnTopCoverage[columnIndex] = resolveColumnCoverage(
                    waterProfile.underwaterCoverage, waterProfile.underwaterCoverageVariance, seed, worldX, worldZ);
        } else {
            column.columnTopBlockID[columnIndex] = inlandProfile.surfaceBlockID;
            column.columnFillBlockID[columnIndex] = inlandProfile.subsurfaceBlockID;
            column.columnTopCoverage[columnIndex] = resolveColumnCoverage(
                    inlandProfile.surfaceCoverage, inlandProfile.surfaceCoverageVariance, seed, worldX, worldZ);
        }

        column.columnRockBlockID[columnIndex] = profile.rockBlockID;
        column.columnProfile[columnIndex] = profile;
    }

    // The biome's coverage for a column, fallen short of its full level by up to its variance, drawn per column
    private short resolveColumnCoverage(short coverage, int variance, long seed, long worldX, long worldZ) {

        if (variance == 0 || !CoverageUtility.isCovered(coverage))
            return coverage;

        float roll = BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(
                seed ^ EngineSetting.BIOME_COVERING_VARIANCE_SALT, (int) worldX, (int) worldZ));

        return CoverageUtility.addLevels(coverage, -(int) (roll * (variance + 1)));
    }

    // Rise per block across the column, from the four corners around it
    private float computeColumnSlope(float[] cornerHeights, int localX, int localZ) {

        int cornersPerAxis = TerrainColumnAsyncContainer.CORNERS_PER_AXIS;
        int corner00 = localZ * cornersPerAxis + localX;
        int corner01 = corner00 + cornersPerAxis;

        float height00 = cornerHeights[corner00];
        float height10 = cornerHeights[corner00 + 1];
        float height01 = cornerHeights[corner01];
        float height11 = cornerHeights[corner01 + 1];

        float slopeX = ((height10 + height11) - (height00 + height01)) * 0.5f;
        float slopeZ = ((height01 + height11) - (height00 + height10)) * 0.5f;

        return (float) Math.sqrt(slopeX * slopeX + slopeZ * slopeZ);
    }

    private void resolveColumnSmoothing(
            TerrainColumnAsyncContainer column,
            int localX, int localZ,
            int columnIndex,
            TerrainFeatureStruct features) {

        int groundHeight = column.groundHeightBlocks[columnIndex];
        int groundMask = SubBlockUtility.MASK_FULL;
        int capMask = SubBlockUtility.MASK_EMPTY;

        boolean besideStillWater = features.hasLake()
                && groundHeight <= features.getLakeLevelBlocks() + EngineSetting.LAKE_RIM_BLOCKS;

        if (groundHeight > TideUtility.BAND_MAX_Y && !besideStillWater) {

            int cornersPerAxis = TerrainColumnAsyncContainer.CORNERS_PER_AXIS;
            float threshold = EngineSetting.SUB_BLOCK_SMOOTHING_THRESHOLD_BLOCKS;

            for (int quadrantZ = 0; quadrantZ < SubBlockUtility.DIVISIONS; quadrantZ++) {
                for (int quadrantX = 0; quadrantX < SubBlockUtility.DIVISIONS; quadrantX++) {

                    int cornerIndex = (localZ + quadrantZ) * cornersPerAxis + (localX + quadrantX);
                    float rise = column.cornerHeightBlocks[cornerIndex] - groundHeight;

                    if (rise >= threshold)
                        capMask |= SubBlockUtility.getOctantBit(
                                SubBlockUtility.getOctant(quadrantX, 0, quadrantZ));
                    else if (-rise >= threshold)
                        groundMask &= ~SubBlockUtility.getOctantBit(
                                SubBlockUtility.getOctant(quadrantX, 1, quadrantZ));
                }
            }
        }

        column.columnGroundMask[columnIndex] = (byte) groundMask;
        column.columnCapMask[columnIndex] = (byte) capMask;
    }

    // Caves keep a roof under any water, including ground the high tide covers, and stay below the deepest bed still
    // water can have wherever it is near; the sea floods caves near it and walls off the rest up to its high tide
    private void resolveColumnCarving(
            TerrainColumnAsyncContainer column,
            int localX, int localZ,
            int columnIndex,
            TerrainFeatureStruct features) {

        int groundHeight = column.groundHeightBlocks[columnIndex];
        boolean oceanWater = column.columnOceanWater[columnIndex];
        boolean oceanFlooded = oceanWater && groundHeight < seaLevel;
        boolean lakeWater = column.columnLakeLevelBlocks[columnIndex] != TerrainFeatureStruct.LAKE_LEVEL_UNDEFINED;
        float coastalWeight = features.getCoastalWeight();

        byte seaZone = TerrainColumnAsyncContainer.SEA_ZONE_NONE;

        if (coastalWeight >= EngineSetting.CAVE_SEA_FLOOD_WEIGHT)
            seaZone = TerrainColumnAsyncContainer.SEA_ZONE_FLOOD;
        else if (coastalWeight >= EngineSetting.CAVE_SEA_BARRIER_WEIGHT)
            seaZone = TerrainColumnAsyncContainer.SEA_ZONE_BARRIER;

        boolean waterCovered = oceanFlooded || lakeWater
                || (seaZone == TerrainColumnAsyncContainer.SEA_ZONE_FLOOD
                        && groundHeight < TideUtility.BAND_MAX_Y + EngineSetting.CAVE_ROOF_BLOCKS);

        int roof = EngineSetting.CAVE_ROOF_BLOCKS;

        if (waterCovered)
            roof = EngineSetting.CAVE_WATER_ROOF_BLOCKS;
        else if (features.hasCaveEntrances())
            roof = 0;

        int caveCeiling = groundHeight - roof;

        if (features.hasLake() && !oceanWater && features.getLakeWeight() >= EngineSetting.CAVE_LAKE_GUARD_WEIGHT)
            caveCeiling = Math.min(
                    caveCeiling, features.getLakeLevelBlocks() - EngineSetting.CAVE_LAKE_GUARD_DEPTH_BLOCKS);

        int caveFloor = Math.max(features.getCaveMinHeightBlocks(), groundHeight - features.getCaveMaxDepthBlocks());

        if (!features.hasCaves())
            caveCeiling = TerrainColumnAsyncContainer.NO_CARVE;

        float rawHeight = column.cornerRawHeightBlocks[localZ * TerrainColumnAsyncContainer.CORNERS_PER_AXIS + localX];
        float rawSlope = computeColumnSlope(column.cornerRawHeightBlocks, localX, localZ);
        float shoreDistance = TerrainShapeUtility.computeShoreDistanceBlocks(rawHeight, rawSlope);
        boolean seaFeatures = seaZone == TerrainColumnAsyncContainer.SEA_ZONE_FLOOD
                && shoreDistance >= 0f
                && (features.getCoastOverhangBlocks() > 0f || features.getSeaCaves() > 0f);
        int seaCeiling = seaFeatures
                ? Math.min(groundHeight - EngineSetting.CAVE_ROOF_BLOCKS, seaFeatureCeilingY)
                : TerrainColumnAsyncContainer.NO_CARVE;

        column.columnSeaZone[columnIndex] = seaZone;
        column.columnCaveFloorY[columnIndex] = caveFloor;
        column.columnCaveCeilingY[columnIndex] = caveCeiling;
        column.columnCaveTunnels[columnIndex] = features.getCaveTunnels();
        column.columnCaveCaverns[columnIndex] = features.getCaveCaverns();
        column.columnSeaCeilingY[columnIndex] = seaCeiling;
        column.columnShoreDistanceBlocks[columnIndex] = shoreDistance;
        column.columnFaceDistanceBlocks[columnIndex] = TerrainShapeUtility.computeCliffFaceDistanceBlocks(
                rawHeight, rawSlope);
        column.columnOverhangBlocks[columnIndex] = features.getCoastOverhangBlocks();
        column.columnSeaCaves[columnIndex] = features.getSeaCaves();

        if (caveFloor <= caveCeiling) {
            column.carveMinY = Math.min(column.carveMinY, caveFloor);
            column.carveMaxY = Math.max(column.carveMaxY, caveCeiling);
        }

        if (seaCeiling >= seaFeatureFloorY) {
            column.carveMinY = Math.min(column.carveMinY, seaFeatureFloorY);
            column.carveMaxY = Math.max(column.carveMaxY, seaCeiling);
            column.hasSeaFeatures = true;
        }
    }

    private void resolveColumnVeinRange(TerrainColumnAsyncContainer column, int columnIndex) {

        int groundHeight = column.groundHeightBlocks[columnIndex];
        BiomeVeinStruct[] veins = column.columnProfile[columnIndex].veins;

        for (int i = 0; i < veins.length; i++) {

            BiomeVeinStruct vein = veins[i];
            int fromY = Math.max(vein.getMinHeightBlocks(), groundHeight - vein.getMaxDepthBlocks());
            int toY = Math.min(vein.getMaxHeightBlocks(), groundHeight);

            if (fromY > toY)
                continue;

            column.veinMinY = Math.min(column.veinMinY, fromY);
            column.veinMaxY = Math.max(column.veinMaxY, toY);
        }
    }

    // Every distinct vein the chunk's columns carry, in first-seen order, up to the palette's size
    private void resolveVeinPalette(TerrainColumnAsyncContainer column) {

        column.veinCount = 0;

        for (int columnIndex = 0; columnIndex < TerrainColumnAsyncContainer.COLUMN_COUNT; columnIndex++) {

            BiomeVeinStruct[] veins = column.columnProfile[columnIndex].veins;

            for (int i = 0; i < veins.length && column.veinCount < TerrainColumnAsyncContainer.VEIN_PALETTE_MAX; i++)
                if (findVeinSlot(column, veins[i].getFieldSeed()) == EngineSetting.INDEX_NOT_FOUND)
                    column.veinSeeds[column.veinCount++] = veins[i].getFieldSeed();
        }
    }

    private int findVeinSlot(TerrainColumnAsyncContainer column, long fieldSeed) {

        for (int slot = 0; slot < column.veinCount; slot++)
            if (column.veinSeeds[slot] == fieldSeed)
                return slot;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // The ocean re-levels from just above its floor; a flooded cave from its lowest hollow cell in the tide band
    private void resolveTideFloors(
            TerrainColumnAsyncContainer column,
            long seed,
            long worldOffsetX, long worldOffsetZ,
            double worldWidthBlocks, double worldHeightBlocks) {

        int bandMinY = TideUtility.BAND_MIN_Y;
        int bandMaxY = TideUtility.BAND_MAX_Y;
        int latticeOriginY = TerrainCarveUtility.alignDown(bandMinY);
        boolean carvesBand = column.carveMinY <= bandMaxY && column.carveMaxY >= bandMinY;
        boolean hasTidalColumns = false;

        if (carvesBand)
            TerrainCarveUtility.fillCaveLattice(
                    seed, worldWidthBlocks, worldHeightBlocks,
                    worldOffsetX, latticeOriginY, worldOffsetZ,
                    TerrainColumnAsyncContainer.TIDE_LATTICE_ROWS, column.hasSeaFeatures, column.caveLattice);

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {

                int columnIndex = localZ * CHUNK_SIZE + localX;
                int groundHeight = column.groundHeightBlocks[columnIndex];
                int tideFloor = TerrainColumnAsyncContainer.NO_TIDE;

                if (column.columnOceanWater[columnIndex])
                    tideFloor = Math.max(groundHeight + 1, bandMinY);

                if (carvesBand && column.columnSeaZone[columnIndex] == TerrainColumnAsyncContainer.SEA_ZONE_FLOOD) {

                    int topY = Math.min(bandMaxY, groundHeight);

                    for (int worldY = bandMinY; worldY <= topY && worldY < tideFloor; worldY++) {
                        if (isCarved(column, columnIndex, localX, localZ, worldY, latticeOriginY)) {
                            tideFloor = worldY;
                            break;
                        }
                    }
                }

                column.columnTideFloorY[columnIndex] = tideFloor;

                if (tideFloor <= bandMaxY)
                    hasTidalColumns = true;
            }
        }

        column.hasTidalColumns = hasTidalColumns;
    }

    private int resolveColumnTop(TerrainColumnAsyncContainer column, int columnIndex, int topWaterY) {

        int groundHeight = column.groundHeightBlocks[columnIndex];
        int lakeLevel = column.columnLakeLevelBlocks[columnIndex];
        int top = column.columnOceanWater[columnIndex] ? Math.max(groundHeight, topWaterY) : groundHeight;

        if (lakeLevel != TerrainFeatureStruct.LAKE_LEVEL_UNDEFINED)
            top = Math.max(top, lakeLevel);

        if (readMask(column.columnCapMask, columnIndex) != SubBlockUtility.MASK_EMPTY)
            top = Math.max(top, groundHeight + 1);

        return top;
    }

    private int readMask(byte[] masks, int columnIndex) {
        return masks[columnIndex] & SubBlockUtility.MASK_FULL;
    }

    private void applyCachedColumn(
            TerrainColumnAsyncContainer column,
            WorldHandle worldHandle,
            long chunkCoordinate,
            GenerationCacheStruct terrainCache) {

        terrainCache.applyTo(column);

        column.computedWorldHandle = worldHandle;
        column.computedChunkCoordinate = chunkCoordinate;
        column.hasComputedColumn = true;
    }

    // Carving \\

    // latticeOriginY is the world Y of the cave lattice's first row, which must cover worldY
    private boolean isCarved(
            TerrainColumnAsyncContainer column,
            int columnIndex,
            int localX, int localZ,
            int worldY,
            int latticeOriginY) {

        boolean carved = false;
        int caveFloor = column.columnCaveFloorY[columnIndex];

        if (worldY >= caveFloor && worldY <= column.columnCaveCeilingY[columnIndex]) {

            int localY = worldY - latticeOriginY;
            float fade = TerrainCarveUtility.computeFloorFade(worldY, caveFloor);

            carved = TerrainCarveUtility.isTunnel(
                    sampleCaveLattice(column, TerrainCarveUtility.CHANNEL_TUNNEL_A, localX, localY, localZ),
                    sampleCaveLattice(column, TerrainCarveUtility.CHANNEL_TUNNEL_B, localX, localY, localZ),
                    column.columnCaveTunnels[columnIndex], fade)
                    || TerrainCarveUtility.isCavern(
                            sampleCaveLattice(column, TerrainCarveUtility.CHANNEL_CAVERN, localX, localY, localZ),
                            column.columnCaveCaverns[columnIndex], fade);
        }

        if (!carved && worldY >= seaFeatureFloorY && worldY <= column.columnSeaCeilingY[columnIndex]) {

            carved = TerrainCarveUtility.isNotch(
                    worldY,
                    column.columnFaceDistanceBlocks[columnIndex],
                    column.columnOverhangBlocks[columnIndex])
                    || TerrainCarveUtility.isSeaCave(
                            sampleCaveLattice(
                                    column, TerrainCarveUtility.CHANNEL_SEA,
                                    localX, worldY - latticeOriginY, localZ),
                            column.columnSeaCaves[columnIndex], worldY,
                            column.columnShoreDistanceBlocks[columnIndex]);
        }

        return carved && !(worldY <= TideUtility.BAND_MAX_Y
                && column.columnSeaZone[columnIndex] == TerrainColumnAsyncContainer.SEA_ZONE_BARRIER);
    }

    private float sampleCaveLattice(
            TerrainColumnAsyncContainer column,
            int channel,
            int localX, int localY, int localZ) {
        return TerrainCarveUtility.sampleLattice(
                column.caveLattice, TerrainCarveUtility.CAVE_CHANNELS, channel, localX, localY, localZ);
    }

    // Maps the column's veins onto the chunk's palette slots, false when it carries none
    private boolean resolveVeinSlots(TerrainColumnAsyncContainer column, int columnIndex) {

        BiomeVeinStruct[] veins = column.columnProfile[columnIndex].veins;

        for (int i = 0; i < veins.length; i++)
            column.veinSlots[i] = findVeinSlot(column, veins[i].getFieldSeed());

        return veins.length > 0;
    }

    private short resolveVein(
            TerrainColumnAsyncContainer column,
            int columnIndex,
            int localX, int localZ,
            int worldY,
            int latticeOriginY,
            short rockBlockID) {

        TerrainSurfaceProfileStruct profile = column.columnProfile[columnIndex];
        int groundHeight = column.groundHeightBlocks[columnIndex];
        int channels = column.veinCount * TerrainCarveUtility.VEIN_CHANNELS_PER_VEIN;
        int localY = worldY - latticeOriginY;

        for (int i = 0; i < profile.veins.length; i++) {

            BiomeVeinStruct vein = profile.veins[i];
            int slot = column.veinSlots[i];

            if (slot == EngineSetting.INDEX_NOT_FOUND
                    || worldY < vein.getMinHeightBlocks()
                    || worldY > vein.getMaxHeightBlocks()
                    || worldY < groundHeight - vein.getMaxDepthBlocks())
                continue;

            int channel = slot * TerrainCarveUtility.VEIN_CHANNELS_PER_VEIN;

            float gate = TerrainCarveUtility.sampleLattice(
                    column.veinLattice, channels, channel + TerrainCarveUtility.CHANNEL_VEIN_GATE,
                    localX, localY, localZ);

            if (!TerrainCarveUtility.isVeinGateOpen(gate, vein))
                continue;

            float ribbon = TerrainCarveUtility.sampleLattice(
                    column.veinLattice, channels, channel + TerrainCarveUtility.CHANNEL_VEIN_RIBBON,
                    localX, localY, localZ);

            if (TerrainCarveUtility.isVeinSeam(ribbon, vein))
                return profile.veinBlockIDs[i];
        }

        return rockBlockID;
    }

    // Probe — any single block column \\

    public int probeGroundHeight(WorldHandle worldHandle, long worldX, long worldZ) {

        long wrappedX = WorldWrapUtility.wrapBlockX(worldHandle, worldX);
        long wrappedZ = WorldWrapUtility.wrapBlockZ(worldHandle, worldZ);

        TerrainColumnAsyncContainer probe = resolveProbeColumn(worldHandle, wrappedX, wrappedZ);

        return TerrainShapeUtility.finalizeGroundHeightBlocks(resolveShapedHeight(
                probe, (int) (wrappedX % CHUNK_SIZE), (int) (wrappedZ % CHUNK_SIZE)));
    }

    // Under the sea or under still water
    public boolean probeFlooded(WorldHandle worldHandle, long worldX, long worldZ) {

        long wrappedX = WorldWrapUtility.wrapBlockX(worldHandle, worldX);
        long wrappedZ = WorldWrapUtility.wrapBlockZ(worldHandle, worldZ);

        TerrainColumnAsyncContainer probe = resolveProbeColumn(worldHandle, wrappedX, wrappedZ);

        int groundHeight = TerrainShapeUtility.finalizeGroundHeightBlocks(resolveShapedHeight(
                probe, (int) (wrappedX % CHUNK_SIZE), (int) (wrappedZ % CHUNK_SIZE)));

        return (TerrainShapeUtility.isOceanReached(probe.features) && groundHeight < seaLevel)
                || TerrainShapeUtility.isLakeCovered(probe.features, groundHeight);
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

    // Surface — any single point \\

    public void sampleSurface(
            WorldHandle worldHandle,
            double worldX,
            double worldZ,
            TerrainSurfaceSampleStruct outSample) {

        BiomeBlendStruct blend = outSample.getBlend();
        TerrainFeatureStruct features = outSample.getFeatures();
        long seed = worldHandle.getSeed();
        double worldWidthBlocks = worldHandle.getWorldScale().x;
        double worldHeightBlocks = worldHandle.getWorldScale().y;

        biomeManager.sampleBiomeField(worldHandle, worldX, worldZ, blend);
        TerrainShapeUtility.resolveFeatures(seed, worldX, worldZ, worldWidthBlocks, worldHeightBlocks, blend, features);

        float macroShape = TerrainShapeUtility.computeMacroShapeBlocks(
                seed, worldX, worldZ, worldWidthBlocks, worldHeightBlocks, blend);
        float ridge = TerrainShapeUtility.computeRidgeBlocks(
                seed, worldX, worldZ, worldWidthBlocks, worldHeightBlocks, features);
        float detail = TerrainShapeUtility.computeDetailBlocks(
                seed, worldX, worldZ, worldWidthBlocks, worldHeightBlocks, features);

        float shapedHeight = TerrainShapeUtility.shapeGroundHeightBlocks(macroShape, ridge, detail, features);
        int groundHeight = TerrainShapeUtility.finalizeGroundHeightBlocks(shapedHeight);
        boolean oceanReached = TerrainShapeUtility.isOceanReached(features);
        boolean lakeWater = TerrainShapeUtility.isLakeCovered(features, groundHeight);

        outSample.groundHeightBlocks = shapedHeight;
        outSample.openWater = oceanReached && groundHeight < seaLevel;
        outSample.lakeWater = lakeWater;

        if (outSample.openWater)
            outSample.waterSurfaceBlocks = seaLevel;
        else if (lakeWater)
            outSample.waterSurfaceBlocks = features.getLakeLevelBlocks();
        else
            outSample.waterSurfaceBlocks = shapedHeight;

        boolean lakeShore = features.hasLake()
                && features.getLakeWeight() >= EngineSetting.LAKE_RIM_FULL_WEIGHT
                && groundHeight <= features.getLakeLevelBlocks() + EngineSetting.TERRAIN_BEACH_HEIGHT_RANGE_BLOCKS;

        resolveSurfaceColors(outSample, lakeWater || lakeShore || (oceanReached
                && groundHeight <= TideUtility.BAND_MAX_Y + EngineSetting.TERRAIN_BEACH_HEIGHT_RANGE_BLOCKS));
    }

    // Under water every biome reaching the point dresses it; on land only the weight each land biome holds by itself
    private void resolveSurfaceColors(TerrainSurfaceSampleStruct sample, boolean underwaterSurface) {

        BiomeBlendStruct blend = sample.getBlend();
        boolean landDressing = !underwaterSurface && blend.getOceanWeight() < 1f;

        float topRed = 0f;
        float topGreen = 0f;
        float topBlue = 0f;
        float sideRed = 0f;
        float sideGreen = 0f;
        float sideBlue = 0f;
        float weightSum = 0f;

        for (int i = 0; i < blend.getCount(); i++) {

            BiomeHandle biome = blend.getBiome(i);

            if (landDressing && biome.hasOceanWater())
                continue;

            TerrainSurfaceProfileStruct profile = resolveSurfaceProfile(biome);
            float weight = underwaterSurface ? blend.getWeight(i) : blend.getNaturalWeight(i);
            int topColor = underwaterSurface ? profile.underwaterTopColor : profile.surfaceTopColor;
            int sideColor = underwaterSurface ? profile.underwaterSideColor : profile.rockSideColor;

            topRed += PackedColorUtility.red(topColor) * weight;
            topGreen += PackedColorUtility.green(topColor) * weight;
            topBlue += PackedColorUtility.blue(topColor) * weight;
            sideRed += PackedColorUtility.red(sideColor) * weight;
            sideGreen += PackedColorUtility.green(sideColor) * weight;
            sideBlue += PackedColorUtility.blue(sideColor) * weight;
            weightSum += weight;
        }

        float inverse = weightSum > 0f ? 1f / weightSum : 0f;

        sample.topColor = PackedColorUtility.pack(topRed * inverse, topGreen * inverse, topBlue * inverse);
        sample.sideColor = PackedColorUtility.pack(sideRed * inverse, sideGreen * inverse, sideBlue * inverse);
    }

    public boolean sampleOpenWater(WorldHandle worldHandle, double worldX, double worldZ, BiomeBlendStruct outBlend) {

        TerrainColumnAsyncContainer probe = probeColumnContainer.getInstance();
        float groundHeight = sampleGroundHeight(worldHandle, worldX, worldZ, outBlend, probe.features);

        return TerrainShapeUtility.isOceanReached(probe.features) && groundHeight < seaLevel;
    }

    private float sampleGroundHeight(
            WorldHandle worldHandle,
            double worldX,
            double worldZ,
            BiomeBlendStruct outBlend,
            TerrainFeatureStruct outFeatures) {

        long seed = worldHandle.getSeed();
        double worldWidthBlocks = worldHandle.getWorldScale().x;
        double worldHeightBlocks = worldHandle.getWorldScale().y;

        biomeManager.sampleBiomeField(worldHandle, worldX, worldZ, outBlend);
        TerrainShapeUtility.resolveFeatures(
                seed, worldX, worldZ, worldWidthBlocks, worldHeightBlocks, outBlend, outFeatures);

        float macroShape = TerrainShapeUtility.computeMacroShapeBlocks(
                seed, worldX, worldZ, worldWidthBlocks, worldHeightBlocks, outBlend);

        return TerrainShapeUtility.shapeGroundHeightBlocks(macroShape, 0f, 0f, outFeatures);
    }

    // Grid Interpolation \\

    private void blendFeatures(TerrainColumnAsyncContainer column, int localX, int localZ) {

        int stride = TerrainColumnAsyncContainer.MACRO_SAMPLE_STRIDE;
        int samplesPerAxis = TerrainColumnAsyncContainer.MACRO_SAMPLES_PER_AXIS;
        int maxCell = samplesPerAxis - 2;

        int cellX = Math.min(localX / stride, maxCell);
        int cellZ = Math.min(localZ / stride, maxCell);

        float tx = (localX - cellX * stride) / (float) stride;
        float tz = (localZ - cellZ * stride) / (float) stride;

        int index00 = cellZ * samplesPerAxis + cellX;
        int index01 = index00 + samplesPerAxis;

        column.features.blendBilinear(
                column.macroFeatures[index00],
                column.macroFeatures[index00 + 1],
                column.macroFeatures[index01],
                column.macroFeatures[index01 + 1],
                tx, tz);
    }

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

        float top = v00 * (1f - tx) + v10 * tx;
        float bottom = v01 * (1f - tx) + v11 * tx;

        return top * (1f - tz) + bottom * tz;
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

    // Surface Profile \\

    private TerrainSurfaceProfileStruct resolveSurfaceProfile(BiomeHandle biomeHandle) {

        if (surfaceProfileRevision == biomeManager.getRevision()) {

            TerrainSurfaceProfileStruct profile = biomeID2SurfaceProfile.get(biomeHandle.getBiomeID());

            if (profile != null && profile.biomeHandle == biomeHandle)
                return profile;
        }

        return createSurfaceProfile(biomeHandle);
    }

    // A live biome rebuild moves the revision, so every profile is rebuilt from the biomes as they now stand
    private synchronized TerrainSurfaceProfileStruct createSurfaceProfile(BiomeHandle biomeHandle) {

        int biomeRevision = biomeManager.getRevision();

        if (surfaceProfileRevision != biomeRevision) {
            biomeID2SurfaceProfile = new Short2ObjectOpenHashMap<>();
            surfaceProfileRevision = biomeRevision;
        }

        TerrainSurfaceProfileStruct profile = biomeID2SurfaceProfile.get(biomeHandle.getBiomeID());

        if (profile != null && profile.biomeHandle == biomeHandle)
            return profile;

        short surfaceBlockID = (short) blockManager.getBlockIDFromBlockName(biomeHandle.getSurfaceBlockName());
        short underwaterBlockID = (short) blockManager.getBlockIDFromBlockName(
                biomeHandle.getUnderwaterBlockName());
        short rockBlockID = (short) blockManager.getBlockIDFromBlockName(biomeHandle.getRockBlockName());

        short surfaceCoverage = resolveProfileCoverage(
                biomeHandle, biomeHandle.getSurfaceCovering(), surfaceBlockID);
        short underwaterCoverage = resolveProfileCoverage(
                biomeHandle, biomeHandle.getUnderwaterCovering(), underwaterBlockID);
        short rockCoverage = resolveProfileCoverage(
                biomeHandle, biomeHandle.getRockCovering(), rockBlockID);

        ObjectArrayList<BiomeVeinStruct> veinList = biomeHandle.getVeins();
        BiomeVeinStruct[] veins = veinList.toArray(new BiomeVeinStruct[0]);
        short[] veinBlockIDs = new short[veins.length];

        for (int i = 0; i < veins.length; i++)
            veinBlockIDs[i] = (short) blockManager.getBlockIDFromBlockName(veins[i].getBlockName());

        profile = new TerrainSurfaceProfileStruct(
                biomeHandle,
                surfaceBlockID,
                (short) blockManager.getBlockIDFromBlockName(biomeHandle.getSubsurfaceBlockName()),
                underwaterBlockID,
                rockBlockID,
                biomeHandle.getRockSlope(),
                surfaceCoverage,
                resolveProfileVariance(biomeHandle.getSurfaceCovering()),
                underwaterCoverage,
                resolveProfileVariance(biomeHandle.getUnderwaterCovering()),
                rockCoverage,
                resolveProfileVariance(biomeHandle.getRockCovering()),
                veins,
                veinBlockIDs,
                resolveCoveredMapColor(biomeHandle, surfaceBlockID, surfaceCoverage, Direction3Vector.UP),
                resolveCoveredMapColor(biomeHandle, underwaterBlockID, underwaterCoverage, Direction3Vector.UP),
                resolveCoveredMapColor(biomeHandle, underwaterBlockID, underwaterCoverage, Direction3Vector.NORTH),
                resolveCoveredMapColor(biomeHandle, rockBlockID, rockCoverage, Direction3Vector.NORTH));

        Short2ObjectOpenHashMap<TerrainSurfaceProfileStruct> next = new Short2ObjectOpenHashMap<>(
                biomeID2SurfaceProfile);
        next.put(biomeHandle.getBiomeID(), profile);
        biomeID2SurfaceProfile = next;

        return profile;
    }

    // The coverage a biome lays over one kind of ground, NONE when it lays none there
    private short resolveProfileCoverage(BiomeHandle biomeHandle, BiomeCoveringStruct covering, short hostBlockID) {

        if (covering == null)
            return CoverageUtility.NONE;

        return coveringManager.resolveCoverage(
                covering.getCoveringName(), covering.getLevel(), hostBlockID, biomeHandle.getBiomeName());
    }

    private int resolveProfileVariance(BiomeCoveringStruct covering) {
        return covering != null ? covering.getVariance() : EngineSetting.DEFAULT_BIOME_COVERING_VARIANCE;
    }

    // A block's map color with the coverage its biome lays over it, tinted by the biome
    private int resolveCoveredMapColor(BiomeHandle biomeHandle, short blockID, short coverage, Direction3Vector face) {

        Color biomeTint = biomeHandle.getBiomeColor();

        return coveringManager.resolveCoveredColor(
                resolveMapColor(biomeHandle, blockID, face),
                coverage,
                PackedColorUtility.packUnit(biomeTint.r, biomeTint.g, biomeTint.b),
                face != Direction3Vector.UP);
    }

    // A block drawn without a texture stands in with its biome's map color, then its biome color
    private int resolveMapColor(BiomeHandle biomeHandle, short blockID, Direction3Vector face) {

        BlockHandle blockHandle = blockManager.getBlockHandleFromBlockID(blockID);

        if (blockHandle.hasMapColor())
            return blockHandle.getMapColorForFace(face);

        int biomeMapColor = biomeManager.getMapColor(biomeHandle);

        if (biomeMapColor != EngineSetting.BIOME_MAP_COLOR_UNDEFINED)
            return biomeMapColor;

        Color biomeColor = biomeHandle.getBiomeColor();

        return PackedColorUtility.packUnit(biomeColor.r, biomeColor.g, biomeColor.b);
    }

    private boolean resolveFillGeometryUniformity(TerrainColumnAsyncContainer column) {

        if (!isFullGeometry(stoneBlockId))
            return false;

        for (int i = 0; i < TerrainColumnAsyncContainer.MACRO_SAMPLE_COUNT; i++)
            if (!isFullGeometryProfile(column.macroProfile[i]) || !isFullGeometryProfile(column.macroInlandProfile[i]))
                return false;

        return true;
    }

    private boolean isFullGeometryProfile(TerrainSurfaceProfileStruct profile) {

        if (!isFullGeometry(profile.surfaceBlockID)
                || !isFullGeometry(profile.subsurfaceBlockID)
                || !isFullGeometry(profile.underwaterBlockID)
                || !isFullGeometry(profile.rockBlockID))
            return false;

        for (int i = 0; i < profile.veinBlockIDs.length; i++)
            if (!isFullGeometry(profile.veinBlockIDs[i]))
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
        int subChunkTopY = offsetY + CHUNK_SIZE - 1;

        boolean carves = offsetY <= column.carveMaxY && subChunkTopY >= column.carveMinY;
        boolean veins = column.veinCount > 0 && offsetY <= column.veinMaxY && subChunkTopY >= column.veinMinY;

        if (!carves && !veins && subChunkTopY + surfaceDepth <= column.columnMinGroundHeightBlocks) {
            subChunkInstance.markUniformFill(DynamicGeometryType.FULL, stoneBlockId);
            return true;
        }

        if (column.allOceanWater
                && offsetY > column.columnMaxGroundHeightBlocks
                && subChunkTopY < TideUtility.BAND_MIN_Y) {
            subChunkInstance.markUniformFill(DynamicGeometryType.LIQUID, waterBlockId);
            return true;
        }

        fillSubChunkLattices(worldHandle, chunkCoordinate, column, offsetY, carves, veins);

        BlockPaletteHandle blocks = subChunkInstance.getBlockPaletteHandle();

        int tideSurfaceLevels = column.tideSurfaceLevels;
        int topWaterY = TideUtility.getTopWaterY(tideSurfaceLevels);

        short uniformBlockID = airBlockId;
        boolean uniformKnown = false;
        boolean isUniform = true;
        boolean hasAirOrWater = false;

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {

                int columnIndex = localZ * CHUNK_SIZE + localX;

                int groundHeight = column.groundHeightBlocks[columnIndex];
                int lakeLevel = column.columnLakeLevelBlocks[columnIndex];
                int columnTop = resolveColumnTop(column, columnIndex, topWaterY);
                int groundMask = readMask(column.columnGroundMask, columnIndex);
                int capMask = readMask(column.columnCapMask, columnIndex);
                boolean seaFlood = column.columnSeaZone[columnIndex] == TerrainColumnAsyncContainer.SEA_ZONE_FLOOD;

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

                short topBlockID = column.columnTopBlockID[columnIndex];
                short topCoverage = column.columnTopCoverage[columnIndex];
                short fillBlockID = column.columnFillBlockID[columnIndex];
                short rockBlockID = column.columnRockBlockID[columnIndex];
                boolean columnVeins = veins && resolveVeinSlots(column, columnIndex);

                for (int localY = 0; localY < CHUNK_SIZE; localY++) {

                    int worldY = localY + offsetY;
                    int packedXYZ = Coordinate3Int.pack(localX, localY, localZ);
                    short resultBlockID;

                    if (worldY > columnTop) {
                        resultBlockID = airBlockId;
                        hasAirOrWater = true;
                    } else if (worldY == groundHeight + 1 && capMask != SubBlockUtility.MASK_EMPTY) {
                        hasAirOrWater = true;
                        isUniform = false;
                        if (carves && groundHeight >= offsetY
                                && isCarved(column, columnIndex, localX, localZ, groundHeight, offsetY)) {
                            resultBlockID = airBlockId;
                        } else {
                            resultBlockID = topBlockID;
                            subChunkInstance.setSubBlocks(packedXYZ, topBlockID, capMask);
                            subChunkInstance.setCoverage(packedXYZ, topCoverage);
                        }
                    } else if (worldY > groundHeight && lakeLevel != TerrainFeatureStruct.LAKE_LEVEL_UNDEFINED) {
                        resultBlockID = waterBlockId;
                        hasAirOrWater = true;
                        isUniform = false;
                        subChunkInstance.writeStillLiquid(packedXYZ, waterBlockId);
                    } else if (worldY > groundHeight) {
                        short level = TideUtility.getFillLevel(tideSurfaceLevels, worldY);
                        resultBlockID = waterBlockId;
                        hasAirOrWater = true;
                        subChunkInstance.writeTidalLiquid(packedXYZ, waterBlockId, level);
                        if (level < EngineSetting.LIQUID_LEVEL_MAX)
                            isUniform = false;
                    } else if (carves && isCarved(column, columnIndex, localX, localZ, worldY, offsetY)) {
                        hasAirOrWater = true;
                        resultBlockID = airBlockId;
                        if (seaFlood && worldY <= topWaterY) {
                            short level = TideUtility.getFillLevel(tideSurfaceLevels, worldY);
                            if (level > EngineSetting.LIQUID_LEVEL_EMPTY) {
                                resultBlockID = waterBlockId;
                                subChunkInstance.writeTidalLiquid(packedXYZ, waterBlockId, level);
                                if (level < EngineSetting.LIQUID_LEVEL_MAX)
                                    isUniform = false;
                            }
                        }
                    } else if (worldY == groundHeight && groundMask != SubBlockUtility.MASK_FULL) {
                        resultBlockID = topBlockID;
                        hasAirOrWater = true;
                        isUniform = false;
                        subChunkInstance.setSubBlocks(packedXYZ, topBlockID, groundMask);
                        subChunkInstance.setCoverage(packedXYZ, topCoverage);
                    } else {
                        if (worldY == groundHeight)
                            resultBlockID = topBlockID;
                        else if (worldY > groundHeight - surfaceDepth)
                            resultBlockID = fillBlockID;
                        else
                            resultBlockID = stoneBlockId;

                        if (columnVeins && (resultBlockID == stoneBlockId || resultBlockID == rockBlockID))
                            resultBlockID = resolveVein(
                                    column, columnIndex, localX, localZ, worldY, offsetY, resultBlockID);

                        blocks.setBlock(localX, localY, localZ, resultBlockID);

                        if (worldY == groundHeight && resultBlockID == topBlockID
                                && CoverageUtility.isCovered(topCoverage)) {
                            subChunkInstance.setCoverage(packedXYZ, topCoverage);
                            isUniform = false;
                        }
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

        return true;
    }

    private void fillSubChunkLattices(
            WorldHandle worldHandle,
            long chunkCoordinate,
            TerrainColumnAsyncContainer column,
            int offsetY,
            boolean carves,
            boolean veins) {

        if (!carves && !veins)
            return;

        long seed = worldHandle.getSeed();
        long worldOffsetX = (long) Coordinate2Long.unpackX(chunkCoordinate) * CHUNK_SIZE;
        long worldOffsetZ = (long) Coordinate2Long.unpackY(chunkCoordinate) * CHUNK_SIZE;
        double worldWidthBlocks = worldHandle.getWorldScale().x;
        double worldHeightBlocks = worldHandle.getWorldScale().y;

        if (carves)
            TerrainCarveUtility.fillCaveLattice(
                    seed, worldWidthBlocks, worldHeightBlocks,
                    worldOffsetX, offsetY, worldOffsetZ,
                    TerrainCarveUtility.LATTICE_SIDE, column.hasSeaFeatures, column.caveLattice);

        if (veins)
            TerrainCarveUtility.fillVeinLattice(
                    seed, worldWidthBlocks, worldHeightBlocks,
                    worldOffsetX, offsetY, worldOffsetZ,
                    TerrainCarveUtility.LATTICE_SIDE, column.veinSeeds, column.veinCount, column.veinLattice);
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

    public int getColumnTideSurfaceLevels(long chunkCoordinate) {
        return requireComputedColumn(chunkCoordinate).tideSurfaceLevels;
    }
}
