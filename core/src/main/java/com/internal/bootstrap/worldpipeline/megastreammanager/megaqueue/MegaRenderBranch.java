package com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue;

import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaData;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.BranchPackage;

/*
 * Uploads merged mega geometry to the GPU then immediately clears the
 * CPU-side geometry buffer — there is no reason to hold it after upload.
 * RENDER_DATA is set on success.
 */
public class MegaRenderBranch extends BranchPackage {

    // Internal
    private WorldRenderManager worldRenderSystem;
    private int renderIndex;
    // Internal \\

    @Override
    protected void get() {
        this.worldRenderSystem = get(WorldRenderManager.class);
        this.renderIndex = MegaData.RENDER_DATA.index;
    }

    public void renderMega(MegaChunkInstance mega, MegaDataSyncContainer sync) {
        if (!sync.tryAcquire())
            return;
        try {
            if (!worldRenderSystem.addMegaInstance(mega))
                return;
            mega.getDynamicPacketInstance().clear();
            sync.data[renderIndex] = true;
        } finally {
            sync.release();
        }
    }
}