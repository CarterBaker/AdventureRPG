package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaChunkInstance;
import com.internal.bootstrap.worldpipeline.megachunk.MegaState;
import com.internal.bootstrap.worldpipeline.worldrendersystem.RenderOperation;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.ThreadHandle;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class BatchSystem extends SystemPackage {

    // Internal
    private ThreadHandle threadHandle;
    private WorldRenderSystem worldRenderSystem;
    private Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.worldRenderSystem = get(WorldRenderSystem.class);
    }

    @Override
    protected void update() {
        assessMegaChunks();
    }

    // Batch Management \\

    private void assessMegaChunks() {

        for (MegaChunkInstance megaInstance : activeMegaChunks.values()) {

            if (megaInstance.getMegaState() == MegaState.NEEDS_MERGE_DATA)
                mergeMega(megaInstance);

            RenderOperation renderOp = megaInstance.getMegaRenderOperation();

            switch (renderOp) {
                case NONE -> {
                } // no-op
                case NEEDS_BATCH_RENDER -> renderBatched(megaInstance);
                case HAS_BATCH_RENDER -> {
                } // no-op, already rendering
                case NEEDS_INDIVIDUAL_RENDER -> renderIndividual(megaInstance);
                case HAS_INDIVIDUAL_RENDER -> {
                } // no-op, already rendering
            }
        }
    }

    // Merge Data \\

    private void mergeMega(MegaChunkInstance megaInstance) {

        if (!megaInstance.tryBeginOperation(MegaState.MERGING))
            return;

        // Submit to merge thread
        executeAsync(threadHandle, () -> {

            if (megaInstance.merge())
                megaInstance.setMegaState(MegaState.COMPLETE);

            else
                megaInstance.setMegaState(MegaState.NEEDS_MERGE_DATA);
        });
    }

    // Render Data \\

    private void renderBatched(MegaChunkInstance megaInstance) {

        // Remove any existing render data
        var batchedChunks = megaInstance.getBatchedChunks();

        for (ChunkInstance chunkInstance : batchedChunks.values())
            worldRenderSystem.removeWorldInstance(chunkInstance.getCoordinate());

        // Render as single batched mega
        if (worldRenderSystem.renderWorldInstance(megaInstance))
            megaInstance.setMegaState(MegaState.RENDERING_BATCHED);
    }

    private void renderIndividual(MegaChunkInstance megaInstance) {

        // Remove any existing render data
        var batchedChunks = megaInstance.getBatchedChunks();

        for (ChunkInstance chunkInstance : batchedChunks.values()) {
            worldRenderSystem.removeWorldInstance(chunkInstance.getCoordinate());
            worldRenderSystem.renderWorldInstance(chunkInstance);
        }

        megaInstance.setMegaState(MegaState.RENDERING_INDIVIDUAL);
    }

    // Accessible \\

    public void setActiveMegaChunks(Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks) {
        this.activeMegaChunks = activeMegaChunks;
    }
}
