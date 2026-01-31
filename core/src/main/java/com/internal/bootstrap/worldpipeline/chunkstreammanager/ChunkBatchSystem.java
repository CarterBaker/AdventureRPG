package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaState;
import com.internal.bootstrap.worldpipeline.worldrendersystem.RenderOperation;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.ThreadHandle;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class ChunkBatchSystem extends SystemPackage {

    // Internal
    private ThreadHandle threadHandle;
    private WorldRenderSystem worldRenderSystem;
    private ChunkStreamManager chunkStreamManager;

    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;

    private int MEGA_CHUNK_SIZE;
    private int megaScale;

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.worldRenderSystem = get(WorldRenderSystem.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);

        this.MEGA_CHUNK_SIZE = EngineSetting.MEGA_CHUNK_SIZE;
        this.megaScale = MEGA_CHUNK_SIZE * MEGA_CHUNK_SIZE;
    }

    @Override
    protected void update() {
        assessMegaChunks();
    }

    // Batching Logic \\

    public boolean batchChunk(ChunkInstance chunkInstance) {

        long megaChunkCoordinate = Coordinate2Long.toMegaChunkCoordinate(chunkInstance.getCoordinate());

        MegaChunkInstance megaChunkInstance = activeMegaChunks.computeIfAbsent(
                megaChunkCoordinate,
                this::createMegaChunkInstance);

        if (!megaChunkInstance.batchChunk(chunkInstance))
            return false;

        // Check if mega chunk is now complete
        if (megaChunkInstance.isComplete())
            megaChunkInstance.setMegaState(MegaState.NEEDS_MERGE_DATA);

        return true;
    }

    private MegaChunkInstance createMegaChunkInstance(long megaChunkCoordinate) {

        MegaChunkInstance megaChunkInstance = create(MegaChunkInstance.class);
        megaChunkInstance.constructor(
                worldRenderSystem,
                chunkStreamManager.getActiveWorldHandle(),
                megaChunkCoordinate,
                chunkStreamManager.getChunkVAO(),
                megaScale);

        // Transfer grid slot handle if chunk exists at mega coordinate
        ChunkInstance chunkInstance = activeChunks.get(megaChunkCoordinate);
        if (chunkInstance == null)
            return megaChunkInstance;

        GridSlotHandle gridSlotHandle = chunkInstance.getGridSlotHandle();
        if (gridSlotHandle != null)
            megaChunkInstance.setGridSlotHandle(gridSlotHandle);

        return megaChunkInstance;
    }

    // Assessment & Processing \\

    private void assessMegaChunks() {

        for (MegaChunkInstance megaChunkInstance : activeMegaChunks.values()) {

            if (megaChunkInstance.getMegaState() == MegaState.NEEDS_MERGE_DATA) {
                mergeMega(megaChunkInstance);
                continue;
            }

            RenderOperation renderOp = megaChunkInstance.getMegaRenderOperation();

            switch (renderOp) {
                case NONE -> {
                } // no-op
                case NEEDS_BATCH_RENDER -> renderBatched(megaChunkInstance);
                case HAS_BATCH_RENDER -> {
                } // no-op, already rendering
                case NEEDS_INDIVIDUAL_RENDER -> renderIndividual(megaChunkInstance);
                case HAS_INDIVIDUAL_RENDER -> {
                } // no-op, already rendering
            }
        }
    }

    // Merge Data \\

    private void mergeMega(MegaChunkInstance megaChunkInstance) {

        if (!megaChunkInstance.tryBeginOperation(MegaState.MERGING))
            return;

        executeAsync(threadHandle, () -> {

            if (megaChunkInstance.merge())
                megaChunkInstance.setMegaState(MegaState.COMPLETE);

            else
                megaChunkInstance.setMegaState(MegaState.NEEDS_MERGE_DATA);
        });
    }

    // Render Data \\

    private void renderBatched(MegaChunkInstance megaChunkInstance) {

        Long2ObjectOpenHashMap<ChunkInstance> batchedChunks = megaChunkInstance.getBatchedChunks();

        for (ChunkInstance chunkInstance : batchedChunks.values())
            worldRenderSystem.removeWorldInstance(chunkInstance.getCoordinate());

        if (worldRenderSystem.renderWorldInstance(megaChunkInstance))
            megaChunkInstance.setMegaState(MegaState.RENDERING_BATCHED);
    }

    private void renderIndividual(MegaChunkInstance megaChunkInstance) {

        Long2ObjectOpenHashMap<ChunkInstance> batchedChunks = megaChunkInstance.getBatchedChunks();

        for (ChunkInstance chunkInstance : batchedChunks.values()) {
            worldRenderSystem.removeWorldInstance(chunkInstance.getCoordinate());
            worldRenderSystem.renderWorldInstance(chunkInstance);
        }

        megaChunkInstance.setMegaState(MegaState.RENDERING_INDIVIDUAL);
    }

    // Accessible \\

    public void setActiveChunks(Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {
        this.activeChunks = activeChunks;
    }

    public void setActiveMegaChunks(Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks) {
        this.activeMegaChunks = activeMegaChunks;
    }
}