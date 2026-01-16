package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkState;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.ThreadHandle;

public class BuildBranch extends BranchPackage {

    // Internal
    private ThreadHandle threadHandle;
    private DynamicGeometryManager dynamicGeometryManager;

    // internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
    }

    // Chunk Generation \\

    public void buildChunk(ChunkInstance chunkInstance) {

        // Submit to generation thread
        executeAsync(threadHandle, () -> {

            boolean success = true;

            // Attempt to build the chunk
            if (!dynamicGeometryManager.build(chunkInstance))
                success = false;

            if (success) // Thread-safe state update
                chunkInstance.setChunkState(ChunkState.HAS_GENERATION_DATA);

            else // Keep it in NEEDS_GENERATION_DATA to retry
                chunkInstance.setChunkState(ChunkState.NEEDS_GENERATION_DATA);
        });
    }
}
