package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunk.ChunkNeighborStruct;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;
import engine.util.mathematics.extras.Direction2Vector;

public class BuildBranch extends BranchPackage {

    /*
     * Async — builds per-subchunk geometry via DynamicGeometryManager on the
     * WorldStreaming thread. Geometry assembly reads across chunk borders into
     * each neighbor's block/biome palette, so every neighbor's own
     * ChunkDataSyncContainer lock is held for the full duration of the build —
     * the same convention every other branch already uses to protect that
     * data. NEIGHBOR_DATA on this chunk is only a point-in-time confirmation
     * made earlier by AssessmentBranch; without locking here, a neighbor could
     * be evicted, reset, and pooled for an unrelated location in the gap
     * between that confirmation and this build actually running, which reads
     * its biome palette back at the reserved "not generated" sentinel and
     * crashes. A neighbor lock that can't be acquired, or one that's acquired
     * but whose GENERATION_DATA turns out to already be false, both abort the
     * build for this pass and clear NEIGHBOR_DATA so assessment re-runs and
     * this chunk retries cleanly once every neighbor is actually ready. Sets
     * BUILD_DATA on the chunk sync container only if the full build succeeds.
     */

    // Internal
    private ThreadHandle threadHandle;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;

    // Settings
    private int neighborDataIndex;
    private int generationDataIndex;

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

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();
        ChunkDataSyncContainer[] neighborLocks = new ChunkDataSyncContainer[Direction2Vector.LENGTH];

        executeAsync(
                threadHandle,
                () -> {
                    DynamicGeometryAsyncContainer geo = dynamicGeometryAsyncContainer.getInstance();
                    try {
                        syncContainer.acquire();

                        if (!lockGeneratedNeighbors(chunkInstance, neighborLocks)) {
                            syncContainer.getData()[neighborDataIndex] = false;
                            return;
                        }

                        if (dynamicGeometryManager.build(geo, chunkInstance))
                            syncContainer.getData()[ChunkData.BUILD_DATA.index] = true;

                    } finally {
                        releaseNeighbors(neighborLocks);
                        syncContainer.release();
                        syncContainer.endWork(ChunkDataSyncContainer.WORK_BUILD);
                        geo.reset();
                    }
                });
    }

    /*
     * Attempts to lock every one of the eight neighbors and confirms each one
     * still has GENERATION_DATA at the moment its lock is taken. Bails and
     * leaves whatever is already locked for the caller's finally block to
     * release the instant a neighbor is missing, busy, or not actually
     * generated. A neighbor that resolves back to this same chunk (possible
     * only on a very small wrapped world) is checked directly against the
     * data the caller already holds, rather than attempting a second
     * self-acquire that would always fail. Two directions that happen to
     * resolve to the same neighbor instance — also only possible on a very
     * small world — share a single lock instead of double-acquiring it.
     */
    private boolean lockGeneratedNeighbors(ChunkInstance chunkInstance, ChunkDataSyncContainer[] neighborLocks) {

        ChunkNeighborStruct neighbors = chunkInstance.getChunkNeighbors();
        ChunkDataSyncContainer ownSync = chunkInstance.getChunkDataSyncContainer();

        for (int i = 0; i < Direction2Vector.LENGTH; i++) {

            ChunkInstance neighborChunk = neighbors.getNeighborChunk(i);

            if (neighborChunk == null)
                return false;

            ChunkDataSyncContainer neighborSync = neighborChunk.getChunkDataSyncContainer();

            if (neighborSync == ownSync) {
                if (!ownSync.getData()[generationDataIndex])
                    return false;
                continue;
            }

            if (isAlreadyLocked(neighborLocks, i, neighborSync)) {
                if (!neighborSync.getData()[generationDataIndex])
                    return false;
                continue;
            }

            if (!neighborSync.tryAcquire())
                return false;

            neighborLocks[i] = neighborSync;

            if (!neighborSync.getData()[generationDataIndex])
                return false;
        }

        return true;
    }

    private boolean isAlreadyLocked(ChunkDataSyncContainer[] neighborLocks, int upTo, ChunkDataSyncContainer target) {

        for (int i = 0; i < upTo; i++)
            if (neighborLocks[i] == target)
                return true;

        return false;
    }

    private void releaseNeighbors(ChunkDataSyncContainer[] neighborLocks) {

        for (int i = 0; i < neighborLocks.length; i++) {

            if (neighborLocks[i] == null)
                continue;

            neighborLocks[i].release();
            neighborLocks[i] = null;
        }
    }
}