package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.util.TerrainCarveUtility;
import application.bootstrap.worldpipeline.util.TerrainFeatureStruct;
import application.bootstrap.worldpipeline.util.TideUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;

public class TerrainColumnAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch holding one resolved chunk column. Biome-dependent
     * values are evaluated per macro grid point and interpolated to all block
     * columns, with the tide surface and the corner height grids, shaped and
     * raw, that drive sub-block edge smoothing, slope and shore distance. Each block column carries its
     * ground, dressing, water, the band its caves and sea caves may hollow,
     * its veins and the lowest cell the tide reaches. The cave and vein
     * lattices are refilled per subchunk. Filled once per chunk by
     * computeColumn() and read by every generateSubChunk() call.
     */

    static final int COLUMN_COUNT = EngineSetting.CHUNK_SIZE * EngineSetting.CHUNK_SIZE;

    static final int MACRO_SAMPLE_STRIDE = EngineSetting.TERRAIN_MACRO_SAMPLE_STRIDE_BLOCKS;
    static final int MACRO_SAMPLES_PER_AXIS = (EngineSetting.CHUNK_SIZE / MACRO_SAMPLE_STRIDE) + 1;
    static final int MACRO_SAMPLE_COUNT = MACRO_SAMPLES_PER_AXIS * MACRO_SAMPLES_PER_AXIS;
    static final int MACRO_CENTER_INDEX = (MACRO_SAMPLES_PER_AXIS / 2) * MACRO_SAMPLES_PER_AXIS
            + (MACRO_SAMPLES_PER_AXIS / 2);

    static final int DETAIL_SAMPLE_STRIDE = EngineSetting.TERRAIN_DETAIL_SAMPLE_STRIDE_BLOCKS;
    static final int DETAIL_SAMPLES_PER_AXIS = (EngineSetting.CHUNK_SIZE / DETAIL_SAMPLE_STRIDE) + 1;
    static final int DETAIL_SAMPLE_COUNT = DETAIL_SAMPLES_PER_AXIS * DETAIL_SAMPLES_PER_AXIS;

    static final int CORNERS_PER_AXIS = EngineSetting.CHUNK_SIZE + 1;
    static final int CORNER_COUNT = CORNERS_PER_AXIS * CORNERS_PER_AXIS;

    static final int TIDE_LATTICE_ROWS = TerrainCarveUtility.computeLatticeRows(
            TideUtility.BAND_MIN_Y, TideUtility.BAND_MAX_Y);
    static final int LATTICE_ROWS = Math.max(TerrainCarveUtility.LATTICE_SIDE, TIDE_LATTICE_ROWS);
    static final int LATTICE_POINTS = LATTICE_ROWS * TerrainCarveUtility.LATTICE_SIDE
            * TerrainCarveUtility.LATTICE_SIDE;

    static final int VEIN_PALETTE_MAX = EngineSetting.TERRAIN_VEIN_PALETTE_MAX;

    static final byte SEA_ZONE_NONE = 0;
    static final byte SEA_ZONE_BARRIER = 1;
    static final byte SEA_ZONE_FLOOD = 2;

    static final int NO_TIDE = Integer.MAX_VALUE;
    static final int NO_CARVE = Integer.MIN_VALUE;

    boolean hasComputedColumn;
    WorldHandle computedWorldHandle;
    long computedChunkCoordinate;

    // Macro Grid — one biome field evaluation each
    BiomeBlendStruct[] macroBlend;
    TerrainFeatureStruct[] macroFeatures;
    float[] macroShapeGridBlocks;
    short[] macroBiomeIDGrid;
    TerrainSurfaceProfileStruct[] macroProfile;
    TerrainSurfaceProfileStruct[] macroInlandProfile;

    // Detail Grid
    float[] detailGridBlocks;
    float[] ridgeGridBlocks;

    // Corner Grid — shaped, and raw before any shaping
    float[] cornerHeightBlocks;
    float[] cornerRawHeightBlocks;
    TerrainFeatureStruct features;

    // Per Block Column
    int[] groundHeightBlocks;
    short[] columnTopBlockID;
    short[] columnFillBlockID;
    short[] columnRockBlockID;
    short[] columnTopCoverage;
    boolean[] columnOceanWater;
    int[] columnLakeLevelBlocks;
    byte[] columnGroundMask;
    byte[] columnCapMask;
    TerrainSurfaceProfileStruct[] columnProfile;

    // Per Block Column — Carving
    byte[] columnSeaZone;
    int[] columnCaveFloorY;
    int[] columnCaveCeilingY;
    float[] columnCaveTunnels;
    float[] columnCaveCaverns;
    int[] columnSeaCeilingY;
    float[] columnShoreDistanceBlocks;
    float[] columnFaceDistanceBlocks;
    float[] columnOverhangBlocks;
    float[] columnSeaCaves;
    int[] columnTideFloorY;

    // Whole Column
    int columnTopBlocks;
    int columnMinGroundHeightBlocks;
    int columnMaxGroundHeightBlocks;
    int carveMinY;
    int carveMaxY;
    boolean hasSeaFeatures;
    int veinMinY;
    int veinMaxY;

    short biomeID;
    boolean allOceanWater;
    boolean hasTidalColumns;
    boolean allFillBlocksFullGeometry;

    // Vein Palette
    long[] veinSeeds;
    int veinCount;

    // Lattices — refilled per subchunk
    float[] caveLattice;
    float[] veinLattice;
    int[] veinSlots;

    // Tide
    int tideSurfaceLevels;

    @Override
    protected void create() {

        this.macroBlend = new BiomeBlendStruct[MACRO_SAMPLE_COUNT];
        this.macroFeatures = new TerrainFeatureStruct[MACRO_SAMPLE_COUNT];

        for (int i = 0; i < MACRO_SAMPLE_COUNT; i++) {
            this.macroBlend[i] = new BiomeBlendStruct();
            this.macroFeatures[i] = new TerrainFeatureStruct();
        }

        this.macroShapeGridBlocks = new float[MACRO_SAMPLE_COUNT];
        this.macroBiomeIDGrid = new short[MACRO_SAMPLE_COUNT];
        this.macroProfile = new TerrainSurfaceProfileStruct[MACRO_SAMPLE_COUNT];
        this.macroInlandProfile = new TerrainSurfaceProfileStruct[MACRO_SAMPLE_COUNT];

        this.detailGridBlocks = new float[DETAIL_SAMPLE_COUNT];
        this.ridgeGridBlocks = new float[DETAIL_SAMPLE_COUNT];

        this.cornerHeightBlocks = new float[CORNER_COUNT];
        this.cornerRawHeightBlocks = new float[CORNER_COUNT];
        this.features = new TerrainFeatureStruct();

        this.groundHeightBlocks = new int[COLUMN_COUNT];
        this.columnTopBlockID = new short[COLUMN_COUNT];
        this.columnFillBlockID = new short[COLUMN_COUNT];
        this.columnRockBlockID = new short[COLUMN_COUNT];
        this.columnTopCoverage = new short[COLUMN_COUNT];
        this.columnOceanWater = new boolean[COLUMN_COUNT];
        this.columnLakeLevelBlocks = new int[COLUMN_COUNT];
        this.columnGroundMask = new byte[COLUMN_COUNT];
        this.columnCapMask = new byte[COLUMN_COUNT];
        this.columnProfile = new TerrainSurfaceProfileStruct[COLUMN_COUNT];

        this.columnSeaZone = new byte[COLUMN_COUNT];
        this.columnCaveFloorY = new int[COLUMN_COUNT];
        this.columnCaveCeilingY = new int[COLUMN_COUNT];
        this.columnCaveTunnels = new float[COLUMN_COUNT];
        this.columnCaveCaverns = new float[COLUMN_COUNT];
        this.columnSeaCeilingY = new int[COLUMN_COUNT];
        this.columnShoreDistanceBlocks = new float[COLUMN_COUNT];
        this.columnFaceDistanceBlocks = new float[COLUMN_COUNT];
        this.columnOverhangBlocks = new float[COLUMN_COUNT];
        this.columnSeaCaves = new float[COLUMN_COUNT];
        this.columnTideFloorY = new int[COLUMN_COUNT];

        this.veinSeeds = new long[VEIN_PALETTE_MAX];

        this.caveLattice = new float[LATTICE_POINTS * TerrainCarveUtility.CAVE_CHANNELS];
        this.veinLattice = new float[LATTICE_POINTS * VEIN_PALETTE_MAX * TerrainCarveUtility.VEIN_CHANNELS_PER_VEIN];
        this.veinSlots = new int[VEIN_PALETTE_MAX];

        this.hasComputedColumn = false;
    }
}
