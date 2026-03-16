package com.internal.bootstrap.worldpipeline.chunkstreammanager;

enum ChunkQueueItem {

    /*
     * Queue slots cycled by ChunkQueueManager each frame. Drives the three
     * phases of the chunk pipeline in order — scan, load, assess.
     */

    SCAN_GRID_SLOTS,
    LOAD,
    ASSESS_ACTIVE_CHUNKS
}