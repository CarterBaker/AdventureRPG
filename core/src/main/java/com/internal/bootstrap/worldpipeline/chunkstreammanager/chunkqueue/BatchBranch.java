package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkState;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkBatchSystem;
import com.internal.core.engine.BranchPackage;

public class BatchBranch extends BranchPackage {
    private ChunkBatchSystem chunkBatchSystem;

    @Override
    protected void get() {
        this.chunkBatchSystem = get(ChunkBatchSystem.class);
    }

    public void batchChunk(ChunkInstance chunkInstance) {

        if (!chunkInstance.tryBeginOperation(QueueOperation.BATCH))
            return;

        if (chunkBatchSystem.batchChunk(chunkInstance))
            chunkInstance.setChunkState(ChunkState.HAS_BATCH_DATA);
        else
            chunkInstance.setChunkState(ChunkState.NEEDS_BATCH_DATA);
    }
}