package com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaData;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.BranchPackage;

/*
 * Fires when a mega's chunks are close enough to render individually.
 * Removes the mega from the GPU, clears BATCH_DATA and RENDER_DATA,
 * clears chunk BATCH_DATA flags, and resets the merged set so all chunks
 * re-contribute naturally when the mega next enters BATCHED range.
 */
public class MegaDumpBranch extends BranchPackage {

    // Internal
    private WorldRenderManager worldRenderSystem;
    private int batchDataIndex;
    // Internal \\

    @Override
    protected void get() {
        this.worldRenderSystem = get(WorldRenderManager.class);
        this.batchDataIndex = ChunkData.BATCH_DATA.index;
    }

    public void dumpMega(MegaChunkInstance mega, MegaDataSyncContainer sync, long megaCoord) {
        if (!sync.tryAcquire())
            return;
        try {
            worldRenderSystem.removeMegaInstance(megaCoord);
            clearChunkBatchFlags(mega);
            mega.reset();
            sync.data[MegaData.BATCH_DATA.index] = false;
            sync.data[MegaData.RENDER_DATA.index] = false;
        } finally {
            sync.release();
        }
    }

    private void clearChunkBatchFlags(MegaChunkInstance mega) {
        for (ChunkInstance chunk : mega.getBatchedChunks().values()) {
            ChunkDataSyncContainer chunkSync = chunk.getChunkDataSyncContainer();
            if (!chunkSync.tryAcquire())
                continue;
            try {
                chunkSync.data[batchDataIndex] = false;
            } finally {
                chunkSync.release();
            }
        }
    }
}