package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunk.ChunkNeighborHandle;
import engine.root.BranchPackage;
import engine.util.mathematics.extras.Direction2Vector;

public class AssessmentBranch extends BranchPackage {

    /*
     * Sets NEIGHBOR_DATA once all eight neighbors have GENERATION_DATA, the
     * stage that fills the palettes geometry reads across borders. The check
     * and the write happen under one hold of the chunk's own lock so contention
     * cannot drop a passed check.
     */

    // Settings
    private int neighborDataIndex;

    // Internal \\

    @Override
    protected void get() {
        this.neighborDataIndex = ChunkData.NEIGHBOR_DATA.index;
    }

    // Chunk Assessment \\

    public void assessChunk(ChunkInstance chunkInstance) {

        ChunkDataSyncContainer syncContainer = chunkInstance.getChunkDataSyncContainer();

        if (!syncContainer.tryAcquire())
            return;

        try {
            ChunkNeighborHandle neighbors = chunkInstance.getChunkNeighbors();

            for (int i = 0; i < Direction2Vector.LENGTH; i++) {
                ChunkInstance neighborChunk = neighbors.getNeighborChunk(i);
                if (neighborChunk == null
                        || !neighborChunk.getChunkDataSyncContainer().hasData(ChunkData.GENERATION_DATA))
                    return;
            }

            syncContainer.getData()[neighborDataIndex] = true;
        } finally {
            syncContainer.release();
        }
    }
}