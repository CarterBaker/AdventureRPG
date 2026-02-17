package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkBatchSystem;
import com.internal.core.engine.BranchPackage;

public class BatchBranch extends BranchPackage {

    private ChunkBatchSystem chunkBatchSystem;

    @Override
    protected void get() {
        this.chunkBatchSystem = get(ChunkBatchSystem.class);
    }

    public void batchChunk(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (syncContainer.isLocked())
            return;

        if (!syncContainer.tryAcquire())
            return;

        try {
            // Just try to batch - ChunkBatchSystem will set BATCH_DATA flag
            // when mega is uploaded to GPU
            chunkBatchSystem.batchChunk(chunkInstance);

        } finally {
            syncContainer.release();
        }
    }
}