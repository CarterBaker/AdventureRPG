package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.QueueOperation;

public enum ChunkState {

    // Create
    UNINITIALIZED(QueueOperation.GENERATE),

    // Generation
    NEEDS_GENERATION_DATA(QueueOperation.GENERATE),
    HAS_GENERATION_DATA(QueueOperation.NEIGHBOR_ASSESSMENT),

    // Neighbors
    NEEDS_NEIGHBOR_ASSIGNMENT(QueueOperation.NEIGHBOR_ASSESSMENT),
    HAS_NEIGHBOR_ASSIGNMENT(QueueOperation.BUILD);

    // Internal
    private final QueueOperation associatedOperation;

    // Internal \\

    ChunkState(QueueOperation associatedOperation) {

        // Internal
        this.associatedOperation = associatedOperation;
    }

    // Accessible \\

    public QueueOperation getAssociatedOperation() {
        return associatedOperation;
    }
}
