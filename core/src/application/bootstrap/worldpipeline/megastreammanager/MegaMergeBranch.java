package application.bootstrap.worldpipeline.megastreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaData;
import application.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import engine.root.BranchPackage;

public class MegaMergeBranch extends BranchPackage {

    /*
     * Merges a chunk's geometry into its parent mega. Acquires the mega sync and,
     * under that same lock, skips entirely when the mega already reflects this
     * exact chunk content version — the common case while a chunk sits waiting on
     * its mega's GPU upload budget, and the fix for what used to be a full mega
     * remerge on every redundant frame. RENDER_DATA is cleared only when a merge
     * actually changes the mega's geometry, so the mega re-uploads exactly when
     * it needs to. Chunk BATCH_DATA is never set here — that happens in
     * MegaRenderBranch after confirmed GPU upload so individual render is never
     * removed before the mega is live on GPU.
     */

    // Settings
    private int renderDataIndex;

    // Internal \\

    @Override
    protected void get() {

        // Settings
        this.renderDataIndex = MegaData.RENDER_DATA.index;
    }

    // Merge \\

    public void mergeChunkIntoMega(ChunkInstance chunkInstance, MegaChunkInstance mega) {

        MegaDataSyncContainer megaSync = mega.getMegaDataSyncContainer();

        if (!megaSync.tryAcquire())
            return;

        try {
            if (!mega.needsMerge(chunkInstance))
                return;

            boolean merged = mega.batchAndMerge(chunkInstance);

            if (merged) {
                megaSync.getData()[renderDataIndex] = false;
                if (mega.isReadyToRender())
                    mega.finalizeGeometry();
            }
        } finally {
            megaSync.release();
        }
    }
}