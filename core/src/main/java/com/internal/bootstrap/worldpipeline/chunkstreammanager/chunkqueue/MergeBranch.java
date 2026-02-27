package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.core.engine.BranchPackage;
import com.internal.core.kernel.threadmanager.SyncStructConsumer;
import com.internal.core.kernel.threadmanager.ThreadHandle;

public class MergeBranch extends BranchPackage {

    // Internal
    private ThreadHandle threadHandle;
    private int mergeIndex;

    // Internal \\

    @Override
    protected void get() {
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.mergeIndex = ChunkData.MERGE_DATA.index;
    }

    // Chunk Merge \\

    public void mergeChunk(ChunkInstance chunkInstance) {
        executeAsync(
                threadHandle,
                chunkInstance.getChunkDataSyncContainer(),
                (SyncStructConsumer<ChunkDataSyncContainer>) container -> {
                    boolean success = chunkInstance.merge();
                    container.data[mergeIndex] = success;
                });
    }
}