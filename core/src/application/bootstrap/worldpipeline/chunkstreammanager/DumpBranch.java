package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkDataUtility;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import engine.root.BranchPackage;

public class DumpBranch extends BranchPackage {

    /*
     * Executes a single dump step per call. ChunkDataUtility determines which
     * stage to shed based on the leadsTo/requires graph and the slot detail level.
     * cascadeClear runs before side-effecting work so concurrent dispatches see
     * the stage as already gone. Item structs survive a GENERATION dump so
     * ITEM_DATA can rebuild without a full re-generation.
     */

    // Internal
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private WorldRenderManager worldRenderManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
        this.worldRenderManager = get(WorldRenderManager.class);
    }

    // Dump \\

    public void dumpChunkData(ChunkInstance chunkInstance, GridSlotHandle gridSlotHandle) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return;

        try {
            ChunkData toDump = ChunkDataUtility.nextToDump(
                    syncContainer.getData(),
                    gridSlotHandle.getDetailLevel());

            if (toDump == null)
                return;

            ChunkDataUtility.cascadeClear(toDump, syncContainer.getData());
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
     * Hollows the interior of any subchunk that has real per-block storage.
     * A subchunk still in its virtual (empty or uniform) representation has
     * nothing to hollow and is left alone — it's already as compact as it
     * can be.
     */
    private void dumpGenerationData(ChunkInstance chunkInstance) {

        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (SubChunkInstance subChunk : subChunks)
            subChunk.dumpInteriorToAir();
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