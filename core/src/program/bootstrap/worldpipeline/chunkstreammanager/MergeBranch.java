package program.bootstrap.worldpipeline.chunkstreammanager;

import program.bootstrap.worldpipeline.chunk.ChunkData;
import program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import program.bootstrap.worldpipeline.chunk.ChunkInstance;
import program.core.engine.BranchPackage;
import program.core.kernel.thread.ThreadHandle;

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