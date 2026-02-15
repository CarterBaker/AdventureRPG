package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkBatchSystem;
import com.internal.core.engine.BranchPackage;

public class BatchBranch extends BranchPackage {

    private ChunkBatchSystem chunkBatchSystem;
    private int batchIndex;

    @Override
    protected void get() {
        this.chunkBatchSystem = get(ChunkBatchSystem.class);
        this.batchIndex = ChunkData.BATCH_DATA.index;
    }

    public void batchChunk(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (syncContainer.isLocked())
            return;

        if (!syncContainer.tryAcquire())
            return;

        try {
            boolean success = chunkBatchSystem.batchChunk(chunkInstance);
            syncContainer.data[batchIndex] = success;

        } finally {
            syncContainer.release();
        }
    }
}