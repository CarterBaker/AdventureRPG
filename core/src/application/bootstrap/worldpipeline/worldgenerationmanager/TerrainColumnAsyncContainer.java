package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;

public class TerrainColumnAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch holding one fully-resolved chunk column. The macro
     * grid is the unit of biome work: the biome field is evaluated once per
     * macro grid point at that point's true world position — never once per
     * chunk — and everything that depends on biome is derived there, so a
     * grid point shared with the neighboring chunk resolves identically from
     * either side. Shape, detail amplitude and wavelength, ocean share, and
     * the dominant biome's dressing blocks are all carried per grid point and
     * interpolated down to the 256 block columns, which is what lets a single
     * chunk hold both sides of a coastline or a biome border without a step
     * anywhere in it. WorldGenerationManager.computeColumn() fills this once
     * per chunk; every generateSubChunk() call for that chunk reads from it
     * instead of re-running the terrain noise stack.
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

    boolean hasComputedColumn;
    long computedChunkCoordinate;

    // Macro Grid — one biome field evaluation each
    BiomeBlendStruct[] macroBlend;
    float[] macroShapeGridBlocks;
    float[] macroDetailAmplitudeGrid;
    float[] macroDetailWavelengthGrid;
    float[] macroOceanWeightGrid;
    short[] macroBiomeIDGrid;
    short[] macroSurfaceBlockIDGrid;
    short[] macroSubsurfaceBlockIDGrid;
    short[] macroUnderwaterBlockIDGrid;

    // Detail Grid
    float[] detailGridBlocks;

    // Per Block Column
    int[] groundHeightBlocks;
    short[] columnSurfaceBlockID;
    short[] columnSubsurfaceBlockID;
    short[] columnUnderwaterBlockID;
    boolean[] columnOceanWater;

    // Whole Column
    int columnTopBlocks;
    int columnMinGroundHeightBlocks;
    int columnMaxGroundHeightBlocks;

    short biomeID;
    boolean allOceanWater;
    boolean allFillBlocksFullGeometry;

    @Override
    protected void create() {

        this.macroBlend = new BiomeBlendStruct[MACRO_SAMPLE_COUNT];

        for (int i = 0; i < MACRO_SAMPLE_COUNT; i++)
            this.macroBlend[i] = new BiomeBlendStruct();

        this.macroShapeGridBlocks = new float[MACRO_SAMPLE_COUNT];
        this.macroDetailAmplitudeGrid = new float[MACRO_SAMPLE_COUNT];
        this.macroDetailWavelengthGrid = new float[MACRO_SAMPLE_COUNT];
        this.macroOceanWeightGrid = new float[MACRO_SAMPLE_COUNT];
        this.macroBiomeIDGrid = new short[MACRO_SAMPLE_COUNT];
        this.macroSurfaceBlockIDGrid = new short[MACRO_SAMPLE_COUNT];
        this.macroSubsurfaceBlockIDGrid = new short[MACRO_SAMPLE_COUNT];
        this.macroUnderwaterBlockIDGrid = new short[MACRO_SAMPLE_COUNT];

        this.detailGridBlocks = new float[DETAIL_SAMPLE_COUNT];

        this.groundHeightBlocks = new int[COLUMN_COUNT];
        this.columnSurfaceBlockID = new short[COLUMN_COUNT];
        this.columnSubsurfaceBlockID = new short[COLUMN_COUNT];
        this.columnUnderwaterBlockID = new short[COLUMN_COUNT];
        this.columnOceanWater = new boolean[COLUMN_COUNT];

        this.hasComputedColumn = false;
    }
}