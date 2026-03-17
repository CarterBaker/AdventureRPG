package com.internal.bootstrap.worldpipeline.megastreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaData;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MegaRenderBranch extends BranchPackage {

    /*
     * Uploads merged mega geometry to the GPU then clears the CPU-side buffer.
     * Sets BATCH_DATA on all batched chunks only after confirmed GPU upload —
     * this is the signal that individual chunk RENDER_DATA is safe to dump.
     * Setting BATCH_DATA here rather than at merge time closes the gap between
     * merge and GPU upload where neither individual nor mega render would be
     * active.
     */

    // Internal
    private WorldRenderManager worldRenderSystem;

    // Settings
    private int renderDataIndex;
    private int chunkBatchDataIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.worldRenderSystem = get(WorldRenderManager.class);

        // Settings
        this.renderDataIndex = MegaData.RENDER_DATA.index;
        this.chunkBatchDataIndex = ChunkData.BATCH_DATA.index;
    }

    // Render \\

    public void renderMega(MegaChunkInstance mega, MegaDataSyncContainer sync) {

        if (!sync.tryAcquire())
            return;

        try {
            if (!worldRenderSystem.addMegaInstance(mega))
                return;

            mega.getDynamicPacketInstance().clear();
            sync.getData()[renderDataIndex] = true;
        } finally {
            sync.release();
        }

        // Mega confirmed on GPU — now safe to remove individual chunk renders
        ObjectArrayList<ChunkInstance> list = mega.getBatchedChunkList();
        Object[] elements = list.elements();
        int size = list.size();

        for (int i = 0; i < size; i++) {

            ChunkInstance chunk = (ChunkInstance) elements[i];
            ChunkDataSyncContainer chunkSync = chunk.getChunkDataSyncContainer();

            if (!chunkSync.tryAcquire())
                continue;

            try {
                chunkSync.getData()[chunkBatchDataIndex] = true;
            } finally {
                chunkSync.release();
            }
        }
    }
}