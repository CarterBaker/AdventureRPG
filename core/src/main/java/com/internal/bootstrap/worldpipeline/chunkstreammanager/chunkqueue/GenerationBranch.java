package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkState;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.ThreadHandle;
import com.internal.core.engine.settings.EngineSetting;

public class GenerationBranch extends BranchPackage {

    // Internal
    private ThreadHandle threadHandle;
    private WorldGenerationManager worldGenerationManager;

    // internal \\

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    // Chunk Generation \\

    public void generateChunk(ChunkInstance chunkInstance) {

        chunkInstance.setChunkState(ChunkState.GENERATING_DATA);

        // Submit to generation thread
        executeAsync(threadHandle, () -> {

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

            if (success) // Thread-safe state update
                chunkInstance.setChunkState(ChunkState.HAS_GENERATION_DATA);

            else // Keep it in NEEDS_GENERATION_DATA to retry
                chunkInstance.setChunkState(ChunkState.NEEDS_GENERATION_DATA);
        });
    }
}