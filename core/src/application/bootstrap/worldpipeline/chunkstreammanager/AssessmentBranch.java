package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunk.ChunkNeighborStruct;
import engine.root.BranchPackage;
import engine.util.mathematics.extras.Direction2Vector;

public class AssessmentBranch extends BranchPackage {

    /*
     * Checks whether every one of a chunk's eight neighbors (the four
     * cardinal directions plus the four diagonals, matching
     * ChunkNeighborStruct) has completed GENERATION_DATA before setting
     * NEIGHBOR_DATA on the chunk. GENERATION_DATA is the correct gate here
     * — it's the stage that actually populates a subchunk's block and
     * biome palettes, and full geometry assembly reads straight into those
     * palettes across chunk borders. ESSENTIAL_DATA is not sufficient: it
     * survives a neighbor's GENERATION_DATA being dumped back off at
     * DISTANT detail, so checking it can wave through a neighbor whose
     * palettes are still sitting at their "not generated" sentinel values.
     * Neighbor check and flag write are atomic within the same lock
     * acquisition.
     */

    // Chunk Assessment \\

    public void assessChunk(ChunkInstance chunkInstance) {

        if (!chunkInstance.getChunkDataSyncContainer().setData(ChunkData.NEIGHBOR_DATA, false))
            return;

        ChunkNeighborStruct neighbors = chunkInstance.getChunkNeighbors();

        for (int i = 0; i < Direction2Vector.LENGTH; i++) {
            ChunkInstance neighborChunk = neighbors.getNeighborChunk(i);
            if (neighborChunk == null || !neighborChunk.getChunkDataSyncContainer().hasData(ChunkData.GENERATION_DATA))
                return;
        }

        chunkInstance.getChunkDataSyncContainer().setData(ChunkData.NEIGHBOR_DATA, true);
    }
}