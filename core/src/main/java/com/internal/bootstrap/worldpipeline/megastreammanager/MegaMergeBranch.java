package com.internal.bootstrap.worldpipeline.megastreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaData;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import com.internal.core.engine.BranchPackage;

/*
 * Merges a chunk's geometry into its parent mega. Acquires the mega sync
 * and performs the full merge in one critical section. RENDER_DATA is cleared
 * so the mega re-uploads after any geometry change. Chunk BATCH_DATA is never
 * set here — that happens in MegaRenderBranch after confirmed GPU upload so
 * individual render is never removed before the mega is live on GPU.
 */
public class MegaMergeBranch extends BranchPackage {

    // Internal \\

    @Override
    protected void get() {
    }

    // Merge \\

    public void mergeChunkIntoMega(ChunkInstance chunkInstance, MegaChunkInstance mega) {

        MegaDataSyncContainer megaSync = mega.getMegaDataSyncContainer();

        if (!megaSync.tryAcquire())
            return;

        try {
            boolean merged = mega.batchAndMerge(chunkInstance);

            if (merged) {
                megaSync.data[MegaData.RENDER_DATA.index] = false;
                if (mega.isReadyToRender())
                    mega.finalizeGeometry();
            }
        } finally {
            megaSync.release();
        }
    }
}