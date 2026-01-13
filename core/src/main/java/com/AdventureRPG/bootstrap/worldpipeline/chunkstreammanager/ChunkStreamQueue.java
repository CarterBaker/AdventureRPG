package com.AdventureRPG.bootstrap.worldpipeline.chunkstreammanager;

enum ChunkStreamQueue {

    LOAD(5),
    GENERATE(4),
    ASSESSMENT(3),
    BUILD(2),
    BATCH(1),
    UNLOAD(0);

    // Internal
    final int priority;

    // Internal \\

    ChunkStreamQueue(int priority) {

        // Internal
        this.priority = priority;
    }
}
