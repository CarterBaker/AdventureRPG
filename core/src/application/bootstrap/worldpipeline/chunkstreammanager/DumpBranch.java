package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkDataUtility;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemPlacementSystem;
import application.bootstrap.worldpipeline.worldrendermanager.RenderType;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import engine.root.BranchPackage;
import engine.util.mathematics.extras.Coordinate2Long;

public class DumpBranch extends BranchPackage {

    /*
     * Sheds one chunk stage per call, chosen by ChunkDataUtility from the
     * dependency graph, the slot's detail level and the same live signals
     * ChunkQueueManager uses, so a chunk still rendering for its unconfirmed
     * mega keeps RENDER_DATA. Item structs survive a GENERATION dump; the
     * trees the chunk held are let go with it, and handed back when it
     * generates again. Trees taken off the GPU take their built flag with
     * them, since an uploaded tree packet keeps no CPU copy to upload again.
     */

    // Internal
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private WorldRenderManager worldRenderManager;
    private TreeManager treeManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
        this.worldRenderManager = get(WorldRenderManager.class);
        this.treeManager = get(TreeManager.class);
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
            long megaCoordinate = Coordinate2Long.toMegaChunkCoordinate(chunkCoordinate);
            boolean treesDrawnIndividually = !coveredByMega
                    || (syncContainer.getData()[ChunkData.TREE_RENDER_DATA.index]
                            && !worldRenderManager.isMegaTreesDrawn(megaCoordinate));

            ChunkData toDump = ChunkDataUtility.nextToDump(
                    syncContainer.getData(), slotLevel, needsIndividualRender, treesDrawnIndividually,
                    partOfMegaBlock);

            if (toDump == null)
                return;

            syncContainer.getData()[toDump.index] = false;
            executeDump(chunkInstance, syncContainer, toDump);
        } finally {
            syncContainer.release();
        }
    }

    private void executeDump(ChunkInstance chunkInstance, ChunkDataSyncContainer syncContainer, ChunkData stage) {
        switch (stage) {
            case GENERATION_DATA -> dumpGenerationData(chunkInstance);
            case BUILD_DATA -> dumpBuildData(chunkInstance);
            case MERGE_DATA -> dumpMergeData(chunkInstance);
            case RENDER_DATA -> dumpRenderData(chunkInstance);
            case TREE_DATA -> dumpTreeData(chunkInstance);
            case TREE_RENDER_DATA -> dumpTreeRenderData(chunkInstance, syncContainer);
            case ITEM_DATA -> dumpItemData(chunkInstance);
            case ITEM_RENDER_DATA -> dumpItemRenderData(chunkInstance);
            default -> {
            }
        }
    }

    private void dumpGenerationData(ChunkInstance chunkInstance) {

        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (SubChunkInstance subChunk : subChunks)
            subChunk.dumpInteriorToAir();

        treeManager.releaseTrees(chunkInstance.getTreePaletteHandle());
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

    private void dumpTreeData(ChunkInstance chunkInstance) {
        chunkInstance.getTreePacketInstance().clear();
    }

    private void dumpTreeRenderData(ChunkInstance chunkInstance, ChunkDataSyncContainer syncContainer) {
        worldRenderManager.removeChunkTrees(chunkInstance.getCoordinate());
        syncContainer.getData()[ChunkData.TREE_DATA.index] = false;
        dumpTreeData(chunkInstance);
    }

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