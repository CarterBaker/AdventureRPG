package application.bootstrap.worldpipeline.worldgenerationmanager;

import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;

public class TerrainColumnAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch buffer holding one fully-resolved chunk column —
     * the single biome that governs it, that biome's resolved surface/
     * subsurface/underwater block IDs, a coarse macro-shape sample grid, a
     * ground height for every one of its 256 block-columns, and the min/max
     * ground height across the whole column. WorldGenerationManager.
     * computeColumn() fills it once per chunk; every generateSubChunk() call
     * for that same chunk reads from it instead of re-running the terrain
     * noise stack. The min/max ground height let generateSubChunk() classify
     * a subchunk as pure sky, pure solid stone, or pure water in O(1) before
     * touching its block palette at all, reserving the per-block loop for
     * subchunks that actually straddle a surface, coastline, or cliff.
     */

    static final int COLUMN_COUNT = EngineSetting.CHUNK_SIZE * EngineSetting.CHUNK_SIZE;
    static final int MACRO_SAMPLE_STRIDE = EngineSetting.TERRAIN_MACRO_SAMPLE_STRIDE_BLOCKS;
    static final int MACRO_SAMPLES_PER_AXIS = (EngineSetting.CHUNK_SIZE / MACRO_SAMPLE_STRIDE) + 1;

    boolean hasComputedColumn;
    long computedChunkCoordinate;

    int[] groundHeightBlocks;
    float[] macroShapeGridBlocks;

    int columnTopBlocks;
    int columnMinGroundHeightBlocks;
    int columnMaxGroundHeightBlocks;

    short biomeID;
    short surfaceBlockID;
    short subsurfaceBlockID;
    short underwaterBlockID;

    @Override
    protected void create() {

        if (EngineSetting.CHUNK_SIZE % MACRO_SAMPLE_STRIDE != 0)
            throwException("EngineSetting.TERRAIN_MACRO_SAMPLE_STRIDE_BLOCKS (" + MACRO_SAMPLE_STRIDE
                    + ") must evenly divide CHUNK_SIZE (" + EngineSetting.CHUNK_SIZE + ")");

        this.groundHeightBlocks = new int[COLUMN_COUNT];
        this.macroShapeGridBlocks = new float[MACRO_SAMPLES_PER_AXIS * MACRO_SAMPLES_PER_AXIS];
        this.hasComputedColumn = false;
    }
}