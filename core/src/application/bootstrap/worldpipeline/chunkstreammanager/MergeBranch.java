package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;

public class MergeBranch extends BranchPackage {

    /*
     * Async — merges all subchunk geometry packets into the chunk's single
     * geometry packet on the WorldStreaming thread. Sets MERGE_DATA on success.
     */

    // Internal
    private ThreadHandle threadHandle;

    // Settings
    private int mergeIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");

        // Settings
        this.mergeIndex = ChunkData.MERGE_DATA.index;
    }

    // Merge \\

    public void mergeChunk(ChunkInstance chunkInstance) {
        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        executeAsync(
                threadHandle,
                () -> {
                    try {
                        syncContainer.acquire();
                        boolean success = chunkInstance.merge();
                        syncContainer.getData()[mergeIndex] = success;
                    } finally {
                        syncContainer.release();
                        syncContainer.endWork(ChunkDataSyncContainer.WORK_MERGE);
                    }
                });
    }
}