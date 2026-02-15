package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
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

        executeAsync(
                threadHandle,
                dynamicGeometryAsyncContainer,
                chunkInstance.getChunkDataSyncContainer(),
                (DynamicGeometryAsyncContainer geo, ChunkDataSyncContainer data) -> {
                    if (dynamicGeometryManager.build(geo, chunkInstance))
                        data.data[ChunkData.BUILD_DATA.index] = true;
                });
    }
}
