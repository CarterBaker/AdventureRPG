package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkState;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkBatchSystem;
import com.internal.core.engine.BranchPackage;

public class BatchBranch extends BranchPackage {

    // Internal
    private ChunkBatchSystem chunkBatchSystem;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.chunkBatchSystem = get(ChunkBatchSystem.class);
    }

    // Batch Management \\

    public void batchChunk(ChunkInstance chunkInstance) {

        chunkInstance.setChunkState(ChunkState.BATCHING_DATA);

        chunkBatchSystem.batchChunk(chunkInstance);

        chunkInstance.setChunkState(ChunkState.HAS_BATCH_DATA);
    }
}
