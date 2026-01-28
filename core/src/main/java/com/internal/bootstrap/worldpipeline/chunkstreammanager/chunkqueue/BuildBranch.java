package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkState;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.ThreadHandle;

public class BuildBranch extends BranchPackage {

    // Internal
    private ThreadHandle threadHandle;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;

    // internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.dynamicGeometryAsyncContainer = dynamicGeometryManager.getDynamicGeometryAsyncInstance();
    }

    // Chunk Generation \\

    public void buildChunk(ChunkInstance chunkInstance) {

        if (!chunkInstance.tryBeginOperation(QueueOperation.BUILD))
            return;

        // Submit to generation thread
        executeAsync(threadHandle, dynamicGeometryAsyncContainer,
                (DynamicGeometryAsyncContainer threadSpecificReferenceContainer) -> {

                    if (dynamicGeometryManager.build(
                            threadSpecificReferenceContainer,
                            chunkInstance))
                        chunkInstance.setChunkState(ChunkState.HAS_GEOMETRY_DATA);

                    else
                        chunkInstance.setChunkState(ChunkState.NEEDS_GEOMETRY_DATA);
                });
    }
}
