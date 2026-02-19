package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotDetailLevel;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.BranchPackage;

public class DumpBranch extends BranchPackage {

    // Internal
    private BlockManager blockManager;
    private short airBlockId;

    // Internal \\

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
    }

    @Override
    protected void awake() {
        this.airBlockId = (short) blockManager.getBlockIDFromBlockName("Air");
    }

    // Accessible \\

    public void dumpChunkData(ChunkInstance chunkInstance) {

        GridSlotHandle gridSlotHandle = chunkInstance.getGridSlotHandle();
        GridSlotDetailLevel targetLevel = gridSlotHandle.getDetailLevel();

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (syncContainer.isLocked())
            return;

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
                default -> {
                }
            }

        } finally {
            syncContainer.release();
        }
    }

    // Dump Methods \\

    private void dumpGenerationData(ChunkInstance chunkInstance, ChunkDataSyncContainer syncContainer) {

        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (SubChunkInstance subChunk : subChunks)
            subChunk.getBlockPaletteHandle().dumpInteriorBlocks(airBlockId);

        syncContainer.data[ChunkData.GENERATION_DATA.index] = false;
        syncContainer.data[ChunkData.LOAD_DATA.index] = false;
    }

    private void dumpBuildData(ChunkInstance chunkInstance, ChunkDataSyncContainer syncContainer) {

        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (SubChunkInstance subChunk : subChunks)
            subChunk.getDynamicPacketInstance().clear();

        syncContainer.data[ChunkData.BUILD_DATA.index] = false;
    }

    private void dumpMergeData(ChunkInstance chunkInstance, ChunkDataSyncContainer syncContainer) {

        chunkInstance.getDynamicPacketInstance().clear();

        syncContainer.data[ChunkData.MERGE_DATA.index] = false;
    }
}