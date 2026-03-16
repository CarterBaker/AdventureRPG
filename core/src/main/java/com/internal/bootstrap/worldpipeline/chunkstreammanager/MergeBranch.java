package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.core.engine.BranchPackage;
import com.internal.core.kernel.syncconsumer.SyncStructConsumer;
import com.internal.core.kernel.thread.ThreadHandle;

public class MergeBranch extends BranchPackage {

    /*
     * Async — merges all subchunk geometry packets into the chunk's single
     * geometry packet on the WorldStreaming thread. Sets MERGE_DATA on success.
     */

    // Internal
    private ThreadHandle threadHandle;

    // Settings
    private int mergeIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");

        // Settings
        this.mergeIndex = ChunkData.MERGE_DATA.index;
    }

    // Merge \\

    public void mergeChunk(ChunkInstance chunkInstance) {
        executeAsync(
                threadHandle,
                chunkInstance.getChunkDataSyncContainer(),
                (SyncStructConsumer<ChunkDataSyncContainer>) container -> {
                    boolean success = chunkInstance.merge();
                    container.getData()[mergeIndex] = success;
                });
    }
}