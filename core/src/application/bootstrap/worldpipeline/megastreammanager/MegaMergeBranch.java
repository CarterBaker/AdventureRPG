package application.bootstrap.worldpipeline.megastreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaData;
import application.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import application.core.engine.BranchPackage;

public class MegaMergeBranch extends BranchPackage {

    /*
     * Merges a chunk's geometry into its parent mega. Acquires the mega sync and
     * performs the full merge in one critical section. RENDER_DATA is cleared so
     * the mega re-uploads after any geometry change. Chunk BATCH_DATA is never set
     * here — that happens in MegaRenderBranch after confirmed GPU upload so
     * individual render is never removed before the mega is live on GPU.
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