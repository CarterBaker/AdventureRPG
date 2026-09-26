package application.bootstrap.worldpipeline.chunk;

import java.util.Arrays;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class ChunkLockBatchStruct extends StructPackage {

    /*
     * An ordered set of chunk locks taken together. Chunks are kept sorted by
     * coordinate, so every task in the world acquires overlapping locks in
     * the same global order and two tasks can never deadlock. Locks are only
     * ever taken with tryAcquire; a batch that cannot take them all releases
     * what it holds and the work is retried on a later pass.
     */

    // Batch
    private final long[] coordinates;
    private final ChunkInstance[] chunks;
    private final ChunkDataSyncContainer[] locks;
    private int count;
    private int heldCount;

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

        return ChunkLockResult.ACQUIRED;
    }

    public void unlockAll() {

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
    }
}
