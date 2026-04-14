package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class GenerationBranch extends BranchPackage {

    /*
     * Async — attempts to load the chunk from disk/cache first, then falls back
     * to procedural generation. Sets LOAD_DATA, ESSENTIAL_DATA, and
     * GENERATION_DATA on the sync container once the chunk is fully populated.
     * Runs on the WorldStreaming thread.
     */

    // Internal
    private ThreadHandle threadHandle;
    private WorldGenerationManager worldGenerationManager;

    // Settings
    private int loadIndex;
    private int essentialIndex;
    private int generationIndex;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.worldGenerationManager = get(WorldGenerationManager.class);

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
                        boolean[] data = syncContainer.getData();

                        boolean loaded = false;

                        if (!data[loadIndex]) {
                            loaded = loadChunk(chunkInstance, syncContainer);
                            data[loadIndex] = true;
                        }

                        if (!loaded)
                            generateChunk(chunkInstance, syncContainer);
                    } finally {
                        syncContainer.release();
                        syncContainer.endWork(ChunkDataSyncContainer.WORK_LOAD);
                    }
                });
    }

    private boolean loadChunk(
            ChunkInstance chunkInstance,
            ChunkDataSyncContainer container) {
        return false; // TODO: Implement loading from disk/cache
    }

    private void generateChunk(
            ChunkInstance chunkInstance,
            ChunkDataSyncContainer container) {

        boolean success = true;
        long chunkCoordinate = chunkInstance.getCoordinate();
        SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

        for (int i = 0; i < EngineSetting.WORLD_HEIGHT; i++) {
            SubChunkInstance subChunk = subChunks[i];
            if (worldGenerationManager.generateSubChunk(chunkCoordinate, subChunk))
                continue;
            success = false;
            break;
        }

        if (success) {
            container.getData()[essentialIndex] = true;
            container.getData()[generationIndex] = true;
        }
    }
}