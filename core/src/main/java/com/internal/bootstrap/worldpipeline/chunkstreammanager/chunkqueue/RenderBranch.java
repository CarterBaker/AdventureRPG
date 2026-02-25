package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderManager;
import com.internal.core.engine.BranchPackage;

public class RenderBranch extends BranchPackage {

    private WorldRenderManager worldRenderSystem;
    private int renderIndex;

    @Override
    protected void get() {

        this.worldRenderSystem = get(WorldRenderManager.class);
        this.renderIndex = ChunkData.RENDER_DATA.index;
    }

    public void renderChunk(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (syncContainer.isLocked())
            return;

        if (!syncContainer.tryAcquire())
            return;

        try {
            boolean success = worldRenderSystem.addChunkInstance(chunkInstance);
            syncContainer.data[renderIndex] = success;
        } finally {
            syncContainer.release();
        }
    }
}