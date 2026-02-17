package com.internal.bootstrap.worldpipeline.chunk;

public enum ChunkData {
    LOAD_DATA(1), // Level 1: Try to load from disk
    ESSENTIAL_DATA(1), // Level 1: Permanent marker - has at least shell data (never cleared once set)
    GENERATION_DATA(2), // Level 2: Has full block data (generated or loaded)
    NEIGHBOR_DATA(3), // Level 3: Has neighbor references
    BUILD_DATA(4), // Level 4: CPU-side geometry (dump when far)
    MERGE_DATA(5), // Level 5: Merged subchunks (dump when far)
    BATCH_DATA(6), // Level 6: GPU batched into mega (mid-range)
    RENDER_DATA(7); // Level 7: Actually rendering (never dump this flag)

    // Internal
    public final int index;
    public final int minDetailLevel;
    public static final ChunkData[] VALUES = values();
    public static final int LENGTH = values().length;

    // Internal \\

    ChunkData(int minDetailLevel) {
        // Internal
        this.index = this.ordinal();
        this.minDetailLevel = minDetailLevel;
    }
}