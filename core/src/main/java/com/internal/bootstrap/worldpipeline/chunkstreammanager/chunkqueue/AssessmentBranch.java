package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkNeighborStruct;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.core.engine.BranchPackage;
import com.internal.core.util.mathematics.Extras.Direction2Vector;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class AssessmentBranch extends BranchPackage {

    // Internal
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;

    // Chunk Assessment \\

    public void assessChunk(ChunkInstance chunkInstance) {

        if (!chunkInstance.getChunkDataSyncContainer().setData(ChunkData.NEIGHBOR_DATA, false))
            return; // Locked, skip

        boolean allNeighborsFound = true;
        ChunkNeighborStruct neighbors = chunkInstance.getChunkNeighbors();

        for (int i = 0; i < Direction2Vector.LENGTH; i++) {

            long neighborCoordinate = neighbors.getNeighborCoordinate(i);
            ChunkInstance neighborChunk = activeChunks.get(neighborCoordinate);

            if (neighborChunk != null && neighborChunk.getChunkDataSyncContainer().hasData(ChunkData.GENERATION_DATA))
                neighbors.setNeighborChunk(i, neighborChunk);
            else
                allNeighborsFound = false;
        }

        if (allNeighborsFound)
            chunkInstance.getChunkDataSyncContainer().setData(ChunkData.NEIGHBOR_DATA, true);
    }

    // Accessible \\

    public void setActiveChunks(Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {
        this.activeChunks = activeChunks;
    }
}