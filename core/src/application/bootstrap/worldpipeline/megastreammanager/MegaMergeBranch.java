package application.bootstrap.worldpipeline.megastreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunk.ChunkLockBatchPoolStruct;
import application.bootstrap.worldpipeline.chunk.ChunkLockBatchStruct;
import application.bootstrap.worldpipeline.chunk.ChunkLockResult;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaData;
import application.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MegaMergeBranch extends BranchPackage {

    /*
     * Merges a chunk's geometry into its parent mega. A first contribution is
     * appended under the caller's lock; a repeat contribution rebuilds the
     * whole mega from every member, taking all their locks first through a
     * ChunkLockBatchStruct. A busy lock aborts the re-merge until a later pass.
     */

    // Settings
    private int renderDataIndex;

    // Lock Batches
    private ChunkLockBatchPoolStruct lockBatchPool;

    // Base \\

    @Override
    protected void create() {
        this.lockBatchPool = new ChunkLockBatchPoolStruct(
                EngineSetting.MEGA_CHUNK_SIZE * EngineSetting.MEGA_CHUNK_SIZE);
    }

    @Override
    protected void get() {
        this.renderDataIndex = MegaData.RENDER_DATA.index;
    }

    // Merge \\

    public void mergeChunkIntoMega(ChunkInstance chunkInstance, MegaChunkInstance mega, long expectedMegaCoordinate) {

        MegaDataSyncContainer megaSync = mega.getMegaDataSyncContainer();

        if (!megaSync.tryAcquire())
            return;

        try {
            if (mega.getCoordinate() != expectedMegaCoordinate)
                return;

            if (!mega.needsMerge(chunkInstance))
                return;

            if (!mergeLocked(chunkInstance, mega))
                return;

            megaSync.getData()[renderDataIndex] = false;

            if (mega.isReadyToRender())
                mega.finalizeGeometry();
        } finally {
            megaSync.release();
        }
    }

    // Locked Merge \\

    private boolean mergeLocked(ChunkInstance chunkInstance, MegaChunkInstance mega) {

        if (!mega.isRegistered(chunkInstance.getCoordinate()))
            return mega.batchAndMergeSingle(chunkInstance);

        ChunkLockBatchStruct batch = lockBatchPool.acquire();

        try {
            collectOtherChunks(chunkInstance, mega.getBatchedChunkList(), batch);
            return batch.tryLockAll() == ChunkLockResult.ACQUIRED && mega.batchAndMergeAll(chunkInstance);
        } finally {
            batch.unlockAll();
            lockBatchPool.release(batch);
        }
    }

    private void collectOtherChunks(
            ChunkInstance triggeringChunk,
            ObjectArrayList<ChunkInstance> batchedChunks,
            ChunkLockBatchStruct batch) {

        long ownCoordinate = triggeringChunk.getCoordinate();

        for (int i = 0; i < batchedChunks.size(); i++) {

            ChunkInstance candidate = batchedChunks.get(i);

            if (candidate.getCoordinate() == ownCoordinate)
                continue;

            if (batch.isFull())
                throwException("Mega re-merge found more member chunks than the mega's own scale allows — "
                        + "the batch registry has drifted out of sync with MEGA_CHUNK_SIZE.");

            batch.add(candidate);
        }
    }
}
