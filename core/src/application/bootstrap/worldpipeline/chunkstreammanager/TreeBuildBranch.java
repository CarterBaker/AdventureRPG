package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class TreeBuildBranch extends BranchPackage {

    /*
     * Async — draws the trees a chunk holds into its own tree packet on the
     * WorldStreaming thread, apart from its terrain. A tree reads nothing of
     * any neighbor, so only the chunk's own lock is held, and a tree that
     * changes never waits on the ground around it. buildLocked() is the one
     * place a chunk's trees are drawn, for the stream and for a change that
     * must show at once alike. Sets TREE_DATA on success.
     */

    // Internal
    private ThreadHandle threadHandle;
    private DynamicGeometryManager dynamicGeometryManager;

    // Settings
    private int treeDataIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.WORLD_STREAMING_THREAD_NAME);
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);

        // Settings
        this.treeDataIndex = ChunkData.TREE_DATA.index;
    }

    // Tree Build \\

    public void buildTrees(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        executeAsync(
                threadHandle,
                () -> {
                    try {
                        syncContainer.acquire();
                        syncContainer.getData()[treeDataIndex] = buildLocked(chunkInstance);
                    } finally {
                        syncContainer.release();
                        syncContainer.endWork(ChunkDataSyncContainer.WORK_TREE);
                    }
                });
    }

    // Under the chunk's lock, on any thread — true when every tree reached the packet
    boolean buildLocked(ChunkInstance chunkInstance) {
        return dynamicGeometryManager.buildTrees(chunkInstance);
    }
}
