package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.geometrypipeline.buildManager.BuildManager;
import com.internal.bootstrap.threadpipeline.ThreadSystem;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkState;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;

public class BuildBranch extends BranchPackage {

    // Internal
    private ThreadSystem threadSystem;
    private BuildManager buildManager;

    // internal \\

    @Override
    protected void get() {

        // Internal
        this.threadSystem = get(ThreadSystem.class);
        this.buildManager = get(BuildManager.class);
    }

    // Chunk Generation \\

    public void buildChunk(ChunkInstance chunkInstance) {

        // Submit to generation thread
        threadSystem.submitGeneration(() -> {

            boolean success = true;
            SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

            // Generate all subchunks
            for (int i = 0; i < EngineSetting.WORLD_HEIGHT; i++) {

                SubChunkInstance subChunk = subChunks[i];
                if (buildManager.build(
                        chunkInstance,
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
