package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.structuremanager.StructureManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class GenerationBranch extends BranchPackage {

    /*
     * Async — generates a chunk on the WorldStreaming thread. computeColumn()
     * resolves the column once and caches it per chunk, every subchunk then
     * generates from it, SettlementManager lays the settlements and the
     * roads between them that reach it, StructureManager stamps overlapping
     * structures, TreeManager hands the chunk every tree that reaches it,
     * none on ground a settlement or road claims, and the tide
     * surface is recorded. Sets LOAD_DATA, ESSENTIAL_DATA and
     * GENERATION_DATA once the chunk is populated.
     */

    // Internal
    private ThreadHandle threadHandle;
    private WorldGenerationManager worldGenerationManager;
    private SettlementManager settlementManager;
    private StructureManager structureManager;
    private TreeManager treeManager;

    // Settings
    private int loadIndex;
    private int essentialIndex;
    private int generationIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.WORLD_STREAMING_THREAD_NAME);
        this.worldGenerationManager = get(WorldGenerationManager.class);
        this.settlementManager = get(SettlementManager.class);
        this.structureManager = get(StructureManager.class);
        this.treeManager = get(TreeManager.class);

        // Settings
        this.loadIndex = ChunkData.LOAD_DATA.index;
        this.essentialIndex = ChunkData.ESSENTIAL_DATA.index;
        this.generationIndex = ChunkData.GENERATION_DATA.index;
    }

    // Generation \\

    public void getNewChunk(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        executeAsync(
                threadHandle,
                () -> {
                    try {
                        syncContainer.acquire();
                        syncContainer.getData()[loadIndex] = true;
                        generateChunk(chunkInstance, syncContainer);
                    } finally {
                        syncContainer.release();
                        syncContainer.endWork(ChunkDataSyncContainer.WORK_LOAD);
                    }
                });
    }

    private void generateChunk(
            ChunkInstance chunkInstance,
            ChunkDataSyncContainer container) {

        boolean success = true;
        long chunkCoordinate = chunkInstance.getCoordinate();
        WorldHandle worldHandle = chunkInstance.getWorldHandle();
        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        worldGenerationManager.computeColumn(worldHandle, chunkCoordinate, chunkInstance.getTerrainCache());

        for (int i = 0; i < EngineSetting.WORLD_HEIGHT; i++) {
            SubChunkInstance subChunk = subChunks[i];
            if (worldGenerationManager.generateSubChunk(worldHandle, chunkCoordinate, subChunk))
                continue;
            success = false;
            break;
        }

        if (success) {
            settlementManager.generateSettlements(worldHandle, chunkCoordinate, subChunks);
            structureManager.generateStructures(worldHandle, chunkCoordinate, subChunks);
            treeManager.generateTrees(worldHandle, chunkCoordinate, chunkInstance.getTreePaletteHandle());
            chunkInstance.setTideSurfaceLevels(worldGenerationManager.getColumnTideSurfaceLevels(chunkCoordinate));
            container.getData()[essentialIndex] = true;
            container.getData()[generationIndex] = true;
        }
    }
}