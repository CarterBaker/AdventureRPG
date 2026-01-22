package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class ChunkBatchSystem extends ManagerPackage {

    // Internal
    private ChunkStreamManager chunkStreamManager;
    private WorldRenderSystem worldRenderSystem;
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;

    // Chunk Position

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.worldRenderSystem = get(WorldRenderSystem.class);
    }

    // Batch Management \\

    public void batchChunk(ChunkInstance chunkInstance) {

        long megaChunkCoordinate = Coordinate2Long.toMegaChunkCoordinate(chunkInstance.getCoordinate());
        MegaChunkInstance megaChunkInstance = activeMegaChunks.computeIfAbsent(
                megaChunkCoordinate,
                this::addMegaChunkInstance);

        if (!megaChunkInstance.addChunkInstance(chunkInstance))
            return;

        megaChunkInstance.merge();
        worldRenderSystem.renderWorldInstance(megaChunkInstance);
    }

    private MegaChunkInstance addMegaChunkInstance(long megaChunkCoordinate) {

        MegaChunkInstance megaChunkInstance = create(MegaChunkInstance.class);
        megaChunkInstance.constructor(
                worldRenderSystem,
                chunkStreamManager.getActiveWorldHandle(),
                megaChunkCoordinate,
                chunkStreamManager.getChunkVAO());

        return megaChunkInstance;
    }

    // Utility \\

    public void setActiveMegaChunks(Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks) {
        this.activeMegaChunks = activeMegaChunks;
    }
}
