package com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaData;
import com.internal.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import com.internal.core.engine.BranchPackage;

/*
 * Merges a chunk's geometry into its parent mega. The mega sync is acquired
 * first — batchAndMerge mutates megaBatchStruct internals (batchedChunks,
 * mergedCoordinates) and RENDER_DATA is cleared in the same critical section.
 *
 * Chunk BATCH_DATA is set in a separate acquire only after a confirmed
 * successful merge. If that acquire fails, the chunk retries next frame via
 * the re-merge path — safe because the geometry in batchedChunks is current.
 */
public class MegaMergeBranch extends BranchPackage {

    // Internal
    private int chunkBatchDataIndex;
    // Internal \\

    @Override
    protected void get() {
        this.chunkBatchDataIndex = ChunkData.BATCH_DATA.index;
    }

    public void mergeChunkIntoMega(ChunkInstance chunkInstance, MegaChunkInstance mega) {
        MegaDataSyncContainer megaSync = mega.getMegaDataSyncContainer();
        if (!megaSync.tryAcquire())
            return;

        boolean merged;
        try {
            merged = mega.batchAndMerge(chunkInstance);
            if (merged) {
                megaSync.data[MegaData.RENDER_DATA.index] = false;
                if (mega.isReadyToRender())
                    mega.finalizeGeometry();
            }
        } finally {
            megaSync.release();
        }

        if (!merged)
            return;

        // Only reached on confirmed merge — chunk flags its geometry as handed off.
        // If acquire fails, chunk retries next frame. Re-merge is safe.
        ChunkDataSyncContainer chunkSync = chunkInstance.getChunkDataSyncContainer();
        if (!chunkSync.tryAcquire())
            return;
        try {
            chunkSync.data[chunkBatchDataIndex] = true;
        } finally {
            chunkSync.release();
        }
    }
}