package application.bootstrap.worldpipeline.chunk;

import java.util.Arrays;

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
     * coordinate this chunk is currently representing. While a lock batch
     * holds the chunk's neighborhood it binds the neighbors it locked here,
     * so a geometry build reads exactly the chunks it holds and checked
     * rather than the active map the main thread changes beneath it.
     */

    // Internal
    private long[] neighborCoordinates;
    private Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks;

    // Locked — the neighbors a lock batch holds, by direction, bound only while it holds every lock
    private ChunkInstance[] lockedNeighborChunks;
    private boolean locked;

    // Internal \\

    @Override
    protected void create() {
        this.neighborCoordinates = new long[Direction2Vector.LENGTH];
        this.lockedNeighborChunks = new ChunkInstance[Direction2Vector.LENGTH];
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

    // Lock \\

    // Under the batch's locks — each direction bound to the chunk the batch holds at that coordinate
    void bindLockedNeighbors(ChunkInstance[] batchChunks, int batchCount) {

        for (int i = 0; i < Direction2Vector.LENGTH; i++)
            lockedNeighborChunks[i] = findChunk(batchChunks, batchCount, neighborCoordinates[i]);

        this.locked = true;
    }

    // Under the batch's locks, before they are released
    void unbindLockedNeighbors() {
        Arrays.fill(lockedNeighborChunks, null);
        this.locked = false;
    }

    private ChunkInstance findChunk(ChunkInstance[] chunks, int count, long coordinate) {

        for (int i = 0; i < count; i++)
            if (chunks[i].getCoordinate() == coordinate)
                return chunks[i];

        return null;
    }

    // Accessible \\

    public long getNeighborCoordinate(int direction2VectorIndex) {
        return neighborCoordinates[direction2VectorIndex];
    }

    public ChunkInstance getNeighborChunk(int direction2VectorIndex) {
        return activeChunks.get(neighborCoordinates[direction2VectorIndex]);
    }

    // The neighbor a geometry build reads: the one its lock batch holds, or the active one for a main-thread
    // build that holds no batch
    public ChunkInstance getBuildNeighborChunk(int direction2VectorIndex) {
        return locked ? lockedNeighborChunks[direction2VectorIndex] : getNeighborChunk(direction2VectorIndex);
    }
}