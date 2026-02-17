package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.core.engine.BranchPackage;

public class RenderBranch extends BranchPackage {

    private WorldRenderSystem worldRenderSystem;
    private int renderIndex;

    @Override
    protected void get() {
        this.worldRenderSystem = get(WorldRenderSystem.class);
        this.renderIndex = ChunkData.RENDER_DATA.index;
    }

    public void renderChunk(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (syncContainer.isLocked())
            return;

        if (!syncContainer.tryAcquire())
            return;

        try {
            // Upload individual chunk geometry to GPU
            boolean success = worldRenderSystem.renderWorldInstance(chunkInstance);
            syncContainer.data[renderIndex] = success;

        } finally {
            syncContainer.release();
        }
    }
}