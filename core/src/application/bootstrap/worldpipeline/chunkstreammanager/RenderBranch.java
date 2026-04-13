package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.core.engine.BranchPackage;

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