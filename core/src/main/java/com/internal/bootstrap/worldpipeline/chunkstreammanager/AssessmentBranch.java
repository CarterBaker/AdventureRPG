package com.internal.bootstrap.worldpipeline.chunkstreammanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkNeighborStruct;
import com.internal.core.engine.BranchPackage;
import com.internal.core.util.mathematics.extras.Direction2Vector;

public class AssessmentBranch extends BranchPackage {

    /*
     * Checks whether all four horizontal neighbors have ESSENTIAL_DATA before
     * setting NEIGHBOR_DATA on the chunk. Neighbor check and flag write are
     * atomic within the same lock acquisition.
     */

    // Chunk Assessment \\

    public void assessChunk(ChunkInstance chunkInstance) {

        if (!chunkInstance.getChunkDataSyncContainer().setData(ChunkData.NEIGHBOR_DATA, false))
            return;

        ChunkNeighborStruct neighbors = chunkInstance.getChunkNeighbors();

        for (int i = 0; i < Direction2Vector.LENGTH; i++) {
            ChunkInstance neighborChunk = neighbors.getNeighborChunk(i);
            if (neighborChunk == null || !neighborChunk.getChunkDataSyncContainer().hasData(ChunkData.ESSENTIAL_DATA))
                return;
        }

        chunkInstance.getChunkDataSyncContainer().setData(ChunkData.NEIGHBOR_DATA, true);
    }
}