package application.bootstrap.worldpipeline.megastreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MegaDumpBranch extends BranchPackage {

    /*
     * Fires when a mega's slot transitions to IMMEDIATE — chunks are close
     * enough to render individually. Removes the mega from the GPU and calls
     * mega.reset() to fully clear its batch registry (mergedCoordinates,
     * batchedChunks, mergedChunkVersions) and its own BATCH_DATA/RENDER_DATA
     * flags, exactly like invalidateMegaForChunk/unloadMega already do.
     * Previously this only cleared RENDER_DATA and the mega's packet while
     * leaving BATCH_DATA true and the registry intact — isReadyToRender()
     * stayed true from the old registry, so BATCH_DATA was never re-requested,
     * and every covered chunk's later re-contribution saw needsMerge() return
     * false (its own content never changed), leaving the wiped packet
     * permanently unrebuilt. Chunk BATCH_DATA flags are cleared here too so
     * every covered chunk is forced to re-register from scratch when the slot
     * returns to NEAR.
     */

    // Internal
    private WorldRenderManager worldRenderSystem;

    // Settings
    private int chunkBatchDataIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.worldRenderSystem = get(WorldRenderManager.class);

        // Settings
        this.chunkBatchDataIndex = ChunkData.BATCH_DATA.index;
    }

    // Dump \\

    public void dumpMega(MegaChunkInstance mega, MegaDataSyncContainer sync, long megaCoord) {

        if (!sync.tryAcquire())
            return;

        try {
            worldRenderSystem.removeMegaInstance(megaCoord);
            clearChunkBatchFlags(mega);
            mega.reset();
        } finally {
            sync.release();
        }
    }

    private void clearChunkBatchFlags(MegaChunkInstance mega) {

        ObjectArrayList<ChunkInstance> list = mega.getBatchedChunkList();
        Object[] elements = list.elements();
        int size = list.size();

        for (int i = 0; i < size; i++) {

            ChunkInstance chunk = (ChunkInstance) elements[i];
            ChunkDataSyncContainer chunkSync = chunk.getChunkDataSyncContainer();

            if (!chunkSync.tryAcquire())
                continue;

            try {
                chunkSync.getData()[chunkBatchDataIndex] = false;
            } finally {
                chunkSync.release();
            }
        }
    }
}