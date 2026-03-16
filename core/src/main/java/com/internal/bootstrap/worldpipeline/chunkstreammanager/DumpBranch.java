package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataUtility;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.BranchPackage;

/*
 * Executes a single dump step per call.
 * ChunkDataUtility determines which stage to shed based on the leadsTo/requires
 * graph and the slot's detail level. cascadeClear runs before any side-effecting
 * work so concurrent or retry dispatches see the stage as already gone.
 *
 * Data lifetime per level:
 *   IMMEDIATE — everything held
 *   NEAR      — item instances and render dumped; block and item structs held
 *   DISTANT   — everything dumped
 *
 * Item structs (subchunk palette) are owned by ITEM_DATA, not GENERATION_DATA.
 * They survive a GENERATION dump so ITEM_DATA can rebuild correctly when the
 * chunk returns to IMMEDIATE range without needing a full re-generation.
 */
public class DumpBranch extends BranchPackage {

    private BlockManager blockManager;
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private WorldRenderManager worldRenderManager;
    private short airBlockId;

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
        this.worldRenderManager = get(WorldRenderManager.class);
    }

    @Override
    protected void awake() {
        this.airBlockId = (short) blockManager.getBlockIDFromBlockName("TerraArcana/Air");
    }

    public void dumpChunkData(ChunkInstance chunkInstance, GridSlotHandle gridSlotHandle) {
        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();
        if (!syncContainer.tryAcquire())
            return;
        try {
            ChunkData toDump = ChunkDataUtility.nextToDump(
                    syncContainer.data,
                    gridSlotHandle.getDetailLevel());
            if (toDump == null)
                return;
            ChunkDataUtility.cascadeClear(toDump, syncContainer.data);
            executeDump(chunkInstance, toDump);
        } finally {
            syncContainer.release();
        }
    }

    private void executeDump(ChunkInstance chunkInstance, ChunkData stage) {
        switch (stage) {
            case GENERATION_DATA -> dumpGenerationData(chunkInstance);
            case BUILD_DATA -> dumpBuildData(chunkInstance);
            case MERGE_DATA -> dumpMergeData(chunkInstance);
            case RENDER_DATA -> dumpRenderData(chunkInstance);
            case ITEM_DATA -> dumpItemData(chunkInstance);
            case ITEM_RENDER_DATA -> dumpItemRenderData(chunkInstance);
            default -> {
            }
        }
    }

    /*
     * Only clears block palette — item structs on subchunks are left intact.
     * They are the source of truth for ITEM_DATA rebuilds and are owned
     * exclusively by dumpItemData.
     */
    private void dumpGenerationData(ChunkInstance chunkInstance) {
        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();
        for (SubChunkInstance subChunk : subChunks)
            subChunk.getBlockPaletteHandle().dumpInteriorBlocks(airBlockId);
    }

    private void dumpBuildData(ChunkInstance chunkInstance) {
        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();
        for (SubChunkInstance subChunk : subChunks)
            subChunk.getDynamicPacketInstance().clear();
    }

    private void dumpMergeData(ChunkInstance chunkInstance) {
        chunkInstance.getDynamicPacketInstance().clear();
    }

    private void dumpRenderData(ChunkInstance chunkInstance) {
        worldRenderManager.removeChunkInstance(chunkInstance.getCoordinate());
    }

    /*
     * Clears both the chunk instance palette and the subchunk struct palette.
     * These are one logical unit — struct palette only exists to rebuild the
     * instance palette, so both go together.
     */
    private void dumpItemData(ChunkInstance chunkInstance) {
        chunkInstance.getWorldItemInstancePaletteHandle().clear();
        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();
        for (SubChunkInstance subChunk : subChunks)
            subChunk.getWorldItemPaletteHandle().clear();
    }

    private void dumpItemRenderData(ChunkInstance chunkInstance) {
        worldItemPlacementSystem.pullChunkFromRenderer(chunkInstance.getCoordinate());
    }
}