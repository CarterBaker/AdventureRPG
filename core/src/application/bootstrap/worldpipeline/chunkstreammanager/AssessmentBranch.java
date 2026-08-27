package application.bootstrap.worldpipeline.chunkstreammanager;

import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunk.ChunkNeighborHandle;
import engine.root.BranchPackage;
import engine.util.mathematics.extras.Direction2Vector;

public class AssessmentBranch extends BranchPackage {

    /*
     * Checks whether every one of a chunk's eight neighbors (the four
     * cardinal directions plus the four diagonals, matching
     * ChunkNeighborHandle) has completed GENERATION_DATA, then sets
     * NEIGHBOR_DATA on the chunk. GENERATION_DATA is the correct gate here
     * — it's the stage that actually populates a subchunk's block and
     * biome palettes, and full geometry assembly reads straight into those
     * palettes across chunk borders; ESSENTIAL_DATA survives a neighbor's
     * GENERATION_DATA being dumped back off at DISTANT detail, so checking
     * it can wave through a neighbor whose palettes are still sitting at
     * their "not generated" sentinel values. The whole check-then-write
     * runs under a single acquisition of this chunk's own lock, held for
     * the entire call rather than released and reacquired between the
     * neighbor scan and the final write — another chunk's build batch can
     * legitimately hold this same lock for the full duration of its own
     * geometry pass (see BuildBranch), and a released-and-reacquired write
     * would silently lose an already-passed check to that contention,
     * leaving NEIGHBOR_DATA false with nothing left to promptly retry it.
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