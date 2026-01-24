package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.QueueOperation;

public enum ChunkState {

    // Create
    UNINITIALIZED(QueueOperation.GENERATE),

    // Generation
    NEEDS_GENERATION_DATA(QueueOperation.GENERATE),
    GENERATING_DATA(QueueOperation.SKIP),
    HAS_GENERATION_DATA(QueueOperation.NEIGHBOR_ASSESSMENT),

    // Neighbors
    NEEDS_NEIGHBOR_ASSIGNMENT(QueueOperation.NEIGHBOR_ASSESSMENT),
    ASSESSING_NEIGHBORS(QueueOperation.SKIP),
    HAS_NEIGHBOR_ASSIGNMENT(QueueOperation.BUILD),

    // Geometry
    NEEDS_GEOMETRY_DATA(QueueOperation.BUILD),
    GENERATING_GEOMETRY(QueueOperation.SKIP),
    HAS_GEOMETRY_DATA(QueueOperation.MERGE),

    // Merge
    NEEDS_MERGE_DATA(QueueOperation.MERGE),
    MERGING_DATA(QueueOperation.SKIP),
    HAS_MERGE_DATA(QueueOperation.BATCH),

    // Batch
    NEEDS_BATCH_DATA(QueueOperation.BATCH),
    BATCHING_DATA(QueueOperation.SKIP),
    HAS_BATCH_DATA(QueueOperation.SKIP);

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
