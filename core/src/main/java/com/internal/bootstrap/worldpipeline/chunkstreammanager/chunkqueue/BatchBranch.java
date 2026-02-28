package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.megastreammanager.MegaStreamManager;
import com.internal.core.engine.BranchPackage;

/*
 * Forwards the chunk to MegaStreamManager for mega batch registration.
 * No lock is acquired here — MegaQueueManager owns the mega sync contract.
 */
public class BatchBranch extends BranchPackage {
    // Internal
    private MegaStreamManager megaStreamManager;
    // Internal \\

    @Override
    protected void get() {
        this.megaStreamManager = get(MegaStreamManager.class);
    }

    // Chunk Batch \\
    public void batchChunk(ChunkInstance chunkInstance) {
        megaStreamManager.batchChunk(chunkInstance);
    }
}