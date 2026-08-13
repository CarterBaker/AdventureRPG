package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import application.bootstrap.worldpipeline.megastreammanager.MegaStreamManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;

public class BatchBranch extends BranchPackage {

    /*
     * Resolves the target mega for a chunk on the main thread — the only part
     * that touches the grid's shared, non-thread-safe mega registry and pool —
     * then dispatches the CPU-side vertex merge onto the WorldStreaming pool,
     * since merging is pure vertex math with no GL dependency. The chunk's own
     * lock is held for the async task's duration, exactly like every other
     * async stage, so a chunk can never be unloaded and pooled out from under
     * an in-flight merge. BATCH_DATA is cleared before dispatch and stays false
     * until MegaRenderBranch confirms the mega on GPU; the async
     * work-in-progress flag is what stops this chunk from being redispatched
     * every single frame while that upload is pending, and MegaMergeBranch's
     * own version check is what stops that redispatch from ever doing real
     * work again until this chunk's geometry actually changes.
     */

    // Internal
    private ThreadHandle threadHandle;
    private MegaStreamManager megaStreamManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
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

        executeAsync(threadHandle, () -> {
            try {
                syncContainer.acquire();
                try {
                    megaStreamManager.mergeIntoMega(chunkInstance, mega);
                } finally {
                    syncContainer.release();
                }
            } finally {
                syncContainer.endWork(ChunkDataSyncContainer.WORK_BATCH);
            }
        });
    }
}