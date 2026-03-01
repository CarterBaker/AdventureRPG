package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.QueueOperation;

public enum ChunkData {
    LOAD_DATA(0, QueueOperation.LOAD),
    ESSENTIAL_DATA(0, QueueOperation.LOAD),
    GENERATION_DATA(3, QueueOperation.LOAD),
    NEIGHBOR_DATA(0, QueueOperation.ASSESSMENT),
    BUILD_DATA(2, QueueOperation.BUILD),
    MERGE_DATA(2, QueueOperation.MERGE),
    BATCH_DATA(0, QueueOperation.BATCH),
    RENDER_DATA(0, QueueOperation.RENDER);

    public static final int NOT_DUMPABLE = 0;
    public static final int DUMP_BELOW_DISTANT = 3;
    public static final int DUMP_BELOW_NEAR = 2;

    public final int index;
    public final int minimumDetailLevelRequired;
    public final QueueOperation queueOperation;

    public static final ChunkData[] VALUES = values();
    public static final int LENGTH = values().length;

    ChunkData(int minimumDetailLevelRequired, QueueOperation queueOperation) {
        this.index = this.ordinal();
        this.minimumDetailLevelRequired = minimumDetailLevelRequired;
        this.queueOperation = queueOperation;
    }

    public boolean isDumpable() {
        return minimumDetailLevelRequired != NOT_DUMPABLE;
    }
}