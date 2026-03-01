package com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue;

import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaData;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.BranchPackage;

/*
 * Fires when a mega's chunks are close enough to render individually.
 * Removes the mega from the GPU renderer and resets MERGE_DATA and
 * RENDER_DATA so the full pipeline re-runs when it becomes DISTANT again.
 * BATCH_DATA is preserved — chunks are still registered and valid.
 */
public class MegaDumpBranch extends BranchPackage {

    // Internal
    private WorldRenderManager worldRenderSystem;
    private int mergeIndex;
    private int renderIndex;
    // Internal \\

    @Override
    protected void get() {
        this.worldRenderSystem = get(WorldRenderManager.class);
        this.mergeIndex = MegaData.MERGE_DATA.index;
        this.renderIndex = MegaData.RENDER_DATA.index;
    }

    public void dumpMega(MegaChunkInstance mega, MegaDataSyncContainer sync, long megaCoord) {
        if (!sync.tryAcquire())
            return;
        try {
            worldRenderSystem.removeMegaInstance(megaCoord);
            sync.data[mergeIndex] = false;
            sync.data[renderIndex] = false;
        } finally {
            sync.release();
        }
    }
}