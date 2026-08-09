package application.bootstrap.worldpipeline.worldgenerationmanager;

import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;

class TerrainColumnAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch buffer holding one fully-resolved chunk column —
     * the single biome that governs it, that biome's resolved surface/
     * subsurface/underwater block IDs, and a ground height for every one of
     * its 256 block-columns. WorldGenerationManager.computeColumn() fills it
     * once per chunk; every generateSubChunk() call for that same chunk reads
     * from it instead of re-running the terrain noise stack.
     */

    static final int COLUMN_COUNT = EngineSetting.CHUNK_SIZE * EngineSetting.CHUNK_SIZE;

    boolean hasComputedColumn;
    long computedChunkCoordinate;

    int[] groundHeightBlocks;

    short biomeID;
    short surfaceBlockID;
    short subsurfaceBlockID;
    short underwaterBlockID;

    @Override
    protected void create() {
        this.groundHeightBlocks = new int[COLUMN_COUNT];
        this.hasComputedColumn = false;
    }
}