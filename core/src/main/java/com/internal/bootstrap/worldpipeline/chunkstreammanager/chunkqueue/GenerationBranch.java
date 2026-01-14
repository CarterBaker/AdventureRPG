package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.threadpipeline.ThreadSystem;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkState;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;

public class GenerationBranch extends BranchPackage {

    // Internal
    private ThreadSystem threadSystem;
    private WorldGenerationManager worldGenerationManager;

    // internal \\

    @Override
    protected void get() {

        // Internal
        this.threadSystem = get(ThreadSystem.class);
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    // Chunk Generation \\

    public void generateChunk(ChunkInstance chunkInstance) {

        // Submit to generation thread
        threadSystem.submitGeneration(() -> {

            boolean success = true;
            long chunkCoordinate = chunkInstance.getChunkCoordinate();
            SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

            // Generate all subchunks
            for (int i = 0; i < EngineSetting.WORLD_HEIGHT; i++) {

                SubChunkInstance subChunk = subChunks[i];
                if (worldGenerationManager.generateSubChunk(
                        chunkCoordinate,
                        subChunk,
                        i))
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