package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;

public class TerrainColumnAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch holding one resolved chunk column. Biome-dependent
     * values are evaluated per macro grid point and interpolated to all block
     * columns, with the tide surface and the corner height grid that drives
     * sub-block edge smoothing. Filled once per chunk by computeColumn() and
     * read by every generateSubChunk() call.
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

    boolean hasComputedColumn;
    WorldHandle computedWorldHandle;
    long computedChunkCoordinate;

    // Macro Grid — one biome field evaluation each
    BiomeBlendStruct[] macroBlend;
    float[] macroShapeGridBlocks;
    float[] macroDetailAmplitudeGrid;
    float[] macroDetailWavelengthGrid;
    float[] macroCoastalWeightGrid;
    short[] macroBiomeIDGrid;
    short[] macroSurfaceBlockIDGrid;
    short[] macroSubsurfaceBlockIDGrid;
    short[] macroUnderwaterBlockIDGrid;

    // Detail Grid
    float[] detailGridBlocks;

    // Corner Grid
    float[] cornerHeightBlocks;

    // Per Block Column
    int[] groundHeightBlocks;
    short[] columnSurfaceBlockID;
    short[] columnSubsurfaceBlockID;
    short[] columnUnderwaterBlockID;
    boolean[] columnOceanWater;
    byte[] columnGroundMask;
    byte[] columnCapMask;

    // Whole Column
    int columnTopBlocks;
    int columnMinGroundHeightBlocks;
    int columnMaxGroundHeightBlocks;

    short biomeID;
    boolean allOceanWater;
    boolean hasTidalColumns;
    boolean allFillBlocksFullGeometry;

    // Tide
    int tideSurfaceLevels;

    @Override
    protected void create() {

        this.macroBlend = new BiomeBlendStruct[MACRO_SAMPLE_COUNT];

        for (int i = 0; i < MACRO_SAMPLE_COUNT; i++)
            this.macroBlend[i] = new BiomeBlendStruct();

        this.macroShapeGridBlocks = new float[MACRO_SAMPLE_COUNT];
        this.macroDetailAmplitudeGrid = new float[MACRO_SAMPLE_COUNT];
        this.macroDetailWavelengthGrid = new float[MACRO_SAMPLE_COUNT];
        this.macroCoastalWeightGrid = new float[MACRO_SAMPLE_COUNT];
        this.macroBiomeIDGrid = new short[MACRO_SAMPLE_COUNT];
        this.macroSurfaceBlockIDGrid = new short[MACRO_SAMPLE_COUNT];
        this.macroSubsurfaceBlockIDGrid = new short[MACRO_SAMPLE_COUNT];
        this.macroUnderwaterBlockIDGrid = new short[MACRO_SAMPLE_COUNT];

        this.detailGridBlocks = new float[DETAIL_SAMPLE_COUNT];

        this.cornerHeightBlocks = new float[CORNER_COUNT];

        this.groundHeightBlocks = new int[COLUMN_COUNT];
        this.columnSurfaceBlockID = new short[COLUMN_COUNT];
        this.columnSubsurfaceBlockID = new short[COLUMN_COUNT];
        this.columnUnderwaterBlockID = new short[COLUMN_COUNT];
        this.columnOceanWater = new boolean[COLUMN_COUNT];
        this.columnGroundMask = new byte[COLUMN_COUNT];
        this.columnCapMask = new byte[COLUMN_COUNT];

        this.hasComputedColumn = false;
    }
}