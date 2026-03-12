package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotDetailLevel;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import com.internal.core.engine.BranchPackage;

/*
 * Dumps chunk data that is no longer required at the current detail level.
 * Each data stage has its own dedicated dump method.
 * Always tryAcquire — safe on any thread.
 *
 * Flag ordering rule: always clear the sync flag BEFORE performing the
 * external side-effecting work (e.g. pulling from the renderer). This
 * ensures that a concurrent or retry dispatch sees the stage as already
 * complete under the lock and cannot race into the same operation.
 */
public class DumpBranch extends BranchPackage {

    // Internal
    private BlockManager blockManager;
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private short airBlockId;

    // Internal \\

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
    }

    @Override
    protected void awake() {
        this.airBlockId = (short) blockManager.getBlockIDFromBlockName("TerraArcana/Air");
    }

    // Dump \\

    public void dumpChunkData(ChunkInstance chunkInstance, GridSlotHandle gridSlotHandle) {
        GridSlotDetailLevel targetLevel = gridSlotHandle.getDetailLevel();
        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();
        if (!syncContainer.tryAcquire())
            return;
        try {
            ChunkData dataToDump = targetLevel.getNextDataToDump(syncContainer.data);
            if (dataToDump == null)
                return;
            switch (dataToDump) {
                case GENERATION_DATA -> dumpGenerationData(chunkInstance, syncContainer);
                case BUILD_DATA -> dumpBuildData(chunkInstance, syncContainer);
                case MERGE_DATA -> dumpMergeData(chunkInstance, syncContainer);
                case ITEM_RENDER_DATA -> dumpItemRenderData(chunkInstance, syncContainer);
                case ITEM_DATA -> dumpItemData(chunkInstance, syncContainer);
                default -> {
                }
            }
        } finally {
            syncContainer.release();
        }
    }

    // Dump Methods \\

    private void dumpGenerationData(ChunkInstance chunkInstance,
            ChunkDataSyncContainer syncContainer) {
        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();
        for (SubChunkInstance subChunk : subChunks) {
            subChunk.getBlockPaletteHandle().dumpInteriorBlocks(airBlockId);
            subChunk.getWorldItemPaletteHandle().clear();
        }
        syncContainer.data[ChunkData.GENERATION_DATA.index] = false;
        syncContainer.data[ChunkData.LOAD_DATA.index] = false;
    }

    private void dumpBuildData(ChunkInstance chunkInstance,
            ChunkDataSyncContainer syncContainer) {
        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();
        for (SubChunkInstance subChunk : subChunks)
            subChunk.getDynamicPacketInstance().clear();
        syncContainer.data[ChunkData.BUILD_DATA.index] = false;
    }

    private void dumpMergeData(ChunkInstance chunkInstance,
            ChunkDataSyncContainer syncContainer) {
        chunkInstance.getDynamicPacketInstance().clear();
        syncContainer.data[ChunkData.MERGE_DATA.index] = false;
    }

    private void dumpItemRenderData(ChunkInstance chunkInstance,
            ChunkDataSyncContainer syncContainer) {
        syncContainer.data[ChunkData.ITEM_RENDER_DATA.index] = false;
        worldItemPlacementSystem.pullChunkFromRenderer(chunkInstance.getCoordinate());
    }

    private void dumpItemData(ChunkInstance chunkInstance,
            ChunkDataSyncContainer syncContainer) {
        chunkInstance.getWorldItemInstancePaletteHandle().clear();
        syncContainer.data[ChunkData.ITEM_DATA.index] = false;
    }
}