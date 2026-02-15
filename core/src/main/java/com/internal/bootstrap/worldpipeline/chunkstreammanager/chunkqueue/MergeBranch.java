package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.ThreadHandle;
import com.internal.core.kernel.threadmanager.InternalThreadManager.SyncStructConsumer;

public class MergeBranch extends BranchPackage {

    // Internal
    private ThreadHandle threadHandle;
    private int mergeIndex;

    // internal \\

    @Override
    protected void get() {

        // Internal
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