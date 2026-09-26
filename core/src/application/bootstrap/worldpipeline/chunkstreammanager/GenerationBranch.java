package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.structuremanager.StructureManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class GenerationBranch extends BranchPackage {

    /*
     * Async — generates a chunk on the WorldStreaming thread. computeColumn()
     * resolves the column once and caches it per chunk, every subchunk then
     * generates from it, StructureManager stamps overlapping structures, and
     * the tide surface is recorded. Sets LOAD_DATA, ESSENTIAL_DATA and
     * GENERATION_DATA once the chunk is populated.
     */

    // Internal
    private ThreadHandle threadHandle;
    private WorldGenerationManager worldGenerationManager;
    private StructureManager structureManager;

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
        this.structureManager = get(StructureManager.class);

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
            structureManager.generateStructures(worldHandle, chunkCoordinate, subChunks);
            chunkInstance.setTideSurfaceLevels(worldGenerationManager.getColumnTideSurfaceLevels(chunkCoordinate));
            container.getData()[essentialIndex] = true;
            container.getData()[generationIndex] = true;
        }
    }
}