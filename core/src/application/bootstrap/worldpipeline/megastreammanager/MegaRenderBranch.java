package application.bootstrap.worldpipeline.megastreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import application.bootstrap.worldpipeline.megachunk.MegaData;
import application.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MegaRenderBranch extends BranchPackage {

    /*
     * Uploads merged mega geometry and clears the CPU copy, then sets
     * BATCH_DATA on every batched chunk under the mega lock, blocking on each
     * chunk's lock so none is missed. The batched list is only read while the
     * mega lock is held.
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

    // Render \\

    public void renderMega(MegaChunkInstance mega, MegaDataSyncContainer sync) {

        if (!sync.tryAcquire())
            return;

        try {
            if (!worldRenderSystem.addMegaInstance(mega))
                return;

            mega.getDynamicPacketInstance().clear();
            sync.getData()[renderDataIndex] = true;

            markBatchedChunksRendered(mega);
        } finally {
            sync.release();
        }
    }

    private void markBatchedChunksRendered(MegaChunkInstance mega) {

        ObjectArrayList<ChunkInstance> list = mega.getBatchedChunkList();
        Object[] elements = list.elements();
        int size = list.size();

        for (int i = 0; i < size; i++) {

            ChunkInstance chunk = (ChunkInstance) elements[i];
            ChunkDataSyncContainer chunkSync = chunk.getChunkDataSyncContainer();

            chunkSync.acquire();
            try {
                chunkSync.getData()[chunkBatchDataIndex] = true;
            } finally {
                chunkSync.release();
            }
        }
    }
}