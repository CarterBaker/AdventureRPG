package application.bootstrap.worldpipeline.chunkstreammanager;

import java.util.Arrays;

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
     * WorldStreaming thread. A build reads across chunk borders into each
     * neighbor's block/biome palette, so this chunk's lock and all 8
     * neighbors' locks are held for the full duration. Every lock in that
     * set is resolved to its owning chunk's coordinate on the calling
     * (main) thread, sorted ascending, and then acquired strictly in that
     * order with non-blocking tryAcquire — every concurrent build anywhere
     * in the world uses the same global order, so two adjacent builds can
     * never each be holding a lock the other one needs. The moment any
     * single lock in the set is unavailable, every lock already taken this
     * attempt is released immediately and the whole thing is abandoned for
     * this pass rather than left half-held. NEIGHBOR_DATA is only cleared
     * when a neighbor is genuinely missing or ungenerated — never for a
     * plain lock race — so ordinary contention just retries next pass
     * instead of forcing a full 8-neighbor reassessment.
     *
     * Chunk instances are pooled and can be handed out to a brand new
     * coordinate the moment they unload, so the ChunkInstance references
     * resolved here on the main thread can, in principle, be reassigned to
     * a completely different coordinate before this async task reaches the
     * front of the WorldStreaming queue. Acquiring a lock only proves the
     * object is currently safe to read — it says nothing about whether it
     * is still the neighbor this task was actually dispatched for. Every
     * lock is therefore re-validated against the coordinate it was resolved
     * against the instant it is acquired; a mismatch is treated exactly
     * like a missing neighbor rather than silently merging whatever chunk
     * now occupies that object.
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

        ChunkDataSyncContainer ownSync = chunkInstance.getChunkDataSyncContainer();
        long[] lockOrder = resolveLockOrder(chunkInstance);

        if (lockOrder == null) {
            ownSync.setData(ChunkData.NEIGHBOR_DATA, false);
            ownSync.endWork(ChunkDataSyncContainer.WORK_BUILD);
            return;
        }

        ChunkInstance[] resolved = resolveChunksInOrder(chunkInstance, lockOrder);
        int ownIndex = -1;

        for (int i = 0; i < resolved.length; i++)
            if (resolved[i] == chunkInstance)
                ownIndex = i;

        int finalOwnIndex = ownIndex;

        executeAsync(threadHandle, () -> runLockedBuild(chunkInstance, resolved, lockOrder, finalOwnIndex));
    }

    // Lock Ordering \\

    private long[] resolveLockOrder(ChunkInstance chunkInstance) {

        ChunkNeighborStruct neighbors = chunkInstance.getChunkNeighbors();
        long[] coordinates = new long[Direction2Vector.LENGTH + 1];
        int count = 0;

        coordinates[count++] = chunkInstance.getCoordinate();

        for (int i = 0; i < Direction2Vector.LENGTH; i++) {

            ChunkInstance neighborChunk = neighbors.getNeighborChunk(i);

            if (neighborChunk == null)
                return null;

            long coordinate = neighborChunk.getCoordinate();
            boolean duplicate = false;

            for (int j = 0; j < count; j++) {
                if (coordinates[j] == coordinate) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate)
                coordinates[count++] = coordinate;
        }

        long[] trimmed = count == coordinates.length ? coordinates : Arrays.copyOf(coordinates, count);
        Arrays.sort(trimmed);
        return trimmed;
    }

    private ChunkInstance[] resolveChunksInOrder(ChunkInstance chunkInstance, long[] lockOrder) {

        ChunkNeighborStruct neighbors = chunkInstance.getChunkNeighbors();
        ChunkInstance[] resolved = new ChunkInstance[lockOrder.length];
        long ownCoordinate = chunkInstance.getCoordinate();

        for (int i = 0; i < lockOrder.length; i++) {

            if (lockOrder[i] == ownCoordinate) {
                resolved[i] = chunkInstance;
                continue;
            }

            for (int d = 0; d < Direction2Vector.LENGTH; d++) {
                ChunkInstance candidate = neighbors.getNeighborChunk(d);
                if (candidate != null && candidate.getCoordinate() == lockOrder[i]) {
                    resolved[i] = candidate;
                    break;
                }
            }
        }

        return resolved;
    }

    // Locked Build \\

    private void runLockedBuild(
            ChunkInstance chunkInstance,
            ChunkInstance[] resolved,
            long[] lockOrder,
            int ownIndex) {

        DynamicGeometryAsyncContainer geo = dynamicGeometryAsyncContainer.getInstance();
        ChunkDataSyncContainer ownSync = chunkInstance.getChunkDataSyncContainer();

        ChunkDataSyncContainer[] locks = new ChunkDataSyncContainer[resolved.length];
        for (int i = 0; i < resolved.length; i++)
            locks[i] = resolved[i].getChunkDataSyncContainer();

        int heldCount = 0;
        boolean missingPrecondition = false;
        boolean gotAllLocks = true;

        try {
            for (int i = 0; i < locks.length; i++) {

                if (!locks[i].tryAcquire()) {
                    gotAllLocks = false;
                    break;
                }

                heldCount = i + 1;

                if (resolved[i].getCoordinate() != lockOrder[i]) {
                    missingPrecondition = true;
                    gotAllLocks = false;
                    break;
                }

                if (!locks[i].getData()[generationDataIndex]) {
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
                locks[i].release();

            ownSync.endWork(ChunkDataSyncContainer.WORK_BUILD);
            geo.reset();
        }
    }
}