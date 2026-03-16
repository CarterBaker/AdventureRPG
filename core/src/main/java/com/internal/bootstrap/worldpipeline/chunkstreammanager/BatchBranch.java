package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.megastreammanager.MegaStreamManager;
import com.internal.core.engine.BranchPackage;

/*
 * Clears BATCH_DATA on the chunk before forwarding to MegaStreamManager.
 * This ensures the flag is false during the contribution window and is only
 * set once the mega has successfully consumed the chunk's geometry.
 * Acts as the re-entry point for both fresh chunks and invalidated ones.
 */
public class BatchBranch extends BranchPackage {

    // Internal
    private MegaStreamManager megaStreamManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.megaStreamManager = get(MegaStreamManager.class);
    }

    // Chunk Batch \\

    public void batchChunk(ChunkInstance chunkInstance) {
        chunkInstance.getChunkDataSyncContainer().setData(ChunkData.BATCH_DATA, false);
        megaStreamManager.batchChunk(chunkInstance);
    }
}