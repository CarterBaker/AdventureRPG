package com.internal.bootstrap.worldpipeline.megastreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridInstance;
import com.internal.core.engine.ManagerPackage;

public class MegaStreamManager extends ManagerPackage {

    /*
     * Internal mega chunk pipeline facade. Owned and created by
     * WorldStreamManager. All operations are per-GridInstance — each grid
     * owns its own activeMegaChunks map. The mega pool is shared internally.
     */

    // Internal
    private MegaQueueManager megaQueueManager;

    // Internal \\

    @Override
    protected void create() {
        this.megaQueueManager = create(MegaQueueManager.class);
    }

    // Grid Events \\

    public void onGridRebuilt(GridInstance grid) {
        megaQueueManager.onGridRebuilt(grid);
    }

    public void onGridRemoved(GridInstance grid) {
        megaQueueManager.onGridRemoved(grid);
    }

    // Accessible \\

    public void batchChunk(ChunkInstance chunkInstance, GridInstance grid) {
        megaQueueManager.batchChunk(chunkInstance, grid);
    }

    public void invalidateMegaForChunk(long chunkCoordinate) {
        megaQueueManager.invalidateMegaForChunk(chunkCoordinate);
    }
}