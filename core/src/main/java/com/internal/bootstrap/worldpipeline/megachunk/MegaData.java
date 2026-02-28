package com.internal.bootstrap.worldpipeline.megachunk;

public enum MegaData {
    BATCH_DATA,
    MERGE_DATA,
    RENDER_DATA;

    public final int index;
    public static final MegaData[] VALUES = values();
    public static final int LENGTH = values().length;

    MegaData() {
        this.index = this.ordinal();
    }
}