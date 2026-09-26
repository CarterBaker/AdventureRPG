package application.bootstrap.worldpipeline.chunk;

import java.util.concurrent.ConcurrentLinkedQueue;

import engine.root.StructPackage;

public class ChunkLockBatchPoolStruct extends StructPackage {

    /*
     * Thread-safe pool of lock batches of one capacity. A batch is usually
     * borrowed on the main thread and returned by the worker that finishes
     * the locked task, so the pool is a concurrent queue.
     */

    // Pool
    private final ConcurrentLinkedQueue<ChunkLockBatchStruct> batches;
    private final int capacity;

    // Constructor \\

    public ChunkLockBatchPoolStruct(int capacity) {
        this.batches = new ConcurrentLinkedQueue<>();
        this.capacity = capacity;
    }

    // Pool \\

    public ChunkLockBatchStruct acquire() {

        ChunkLockBatchStruct batch = batches.poll();

        return batch != null ? batch : new ChunkLockBatchStruct(capacity);
    }

    public void release(ChunkLockBatchStruct batch) {
        batch.clear();
        batches.offer(batch);
    }
}
