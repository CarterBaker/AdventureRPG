package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkNeighborStruct;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.core.engine.BranchPackage;
import com.internal.core.util.mathematics.Extras.Direction2Vector;

public class AssessmentBranch extends BranchPackage {

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