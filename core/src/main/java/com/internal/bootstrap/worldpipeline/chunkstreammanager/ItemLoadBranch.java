package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import com.internal.core.engine.BranchPackage;
import com.internal.core.kernel.syncconsumer.SyncStructConsumer;
import com.internal.core.kernel.thread.ThreadHandle;

/*
 * Async — CPU only under the lock.
 * Builds WorldItemInstances from subchunk structs into the chunk palette.
 * Does not touch the renderer — that is ItemRenderBranch's responsibility.
 */
public class ItemLoadBranch extends BranchPackage {

    // Internal
    private ThreadHandle threadHandle;
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private int itemDataIndex;

    // Internal \\

    @Override
    protected void get() {
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
        this.itemDataIndex = ChunkData.ITEM_DATA.index;
    }

    // Item Load \\

    public void loadItems(ChunkInstance chunkInstance) {
        long chunkCoordinate = chunkInstance.getCoordinate();
        executeAsync(
                threadHandle,
                chunkInstance.getChunkDataSyncContainer(),
                (SyncStructConsumer<ChunkDataSyncContainer>) container -> {
                    worldItemPlacementSystem.buildChunkInstances(chunkInstance, chunkCoordinate);
                    container.data[itemDataIndex] = true;
                });
    }
}