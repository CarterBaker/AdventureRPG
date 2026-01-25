package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkNeighborStruct;
import com.internal.bootstrap.worldpipeline.chunk.ChunkState;
import com.internal.core.engine.BranchPackage;
import com.internal.core.util.mathematics.Extras.Direction2Vector;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class AssessmentBranch extends BranchPackage {

    // Internal
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;

    // Chunk Assessment \\

    public void assessChunk(ChunkInstance chunkInstance) {

        if (!chunkInstance.tryBeginOperation(QueueOperation.NEIGHBOR_ASSESSMENT))
            return;

        // Run on render thread - no thread handoff needed
        boolean allNeighborsFound = true;
        ChunkNeighborStruct neighbors = chunkInstance.getChunkNeighbors();

        // Search for all neighbors
        for (int i = 0; i < Direction2Vector.LENGTH; i++) {

            long neighborCoordinate = neighbors.getNeighborCoordinate(i);
            ChunkInstance neighborChunk = activeChunks.get(neighborCoordinate);

            if (neighborChunk != null)
                neighbors.setNeighborChunk(i, neighborChunk);

            else
                allNeighborsFound = false;
        }

        // Update state
        if (allNeighborsFound)
            chunkInstance.setChunkState(ChunkState.HAS_NEIGHBOR_ASSIGNMENT);

        else
            chunkInstance.setChunkState(ChunkState.NEEDS_NEIGHBOR_ASSIGNMENT);
    }

    // Accessible \\

    public void setActiveChunks(Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {
        this.activeChunks = activeChunks;
    }
}