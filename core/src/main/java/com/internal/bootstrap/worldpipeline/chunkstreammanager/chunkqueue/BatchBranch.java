package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkState;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaState;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class BatchBranch extends BranchPackage {

    // Internal
    private ChunkStreamManager chunkStreamManager;
    private WorldRenderSystem worldRenderSystem;
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;

    // Batch Data
    private int MEGA_CHUNK_SIZE;
    private int megaScale;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.worldRenderSystem = get(WorldRenderSystem.class);

        // Batch Data
        this.MEGA_CHUNK_SIZE = EngineSetting.MEGA_CHUNK_SIZE;
        this.megaScale = MEGA_CHUNK_SIZE * MEGA_CHUNK_SIZE;
    }

    // Batch Management \\

    public void batchChunk(ChunkInstance chunkInstance) {

        if (!chunkInstance.tryBeginOperation(QueueOperation.BATCH))
            return;

        long megaChunkCoordinate = Coordinate2Long.toMegaChunkCoordinate(chunkInstance.getCoordinate());

        MegaChunkInstance megaChunkInstance = activeMegaChunks.computeIfAbsent(
                megaChunkCoordinate,
                this::addMegaChunkInstance);

        if (!megaChunkInstance.batchChunk(chunkInstance))
            return;

        // Update chunk state
        chunkInstance.setChunkState(ChunkState.HAS_BATCH_DATA);

        // Check if mega chunk is now complete
        if (megaChunkInstance.isComplete())
            megaChunkInstance.setMegaState(MegaState.NEEDS_MERGE_DATA);
    }

    private MegaChunkInstance addMegaChunkInstance(long megaChunkCoordinate) {

        MegaChunkInstance megaChunkInstance = create(MegaChunkInstance.class);
        megaChunkInstance.constructor(
                worldRenderSystem,
                chunkStreamManager.getActiveWorldHandle(),
                megaChunkCoordinate,
                chunkStreamManager.getChunkVAO(),
                megaScale);

        return megaChunkInstance;
    }

    // Accessible \\

    public void setActiveMegaChunks(Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks) {
        this.activeMegaChunks = activeMegaChunks;
    }
}