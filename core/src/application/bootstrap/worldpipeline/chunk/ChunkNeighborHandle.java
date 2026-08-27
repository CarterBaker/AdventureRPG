package application.bootstrap.worldpipeline.chunk;

import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.HandlePackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Direction2Vector;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class ChunkNeighborHandle extends HandlePackage {

    /*
     * Pre-computed neighbor coordinates and a shared reference to the active
     * chunk map for fast neighbor lookups during assessment. Owned by its
     * ChunkInstance for the pooled object's whole lifetime and reconfigured
     * in place via reconfigure() on every reuse — the backing array is
     * never reallocated, and coordinates are re-wrapped fresh each time so
     * world boundary handling stays correct no matter which grid or
     * coordinate this chunk is currently representing.
     */

    // Internal
    private long[] neighborCoordinates;
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;

    // Internal \\

    @Override
    protected void create() {
        this.neighborCoordinates = new long[Direction2Vector.LENGTH];
    }

    // Reconfigure \\

    public void reconfigure(
            long chunkCoordinate,
            ChunkInstance chunkInstance,
            Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks) {

        this.activeChunks = activeChunks;

        for (byte i = 0; i < Direction2Vector.LENGTH; i++) {
            long neighborCoordinate = Coordinate2Long.getNeighbor(
                    chunkCoordinate,
                    Direction2Vector.VALUES[i]);
            neighborCoordinates[i] = WorldWrapUtility.wrapAroundWorld(
                    chunkInstance.getWorldHandle(),
                    neighborCoordinate);
        }
    }

    // Accessible \\

    public long getNeighborCoordinate(int direction2VectorIndex) {
        return neighborCoordinates[direction2VectorIndex];
    }

    public ChunkInstance getNeighborChunk(int direction2VectorIndex) {
        return activeChunks.get(neighborCoordinates[direction2VectorIndex]);
    }
}