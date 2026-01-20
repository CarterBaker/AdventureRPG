package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.ThreadHandle;

public class MergeBranch extends BranchPackage {

    // Internal
    private ThreadHandle threadHandle;

    // internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
    }

    // Chunk Merge \\

    public void mergeChunk(ChunkInstance chunkInstance) {

        // Submit to generation thread
        executeAsync(threadHandle, () -> {

            DynamicPacketInstance dynamicPacketInstance = chunkInstance.getDynamicPacketInstance();
            SubChunkInstance[] subChunkInstances = chunkInstance.getSubChunks();

            for (SubChunkInstance subChunkInstance : subChunkInstances)
                dynamicPacketInstance.merge(subChunkInstance.getDynamicPacketInstance());
        });
    }
}
