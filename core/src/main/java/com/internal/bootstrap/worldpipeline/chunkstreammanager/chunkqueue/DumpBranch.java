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

        // Skip if locked
        if (syncContainer.isLocked())
            return;

        // Try to acquire lock
        if (!syncContainer.tryAcquire())
            return;

        try {

            // Get what needs to be dumped
            ChunkData dataToDump = targetLevel.getNextDataToDump(syncContainer.data);

            if (dataToDump == null)
                return; // Nothing to dump

            // Dump the appropriate data
            switch (dataToDump) {

                case BATCH_DATA -> dumpBatchData(chunkInstance, syncContainer);

                case MERGE_DATA -> dumpMergeData(chunkInstance, syncContainer);

                case BUILD_DATA -> dumpBuildData(chunkInstance, syncContainer);

                case NEIGHBOR_DATA -> dumpNeighborData(syncContainer);

                case GENERATION_DATA -> dumpGenerationData(chunkInstance, syncContainer);

                case LOAD_DATA -> dumpLoadData(syncContainer);

                case ESSENTIAL_DATA, RENDER_DATA -> {
                } // Never dump these flags
            }

        } finally {
            syncContainer.release();
        }
    }

    // Dump Methods \\

    private void dumpBatchData(ChunkInstance chunkInstance, ChunkDataSyncContainer syncContainer) {
        // Just clear the flag - the chunk is no longer part of a mega batch
        syncContainer.data[ChunkData.BATCH_DATA.index] = false;
    }

    private void dumpMergeData(ChunkInstance chunkInstance, ChunkDataSyncContainer syncContainer) {
        // Clear merged geometry from chunk's dynamic packet
        chunkInstance.getDynamicPacketInstance().clear();

        syncContainer.data[ChunkData.MERGE_DATA.index] = false;
    }

    private void dumpBuildData(ChunkInstance chunkInstance, ChunkDataSyncContainer syncContainer) {
        // Clear geometry from all subchunks
        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (SubChunkInstance subChunk : subChunks)
            subChunk.getDynamicPacketInstance().clear();

        syncContainer.data[ChunkData.BUILD_DATA.index] = false;
    }

    private void dumpNeighborData(ChunkDataSyncContainer syncContainer) {
        // Just clear the flag - neighbor references don't need explicit clearing
        // They're just pointers that will naturally become stale when neighbors change
        // detail levels
        syncContainer.data[ChunkData.NEIGHBOR_DATA.index] = false;
    }

    private void dumpGenerationData(ChunkInstance chunkInstance, ChunkDataSyncContainer syncContainer) {
        // Dump interior blocks from all subchunks, keeping only outer shell
        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (SubChunkInstance subChunk : subChunks)
            subChunk.getBlockPaletteHandle().dumpInteriorBlocks(airBlockId);

        // Clear GENERATION_DATA so we can regenerate interior if needed later
        // Keep ESSENTIAL_DATA true - this marks we have at least shell data for
        // neighbors
        // Clear LOAD_DATA so we can try loading again if chunk moves back to higher
        // detail
        syncContainer.data[ChunkData.GENERATION_DATA.index] = false;
        syncContainer.data[ChunkData.LOAD_DATA.index] = false;
    }

    private void dumpLoadData(ChunkDataSyncContainer syncContainer) {
        // Just clear the load flag - this case shouldn't normally happen
        // since LOAD_DATA should only be set alongside GENERATION_DATA
        syncContainer.data[ChunkData.LOAD_DATA.index] = false;
    }
}