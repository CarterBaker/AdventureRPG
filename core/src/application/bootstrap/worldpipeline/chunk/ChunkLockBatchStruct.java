package application.bootstrap.worldpipeline.chunk;

import java.util.Arrays;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.extras.Direction2Vector;

public class ChunkLockBatchStruct extends StructPackage {

    /*
     * An ordered set of chunk locks taken together. Chunks are kept sorted by
     * coordinate, so every task in the world acquires overlapping locks in
     * the same global order and two tasks can never deadlock. Locks are only
     * ever taken with tryAcquire; a batch that cannot take them all releases
     * what it holds and the work is retried on a later pass. addNeighborhood()
     * gathers a chunk with its eight neighbors, the set any geometry build
     * reads, and binds them to the chunk's neighbor handle while every lock
     * is held, so the build reads the very chunks this batch checked.
     */

    // Batch
    private final long[] coordinates;
    private final ChunkInstance[] chunks;
    private final ChunkDataSyncContainer[] locks;
    private int count;
    private int heldCount;

    // Neighborhood — the chunk whose neighbors this batch binds while it holds them
    private ChunkInstance neighborhoodChunk;
    private boolean neighborsBound;

    // Constructor \\

    public ChunkLockBatchStruct(int capacity) {
        this.coordinates = new long[capacity];
        this.chunks = new ChunkInstance[capacity];
        this.locks = new ChunkDataSyncContainer[capacity];
    }

    // Collect \\

    public void add(ChunkInstance chunk) {

        long coordinate = chunk.getCoordinate();
        int index = count - 1;

        for (int i = 0; i < count; i++)
            if (coordinates[i] == coordinate)
                return;

        while (index >= 0 && coordinates[index] > coordinate) {
            coordinates[index + 1] = coordinates[index];
            chunks[index + 1] = chunks[index];
            index--;
        }

        coordinates[index + 1] = coordinate;
        chunks[index + 1] = chunk;
        count++;
    }

    // A chunk and its eight neighbors — false when any neighbor is not loaded
    public boolean addNeighborhood(ChunkInstance chunk) {

        ChunkNeighborHandle neighbors = chunk.getChunkNeighbors();

        add(chunk);

        for (int i = 0; i < Direction2Vector.LENGTH; i++) {

            ChunkInstance neighborChunk = neighbors.getNeighborChunk(i);

            if (neighborChunk == null)
                return false;

            add(neighborChunk);
        }

        this.neighborhoodChunk = chunk;
        return true;
    }

    public boolean isFull() {
        return count == coordinates.length;
    }

    // Lock \\

    public ChunkLockResult tryLockAll() {
        return tryLockAll(EngineSetting.INDEX_NOT_FOUND);
    }

    public ChunkLockResult tryLockAll(int requiredDataIndex) {

        for (int i = 0; i < count; i++) {

            ChunkDataSyncContainer lock = chunks[i].getChunkDataSyncContainer();
            locks[i] = lock;

            if (!lock.tryAcquire())
                return ChunkLockResult.BUSY;

            heldCount = i + 1;

            if (chunks[i].getCoordinate() != coordinates[i])
                return ChunkLockResult.STALE;

            if (requiredDataIndex != EngineSetting.INDEX_NOT_FOUND && !lock.getData()[requiredDataIndex])
                return ChunkLockResult.STALE;
        }

        bindNeighbors();
        return ChunkLockResult.ACQUIRED;
    }

    private void bindNeighbors() {

        if (neighborhoodChunk == null)
            return;

        neighborhoodChunk.getChunkNeighbors().bindLockedNeighbors(chunks, count);
        this.neighborsBound = true;
    }

    public void unlockAll() {

        if (neighborsBound) {
            neighborhoodChunk.getChunkNeighbors().unbindLockedNeighbors();
            this.neighborsBound = false;
        }

        for (int i = 0; i < heldCount; i++)
            locks[i].release();

        heldCount = 0;
    }

    // Reset \\

    public void clear() {
        Arrays.fill(chunks, null);
        Arrays.fill(locks, null);
        count = 0;
        heldCount = 0;
        neighborhoodChunk = null;
        neighborsBound = false;
    }
}
