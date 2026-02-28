package com.internal.bootstrap.worldpipeline.megastreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

/*
 * Public facade for the mega chunk pipeline. Owns no state directly —
 * activeMegaChunks is dispatched from ChunkStreamManager. All logic
 * is delegated to MegaQueueManager.
 */
public class MegaStreamManager extends ManagerPackage {

    // Internal
    private MegaQueueManager megaQueueManager;
    // Internal \\

    @Override
    protected void create() {
        this.megaQueueManager = create(MegaQueueManager.class);
    }

    // Dispatch \\

    public void setActiveMegaChunks(Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks) {
        megaQueueManager.setActiveMegaChunks(activeMegaChunks);
    }

    // Grid rebuild trickle \\

    public void onGridRebuilt() {
        megaQueueManager.onGridRebuilt();
    }

    // Bridge from chunk pipeline \\

    public void batchChunk(ChunkInstance chunkInstance) {
        megaQueueManager.batchChunk(chunkInstance);
    }

    public void invalidateMegaForChunk(long chunkCoordinate) {
        megaQueueManager.invalidateMegaForChunk(chunkCoordinate);
    }
}