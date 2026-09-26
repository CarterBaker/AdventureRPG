package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import application.bootstrap.worldpipeline.megastreammanager.MegaStreamManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class BatchBranch extends BranchPackage {

    /*
     * Resolves a chunk's target mega on the main thread, then merges its
     * vertices into the mega on the WorldStreaming pool under the chunk's lock.
     * The mega's coordinate is captured at resolution so the merge refuses a
     * pooled mega that has since moved; BATCH_DATA stays clear until the mega
     * reaches the GPU.
     */

    // Internal
    private ThreadHandle threadHandle;
    private MegaStreamManager megaStreamManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.WORLD_STREAMING_THREAD_NAME);
        this.megaStreamManager = get(MegaStreamManager.class);
    }

    // Batch \\

    public void batchChunk(ChunkInstance chunkInstance, GridInstance grid) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();
        syncContainer.setData(ChunkData.BATCH_DATA, false);

        MegaChunkInstance mega = megaStreamManager.resolveMegaForChunk(chunkInstance, grid);

        if (mega == null) {
            syncContainer.endWork(ChunkDataSyncContainer.WORK_BATCH);
            return;
        }

        long expectedMegaCoordinate = mega.getCoordinate();

        executeAsync(threadHandle, () -> {
            try {
                syncContainer.acquire();
                try {
                    megaStreamManager.mergeIntoMega(chunkInstance, mega, expectedMegaCoordinate);
                } finally {
                    syncContainer.release();
                }
            } finally {
                syncContainer.endWork(ChunkDataSyncContainer.WORK_BATCH);
            }
        });
    }
}