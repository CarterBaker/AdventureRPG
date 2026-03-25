package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import com.internal.core.engine.BranchPackage;
import com.internal.core.kernel.thread.ThreadHandle;

public class ItemLoadBranch extends BranchPackage {

    /*
     * Async — builds WorldItemInstances from subchunk structs into the chunk
     * palette on the WorldStreaming thread. Does not touch the renderer — that
     * is ItemRenderBranch's responsibility. Sets ITEM_DATA on success.
     */

    // Internal
    private ThreadHandle threadHandle;
    private WorldItemPlacementSystem worldItemPlacementSystem;

    // Settings
    private int itemDataIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);

        // Settings
        this.itemDataIndex = ChunkData.ITEM_DATA.index;
    }

    // Item Load \\

    public void loadItems(ChunkInstance chunkInstance) {

        long chunkCoordinate = chunkInstance.getCoordinate();
        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        executeAsync(
                threadHandle,
                () -> {
                    try {
                        syncContainer.acquire();
                        worldItemPlacementSystem.buildChunkInstances(chunkInstance, chunkCoordinate);
                        syncContainer.getData()[itemDataIndex] = true;
                    } finally {
                        syncContainer.release();
                        syncContainer.endWork(ChunkDataSyncContainer.WORK_ITEM_LOAD);
                    }
                });
    }
}