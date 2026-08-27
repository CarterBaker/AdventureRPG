package application.bootstrap.worldpipeline.megastreammanager;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaData;
import application.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MegaMergeBranch extends BranchPackage {

    /*
     * Merges a chunk's geometry into its parent mega. A fresh contribution
     * only ever reads the triggering chunk's own packet, already locked by
     * the caller, and is appended safely. A re-contribution instead rebuilds
     * the mega's whole packet from every registered chunk's current
     * geometry, since a chunk's prior contribution can't be surgically
     * removed from the shared vertex buffer — and every one of those OTHER
     * chunks has its own DynamicPacketInstance mutated independently, under
     * its own lock, by the ordinary streaming and liquid-tick pipelines.
     * Reading any of them without holding that same lock would race those
     * writers, so every other registered chunk's lock is acquired here
     * first, sorted by coordinate exactly like BuildBranch's neighbor
     * locking and always via non-blocking tryAcquire — a single unavailable
     * lock aborts the whole re-merge for this pass rather than ever
     * touching a chunk's packet unlocked. BATCH_DATA stays clear on an
     * aborted pass, so the chunk is simply reassessed and retried once
     * contention clears.
     */

    // Settings
    private int renderDataIndex;
    private int otherChunkCapacity;

    // Lock Batch Pool
    private final ConcurrentLinkedQueue<LockBatch> lockBatchPool = new ConcurrentLinkedQueue<>();

    // Internal \\

    @Override
    protected void create() {
        this.otherChunkCapacity = EngineSetting.MEGA_CHUNK_SIZE * EngineSetting.MEGA_CHUNK_SIZE;
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

        LockBatch batch = acquireBatch();

        try {
            resolveOtherChunks(chunkInstance, mega.getBatchedChunkList(), batch);
            return runLockedRemerge(chunkInstance, mega, batch);
        } finally {
            releaseBatch(batch);
        }
    }

    private void resolveOtherChunks(
            ChunkInstance triggeringChunk,
            ObjectArrayList<ChunkInstance> batchedChunks,
            LockBatch batch) {

        long ownCoordinate = triggeringChunk.getCoordinate();
        int count = 0;

        for (int i = 0; i < batchedChunks.size(); i++) {

            ChunkInstance candidate = batchedChunks.get(i);
            long coordinate = candidate.getCoordinate();

            if (coordinate == ownCoordinate)
                continue;

            if (count >= otherChunkCapacity)
                throwException("Mega re-merge found more member chunks than the mega's own scale allows — "
                        + "the batch registry has drifted out of sync with MEGA_CHUNK_SIZE.");

            insertSorted(batch, count, coordinate, candidate);
            count++;
        }

        batch.count = count;
    }

    private void insertSorted(LockBatch batch, int count, long coordinate, ChunkInstance chunk) {

        int j = count - 1;

        while (j >= 0 && batch.coordinates[j] > coordinate) {
            batch.coordinates[j + 1] = batch.coordinates[j];
            batch.resolved[j + 1] = batch.resolved[j];
            j--;
        }

        batch.coordinates[j + 1] = coordinate;
        batch.resolved[j + 1] = chunk;
    }

    private boolean runLockedRemerge(ChunkInstance triggeringChunk, MegaChunkInstance mega, LockBatch batch) {

        int heldCount = 0;

        try {
            for (int i = 0; i < batch.count; i++) {

                ChunkDataSyncContainer lock = batch.resolved[i].getChunkDataSyncContainer();
                batch.locks[i] = lock;

                if (!lock.tryAcquire())
                    return false;

                heldCount = i + 1;

                if (batch.resolved[i].getCoordinate() != batch.coordinates[i])
                    return false;
            }

            return mega.batchAndMergeAll(triggeringChunk);
        } finally {
            for (int i = 0; i < heldCount; i++)
                batch.locks[i].release();
        }
    }

    // Lock Batch Pool \\

    private LockBatch acquireBatch() {
        LockBatch batch = lockBatchPool.poll();
        return batch != null ? batch : new LockBatch(otherChunkCapacity);
    }

    private void releaseBatch(LockBatch batch) {
        batch.clear();
        lockBatchPool.offer(batch);
    }

    // Lock Batch \\

    private static final class LockBatch {

        final long[] coordinates;
        final ChunkInstance[] resolved;
        final ChunkDataSyncContainer[] locks;
        int count;

        LockBatch(int capacity) {
            this.coordinates = new long[capacity];
            this.resolved = new ChunkInstance[capacity];
            this.locks = new ChunkDataSyncContainer[capacity];
        }

        void clear() {
            Arrays.fill(resolved, null);
            Arrays.fill(locks, null);
            count = 0;
        }
    }
}