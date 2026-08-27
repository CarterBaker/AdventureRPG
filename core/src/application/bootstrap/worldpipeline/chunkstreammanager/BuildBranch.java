package application.bootstrap.worldpipeline.chunkstreammanager;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunk.ChunkNeighborHandle;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;
import engine.util.mathematics.extras.Direction2Vector;

public class BuildBranch extends BranchPackage {

    /*
     * Async — builds per-subchunk geometry via DynamicGeometryManager on the
     * WorldStreaming thread. A build reads across chunk borders into each
     * neighbor's block/biome palette, so this chunk's lock and all 8
     * neighbors' locks are acquired strictly in ascending coordinate order
     * via non-blocking tryAcquire before any work begins, and held for the
     * full duration — the same global order for every concurrent build
     * anywhere in the world, so two adjacent builds can never deadlock each
     * other. The moment any single lock in the set is unavailable, every
     * lock already taken this attempt is released and the whole thing is
     * abandoned for this pass. Since a chunk with busy neighbors can be
     * redispatched many times before all nine locks ever line up free at
     * once, the coordinate/reference/lock arrays behind one attempt are
     * pooled rather than allocated fresh each time. The pool is a
     * ConcurrentLinkedQueue rather than the engine's usual ArrayDeque pools
     * because a batch is borrowed on the main thread and returned from the
     * WorldStreaming worker thread that finishes the async build.
     */

    // Internal
    private ThreadHandle threadHandle;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;

    // Settings
    private int neighborDataIndex;
    private int generationDataIndex;

    // Lock Batch Pool
    private final ConcurrentLinkedQueue<LockBatch> lockBatchPool = new ConcurrentLinkedQueue<>();

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.dynamicGeometryAsyncContainer = dynamicGeometryManager.getDynamicGeometryAsyncInstance();

        // Settings
        this.neighborDataIndex = ChunkData.NEIGHBOR_DATA.index;
        this.generationDataIndex = ChunkData.GENERATION_DATA.index;
    }

    // Build \\

    public void buildChunk(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer ownSync = chunkInstance.getChunkDataSyncContainer();
        LockBatch batch = acquireBatch();

        if (!resolveLockOrder(chunkInstance, batch)) {
            releaseBatch(batch);
            ownSync.setData(ChunkData.NEIGHBOR_DATA, false);
            ownSync.endWork(ChunkDataSyncContainer.WORK_BUILD);
            return;
        }

        resolveChunksInOrder(chunkInstance, batch);

        executeAsync(threadHandle, () -> runLockedBuild(chunkInstance, batch));
    }

    // Lock Batch Pool \\

    private LockBatch acquireBatch() {
        LockBatch batch = lockBatchPool.poll();
        return batch != null ? batch : new LockBatch();
    }

    private void releaseBatch(LockBatch batch) {
        batch.clear();
        lockBatchPool.offer(batch);
    }

    // Lock Ordering \\

    private boolean resolveLockOrder(ChunkInstance chunkInstance, LockBatch batch) {

        ChunkNeighborHandle neighbors = chunkInstance.getChunkNeighbors();
        int count = 0;

        batch.coordinates[count++] = chunkInstance.getCoordinate();

        for (int i = 0; i < Direction2Vector.LENGTH; i++) {

            ChunkInstance neighborChunk = neighbors.getNeighborChunk(i);

            if (neighborChunk == null)
                return false;

            long coordinate = neighborChunk.getCoordinate();
            boolean duplicate = false;

            for (int j = 0; j < count; j++) {
                if (batch.coordinates[j] == coordinate) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate)
                batch.coordinates[count++] = coordinate;
        }

        batch.count = count;
        Arrays.sort(batch.coordinates, 0, count);

        return true;
    }

    private void resolveChunksInOrder(ChunkInstance chunkInstance, LockBatch batch) {

        ChunkNeighborHandle neighbors = chunkInstance.getChunkNeighbors();
        long ownCoordinate = chunkInstance.getCoordinate();

        for (int i = 0; i < batch.count; i++) {

            if (batch.coordinates[i] == ownCoordinate) {
                batch.resolved[i] = chunkInstance;
                continue;
            }

            for (int d = 0; d < Direction2Vector.LENGTH; d++) {
                ChunkInstance candidate = neighbors.getNeighborChunk(d);
                if (candidate != null && candidate.getCoordinate() == batch.coordinates[i]) {
                    batch.resolved[i] = candidate;
                    break;
                }
            }
        }
    }

    // Locked Build \\

    private void runLockedBuild(ChunkInstance chunkInstance, LockBatch batch) {

        DynamicGeometryAsyncContainer geo = dynamicGeometryAsyncContainer.getInstance();
        ChunkDataSyncContainer ownSync = chunkInstance.getChunkDataSyncContainer();

        for (int i = 0; i < batch.count; i++)
            batch.locks[i] = batch.resolved[i].getChunkDataSyncContainer();

        int heldCount = 0;
        boolean missingPrecondition = false;
        boolean gotAllLocks = true;

        try {
            for (int i = 0; i < batch.count; i++) {

                if (!batch.locks[i].tryAcquire()) {
                    gotAllLocks = false;
                    break;
                }

                heldCount = i + 1;

                if (batch.resolved[i].getCoordinate() != batch.coordinates[i]) {
                    missingPrecondition = true;
                    gotAllLocks = false;
                    break;
                }

                if (!batch.locks[i].getData()[generationDataIndex]) {
                    missingPrecondition = true;
                    gotAllLocks = false;
                    break;
                }
            }

            if (!gotAllLocks)
                return;

            if (dynamicGeometryManager.build(geo, chunkInstance))
                ownSync.getData()[ChunkData.BUILD_DATA.index] = true;

        } finally {

            if (missingPrecondition)
                ownSync.getData()[neighborDataIndex] = false;

            for (int i = 0; i < heldCount; i++)
                batch.locks[i].release();

            ownSync.endWork(ChunkDataSyncContainer.WORK_BUILD);
            geo.reset();
            releaseBatch(batch);
        }
    }

    // Lock Batch \\

    private static final class LockBatch {

        final long[] coordinates = new long[Direction2Vector.LENGTH + 1];
        final ChunkInstance[] resolved = new ChunkInstance[Direction2Vector.LENGTH + 1];
        final ChunkDataSyncContainer[] locks = new ChunkDataSyncContainer[Direction2Vector.LENGTH + 1];
        int count;

        void clear() {
            Arrays.fill(resolved, null);
            Arrays.fill(locks, null);
            count = 0;
        }
    }
}