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
     * mega.reset() to fully clear its batch registry and its own
     * BATCH_DATA/RENDER_DATA flags. Every covered chunk's own BATCH_DATA is
     * cleared here too, reliably — nothing else in the engine ever sets that
     * flag except MegaRenderBranch after a confirmed upload, so a chunk this
     * loop failed to reach would keep reporting itself as already batched
     * into a mega that no longer has any record of it, and would never be
     * re-queued to rejoin it once the slot returns to NEAR.
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

            chunkSync.acquire();
            try {
                chunkSync.getData()[chunkBatchDataIndex] = false;
            } finally {
                chunkSync.release();
            }
        }
    }
}