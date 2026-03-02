package com.internal.bootstrap.worldpipeline.megachunk;

import com.internal.bootstrap.worldpipeline.megastreammanager.megaqueue.MegaQueueOperation;

public enum MegaData {
    BATCH_DATA(MegaQueueOperation.ASSESS),
    RENDER_DATA(MegaQueueOperation.RENDER);

    public final int index;
    public final MegaQueueOperation queueOperation;

    public static final MegaData[] VALUES = values();
    public static final int LENGTH = values().length;

    MegaData(MegaQueueOperation queueOperation) {
        this.index = this.ordinal();
        this.queueOperation = queueOperation;
    }
}