package com.internal.bootstrap.worldpipeline.chunk;

public enum ChunkData {

    LOAD_DATA(0),
    ESSENTIAL_DATA(0),
    GENERATION_DATA(3),
    NEIGHBOR_DATA(0),
    BUILD_DATA(2),
    MERGE_DATA(2),
    BATCH_DATA(0),
    RENDER_DATA(0);

    public static final int NOT_DUMPABLE = 0;
    public static final int DUMP_BELOW_DISTANT = 3;
    public static final int DUMP_BELOW_NEAR = 2;

    public final int index;
    public final int minimumDetailLevelRequired;

    public static final ChunkData[] VALUES = values();
    public static final int LENGTH = values().length;

    ChunkData(int minimumDetailLevelRequired) {
        this.index = this.ordinal();
        this.minimumDetailLevelRequired = minimumDetailLevelRequired;
    }

    public boolean isDumpable() {
        return minimumDetailLevelRequired != NOT_DUMPABLE;
    }
}