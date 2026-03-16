package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import com.internal.core.engine.BranchPackage;

/*
 * Main thread — always tryAcquire, never executeAsync.
 * Pushes the chunk's built item instances to the composite renderer.
 * Fires only after ITEM_DATA is true (instances already built by ItemLoadBranch).
 */
public class ItemRenderBranch extends BranchPackage {

    // Internal
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private int itemRenderDataIndex;

    // Internal \\

    @Override
    protected void get() {
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
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
            syncContainer.data[itemRenderDataIndex] = true;
        } finally {
            syncContainer.release();
        }
    }
}