package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import com.internal.core.engine.BranchPackage;

public class ItemRenderBranch extends BranchPackage {

    /*
     * Main thread — always tryAcquire, never async. Pushes the chunk's built
     * item instances to the composite renderer. Fires only after ITEM_DATA is
     * true — instances already built by ItemLoadBranch. Sets ITEM_RENDER_DATA
     * on success.
     */

    // Internal
    private WorldItemPlacementSystem worldItemPlacementSystem;

    // Settings
    private int itemRenderDataIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);

        // Settings
        this.itemRenderDataIndex = ChunkData.ITEM_RENDER_DATA.index;
    }

    // Item Render \\

    public void renderItems(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return;

        try {
            worldItemPlacementSystem.pushChunkToRenderer(
                    chunkInstance, chunkInstance.getCoordinate());
            syncContainer.getData()[itemRenderDataIndex] = true;
        } finally {
            syncContainer.release();
        }
    }
}