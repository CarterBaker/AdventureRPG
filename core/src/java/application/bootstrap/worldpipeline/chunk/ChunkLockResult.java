package application.bootstrap.worldpipeline.chunk;

public enum ChunkLockResult {

    /*
     * Outcome of ChunkLockBatchStruct.tryLockAll(). BUSY means another task
     * holds one of the locks, STALE means a chunk was reassigned to another
     * coordinate or lacks the required data since the batch was collected.
     */

    ACQUIRED,
    BUSY,
    STALE
}
