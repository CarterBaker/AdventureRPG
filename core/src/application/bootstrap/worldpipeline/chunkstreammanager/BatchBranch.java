package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.megastreammanager.MegaStreamManager;
import engine.root.BranchPackage;

public class BatchBranch extends BranchPackage {

    /*
     * Clears BATCH_DATA on the chunk then forwards it to MegaStreamManager.
     * BATCH_DATA is cleared here so it is false during the contribution window
     * and only set by MegaRenderBranch after confirmed GPU upload. Acts as the
     * re-entry point for both fresh chunks and invalidated ones.
     */

    // Internal
    private MegaStreamManager megaStreamManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.megaStreamManager = get(MegaStreamManager.class);
    }

    // Batch \\

    public void batchChunk(ChunkInstance chunkInstance, GridInstance grid) {
        chunkInstance.getChunkDataSyncContainer().setData(ChunkData.BATCH_DATA, false);
        megaStreamManager.batchChunk(chunkInstance, grid);
    }
}