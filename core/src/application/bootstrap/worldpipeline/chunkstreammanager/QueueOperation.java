package application.bootstrap.worldpipeline.chunkstreammanager;

enum QueueOperation {

    /*
     * Branch dispatch targets returned by
     * ChunkQueueManager.determineQueueOperation.
     * Maps each ChunkData stage to the branch responsible for executing it.
     */

    LOAD,
    ASSESSMENT,
    BUILD,
    MERGE,
    ITEM_LOAD,
    ITEM_RENDER,
    BATCH,
    RENDER,
    DUMP,
    SKIP
}