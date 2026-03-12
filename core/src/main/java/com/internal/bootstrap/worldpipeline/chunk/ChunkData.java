package com.internal.bootstrap.worldpipeline.chunk;

import com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue.QueueOperation;

public enum ChunkData {
    LOAD_DATA(ChunkDataUtility.NOT_DUMPABLE, QueueOperation.LOAD),
    ESSENTIAL_DATA(ChunkDataUtility.NOT_DUMPABLE, QueueOperation.LOAD),
    GENERATION_DATA(ChunkDataUtility.DUMP_BELOW_NEAR, QueueOperation.LOAD),
    NEIGHBOR_DATA(ChunkDataUtility.NOT_DUMPABLE, QueueOperation.ASSESSMENT),
    BUILD_DATA(ChunkDataUtility.DUMP_BELOW_NEAR, QueueOperation.BUILD),
    MERGE_DATA(ChunkDataUtility.DUMP_BELOW_IMMEDIATE, QueueOperation.MERGE),
    ITEM_DATA(ChunkDataUtility.DUMP_BELOW_IMMEDIATE, QueueOperation.ITEM_LOAD),
    ITEM_RENDER_DATA(ChunkDataUtility.DUMP_BELOW_IMMEDIATE, QueueOperation.ITEM_RENDER),
    BATCH_DATA(ChunkDataUtility.NOT_DUMPABLE, QueueOperation.BATCH),
    RENDER_DATA(ChunkDataUtility.NOT_DUMPABLE, QueueOperation.RENDER);

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
        return minimumDetailLevelRequired != ChunkDataUtility.NOT_DUMPABLE;
    }
}