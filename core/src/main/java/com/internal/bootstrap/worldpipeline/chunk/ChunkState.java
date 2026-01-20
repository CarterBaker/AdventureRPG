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
    HAS_NEIGHBOR_ASSIGNMENT(QueueOperation.BUILD),

    // Geometry
    NEEDS_GEOMETRY_ASSIGNMENT(QueueOperation.BUILD),
    HAS_GEOMETRY_ASSIGNMENT(QueueOperation.MERGE),

    // Merge
    NEEDS_MERGE_DATA(QueueOperation.MERGE),
    HAS_MERGE_DATA(QueueOperation.BATCH),

    // Batch
    NEEDS_BATCH_DATA(QueueOperation.BATCH),
    HAS_BATCH_DATA(QueueOperation.FINALIZE);

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
