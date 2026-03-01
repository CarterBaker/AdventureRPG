package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.BranchPackage;

/*
 * Registers a chunk with the render system once its geometry is ready.
 * Sets RENDER_DATA on success.
 */
public class RenderBranch extends BranchPackage {

    // Internal
    private WorldRenderManager worldRenderSystem;
    private int renderIndex;
    // Internal \\

    @Override
    protected void get() {
        this.worldRenderSystem = get(WorldRenderManager.class);
        this.renderIndex = ChunkData.RENDER_DATA.index;
    }

    // Chunk Render \\

    public void renderChunk(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return;

        try {
            syncContainer.data[renderIndex] = worldRenderSystem.addChunkInstance(chunkInstance);
        } finally {
            syncContainer.release();
        }
    }
}