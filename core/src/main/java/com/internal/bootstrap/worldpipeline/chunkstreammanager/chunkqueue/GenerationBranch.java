package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.ThreadHandle;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.kernel.threadmanager.InternalThreadManager.SyncStructConsumer;

public class GenerationBranch extends BranchPackage {

    // Internal
    private ThreadHandle threadHandle;
    private WorldGenerationManager worldGenerationManager;

    private int loadIndex;
    private int generationIndex;

    // internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.worldGenerationManager = get(WorldGenerationManager.class);

        this.loadIndex = ChunkData.LOAD_DATA.index;
        this.generationIndex = ChunkData.GENERATION_DATA.index;
    }

    // Chunk Creation \\

    public void getNewChunk(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        // Single executeAsync that handles both load and generation
        executeAsync(
                threadHandle,
                syncContainer,
                (SyncStructConsumer<ChunkDataSyncContainer>) container -> {

                    boolean loaded = false;

                    // Check if already attempted
                    if (!container.data[loadIndex]) {

                        loaded = loadChunk(
                                chunkInstance,
                                container);

                        container.data[loadIndex] = true;
                    }

                    if (!loaded)
                        generateChunk(
                                chunkInstance,
                                container);
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

        // Generate all subchunks
        for (int i = 0; i < EngineSetting.WORLD_HEIGHT; i++) {

            SubChunkInstance subChunk = subChunks[i];

            if (worldGenerationManager.generateSubChunk(
                    chunkCoordinate,
                    subChunk))
                continue;

            success = false;
            break;
        }

        // Thread-safe state update via sync container
        container.data[generationIndex] = success;
    }
}