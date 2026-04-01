package program.bootstrap.worldpipeline.chunkstreammanager;

import program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import program.bootstrap.worldpipeline.chunk.ChunkData;
import program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import program.bootstrap.worldpipeline.chunk.ChunkInstance;
import program.core.engine.BranchPackage;
import program.core.kernel.thread.ThreadHandle;

public class BuildBranch extends BranchPackage {

    /*
     * Async — builds per-subchunk geometry via DynamicGeometryManager on the
     * WorldStreaming thread. Sets BUILD_DATA on the chunk sync container only
     * if the full build succeeds.
     */

    // Internal
    private ThreadHandle threadHandle;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.dynamicGeometryAsyncContainer = dynamicGeometryManager.getDynamicGeometryAsyncInstance();
    }

    // Build \\

    public void buildChunk(ChunkInstance chunkInstance) {
        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        executeAsync(
                threadHandle,
                () -> {
                    DynamicGeometryAsyncContainer geo = dynamicGeometryAsyncContainer.getInstance();
                    try {
                        syncContainer.acquire();
                        if (dynamicGeometryManager.build(geo, chunkInstance))
                            syncContainer.getData()[ChunkData.BUILD_DATA.index] = true;
                    } finally {
                        syncContainer.release();
                        syncContainer.endWork(ChunkDataSyncContainer.WORK_BUILD);
                        geo.reset();
                    }
                });
    }
}