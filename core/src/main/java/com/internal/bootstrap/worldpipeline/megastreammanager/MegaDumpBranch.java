package com.internal.bootstrap.worldpipeline.megastreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.BranchPackage;

/*
 * Fires when a mega's slot transitions to IMMEDIATE — chunks are close enough
 * to render individually. Removes the mega from the GPU and clears BATCH_DATA
 * on all registered chunks so they re-contribute when the slot returns to NEAR.
 * BATCH_DATA on the mega itself is not dumpable — the chunk registry is kept
 * so re-contribution can happen without a full re-registration cycle.
 */
public class MegaDumpBranch extends BranchPackage {

    // Internal
    private WorldRenderManager worldRenderSystem;

    // Settings
    private int chunkBatchDataIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.worldRenderSystem = get(WorldRenderManager.class);

        // Settings
        this.chunkBatchDataIndex = ChunkData.BATCH_DATA.index;
    }

    // Dump \\

    public void dumpMega(MegaChunkInstance mega, MegaDataSyncContainer sync, long megaCoord) {

        if (!sync.tryAcquire())
            return;

        try {
            worldRenderSystem.removeMegaInstance(megaCoord);
            clearChunkBatchFlags(mega);
            mega.getDynamicPacketInstance().clear();
            sync.data[com.internal.bootstrap.worldpipeline.megachunk.MegaData.RENDER_DATA.index] = false;
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
                chunkSync.data[chunkBatchDataIndex] = false;
            } finally {
                chunkSync.release();
            }
        }
    }
}