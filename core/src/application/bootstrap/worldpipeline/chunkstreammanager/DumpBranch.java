package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkDataUtility;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem;
import application.bootstrap.worldpipeline.worldrendermanager.RenderType;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import engine.root.BranchPackage;
import engine.util.mathematics.extras.Coordinate2Long;

public class DumpBranch extends BranchPackage {

    /*
     * Executes a single dump step per call. ChunkDataUtility determines which
     * stage to shed based on the requires graph, the slot detail level, and
     * the same two live signals ChunkQueueManager uses to decide whether to
     * dump in the first place — needsIndividualRender and partOfMegaBlock —
     * recomputed here identically so this branch can never dump RENDER_DATA
     * out from under a chunk that is only rendering because its mega hasn't
     * confirmed on GPU yet. The selected stage's flag is cleared directly;
     * nothing downstream needs a forced cascade, since nextToDump already
     * only ever selects a stage nothing still-pending depends on. Item
     * structs survive a GENERATION dump so ITEM_DATA can rebuild without a
     * full re-generation.
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

    public void dumpChunkData(GridInstance grid, ChunkInstance chunkInstance, GridSlotHandle gridSlotHandle) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return;

        try {
            long chunkCoordinate = chunkInstance.getCoordinate();
            GridSlotDetailLevel slotLevel = gridSlotHandle.getDetailLevel();

            boolean coveredByMega = !grid.getChunkRenderQueue().containsKey(chunkCoordinate);
            boolean needsIndividualRender = !coveredByMega
                    || !worldRenderManager.isMegaRendered(Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate));
            boolean partOfMegaBlock = coveredByMega && slotLevel.renderMode == RenderType.BATCHED;

            ChunkData toDump = ChunkDataUtility.nextToDump(
                    syncContainer.getData(), slotLevel, needsIndividualRender, partOfMegaBlock);

            if (toDump == null)
                return;

            syncContainer.getData()[toDump.index] = false;
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