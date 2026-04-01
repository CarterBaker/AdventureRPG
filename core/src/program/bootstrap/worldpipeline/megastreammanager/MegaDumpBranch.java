package program.bootstrap.worldpipeline.megastreammanager;

import program.bootstrap.worldpipeline.chunk.ChunkData;
import program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import program.bootstrap.worldpipeline.chunk.ChunkInstance;
import program.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import program.bootstrap.worldpipeline.megachunk.MegaData;
import program.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import program.core.engine.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MegaDumpBranch extends BranchPackage {

    /*
     * Fires when a mega's slot transitions to IMMEDIATE — chunks are close enough
     * to render individually. Removes the mega from the GPU, clears chunk
     * BATCH_DATA
     * flags so they re-contribute when the slot returns to NEAR, and clears
     * RENDER_DATA. BATCH_DATA on the mega is not dumpable — the chunk registry is
     * kept so re-contribution can happen without a full re-registration cycle.
     */

    // Internal
    private WorldRenderManager worldRenderSystem;

    // Settings
    private int renderDataIndex;
    private int chunkBatchDataIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.worldRenderSystem = get(WorldRenderManager.class);

        // Settings
        this.renderDataIndex = MegaData.RENDER_DATA.index;
        this.chunkBatchDataIndex = ChunkData.BATCH_DATA.index;
    }

    // Dump \\

    public void dumpMega(MegaChunkInstance mega, MegaDataSyncContainer sync, long megaCoord) {

        if (!sync.tryAcquire())
            return;

        try {
            worldRenderSystem.removeMegaInstance(megaCoord);
            clearChunkBatchFlags(mega);
            mega.getDynamicPacketInstance().clear();
            sync.getData()[renderDataIndex] = false;
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