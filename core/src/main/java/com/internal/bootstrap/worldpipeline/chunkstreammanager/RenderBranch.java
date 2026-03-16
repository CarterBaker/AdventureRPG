package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.BranchPackage;

public class RenderBranch extends BranchPackage {

    /*
     * Main thread — registers the chunk with the render system once its merged
     * geometry is ready. Sets RENDER_DATA on success.
     */

    // Internal
    private WorldRenderManager worldRenderSystem;

    // Settings
    private int renderIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.worldRenderSystem = get(WorldRenderManager.class);

        // Settings
        this.renderIndex = ChunkData.RENDER_DATA.index;
    }

    // Render \\

    public void renderChunk(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return;

        try {
            syncContainer.getData()[renderIndex] = worldRenderSystem.addChunkInstance(chunkInstance);
        } finally {
            syncContainer.release();
        }
    }
}