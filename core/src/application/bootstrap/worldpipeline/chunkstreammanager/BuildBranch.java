package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunk.ChunkLockBatchPoolStruct;
import application.bootstrap.worldpipeline.chunk.ChunkLockBatchStruct;
import application.bootstrap.worldpipeline.chunk.ChunkLockResult;
import application.bootstrap.worldpipeline.chunk.ChunkNeighborHandle;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction2Vector;

public class BuildBranch extends BranchPackage {

    /*
     * Async — builds per-subchunk geometry on the WorldStreaming thread. A
     * build reads into all eight neighbors, so it holds this chunk's lock and
     * every neighbor's, taken together through a ChunkLockBatchStruct. A busy
     * lock abandons the attempt for this pass; a stale or ungenerated
     * neighbor also clears NEIGHBOR_DATA so the chunk is reassessed.
     */

    // Internal
    private ThreadHandle threadHandle;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;

    // Settings
    private int neighborDataIndex;
    private int generationDataIndex;
    private int buildDataIndex;

    // Lock Batches
    private ChunkLockBatchPoolStruct lockBatchPool;

    // Base \\

    @Override
    protected void create() {
        this.lockBatchPool = new ChunkLockBatchPoolStruct(Direction2Vector.LENGTH + 1);
    }

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.WORLD_STREAMING_THREAD_NAME);
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.dynamicGeometryAsyncContainer = dynamicGeometryManager.getDynamicGeometryAsyncInstance();

        // Settings
        this.neighborDataIndex = ChunkData.NEIGHBOR_DATA.index;
        this.generationDataIndex = ChunkData.GENERATION_DATA.index;
        this.buildDataIndex = ChunkData.BUILD_DATA.index;
    }

    // Build \\

    public void buildChunk(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer ownSync = chunkInstance.getChunkDataSyncContainer();
        ChunkLockBatchStruct batch = lockBatchPool.acquire();

        if (!collectLockSet(chunkInstance, batch)) {
            lockBatchPool.release(batch);
            ownSync.setData(ChunkData.NEIGHBOR_DATA, false);
            ownSync.endWork(ChunkDataSyncContainer.WORK_BUILD);
            return;
        }

        executeAsync(threadHandle, () -> runLockedBuild(chunkInstance, batch));
    }

    private boolean collectLockSet(ChunkInstance chunkInstance, ChunkLockBatchStruct batch) {

        ChunkNeighborHandle neighbors = chunkInstance.getChunkNeighbors();

        batch.add(chunkInstance);

        for (int i = 0; i < Direction2Vector.LENGTH; i++) {

            ChunkInstance neighborChunk = neighbors.getNeighborChunk(i);

            if (neighborChunk == null)
                return false;

            batch.add(neighborChunk);
        }

        return true;
    }

    // Locked Build \\

    private void runLockedBuild(ChunkInstance chunkInstance, ChunkLockBatchStruct batch) {

        DynamicGeometryAsyncContainer geo = dynamicGeometryAsyncContainer.getInstance();
        ChunkDataSyncContainer ownSync = chunkInstance.getChunkDataSyncContainer();
        ChunkLockResult result = ChunkLockResult.BUSY;

        try {
            result = batch.tryLockAll(generationDataIndex);

            if (result == ChunkLockResult.ACQUIRED && dynamicGeometryManager.build(geo, chunkInstance))
                ownSync.getData()[buildDataIndex] = true;
        } finally {

            if (result == ChunkLockResult.STALE)
                ownSync.getData()[neighborDataIndex] = false;

            batch.unlockAll();
            ownSync.endWork(ChunkDataSyncContainer.WORK_BUILD);
            geo.reset();
            lockBatchPool.release(batch);
        }
    }
}
