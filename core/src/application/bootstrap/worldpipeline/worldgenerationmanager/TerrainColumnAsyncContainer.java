package application.bootstrap.worldpipeline.worldgenerationmanager;

import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;

public class TerrainColumnAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch buffer holding one fully-resolved chunk column —
     * the single biome that governs it, that biome's resolved surface/
     * subsurface/underwater block IDs, a coarse macro-shape sample grid, a
     * coarse detail sample grid, a ground height for every one of its 256
     * block-columns, and the min/max ground height across the whole column.
     * WorldGenerationManager.computeColumn() fills it once per chunk; every
     * generateSubChunk() call for that same chunk reads from it instead of
     * re-running the terrain noise stack. Both grids are sampled on a coarse
     * world-aligned stride and bilinearly interpolated per block — see
     * EngineSetting.TERRAIN_MACRO_SAMPLE_STRIDE_BLOCKS and
     * TERRAIN_DETAIL_SAMPLE_STRIDE_BLOCKS for the stride each one uses. The
     * min/max ground height let generateSubChunk() classify a subchunk as
     * pure sky, pure solid stone, or pure water in O(1) before touching its
     * block palette at all, reserving the per-block loop for subchunks that
     * actually straddle a surface, coastline, or cliff.
     */

    static final int COLUMN_COUNT = EngineSetting.CHUNK_SIZE * EngineSetting.CHUNK_SIZE;

    static final int MACRO_SAMPLE_STRIDE = EngineSetting.TERRAIN_MACRO_SAMPLE_STRIDE_BLOCKS;
    static final int MACRO_SAMPLES_PER_AXIS = (EngineSetting.CHUNK_SIZE / MACRO_SAMPLE_STRIDE) + 1;

    static final int DETAIL_SAMPLE_STRIDE = EngineSetting.TERRAIN_DETAIL_SAMPLE_STRIDE_BLOCKS;
    static final int DETAIL_SAMPLES_PER_AXIS = (EngineSetting.CHUNK_SIZE / DETAIL_SAMPLE_STRIDE) + 1;

    boolean hasComputedColumn;
    long computedChunkCoordinate;

    int[] groundHeightBlocks;
    float[] macroShapeGridBlocks;
    float[] detailGridBlocks;

    int columnTopBlocks;
    int columnMinGroundHeightBlocks;
    int columnMaxGroundHeightBlocks;

    short biomeID;
    short surfaceBlockID;
    short subsurfaceBlockID;
    short underwaterBlockID;

    @Override
    protected void create() {
        this.groundHeightBlocks = new int[COLUMN_COUNT];
        this.macroShapeGridBlocks = new float[MACRO_SAMPLES_PER_AXIS * MACRO_SAMPLES_PER_AXIS];
        this.detailGridBlocks = new float[DETAIL_SAMPLES_PER_AXIS * DETAIL_SAMPLES_PER_AXIS];
        this.hasComputedColumn = false;
    }
}