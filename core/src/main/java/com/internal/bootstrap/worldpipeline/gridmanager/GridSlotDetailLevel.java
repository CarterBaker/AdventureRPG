package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;

public enum GridSlotDetailLevel {
    // Distance thresholds (percentage from center, 0.0 = center, 1.0 = edge)
    // Also enforces max chunk distance caps to prevent excessive high-detail chunks
    NONE(0, 1.00f, Integer.MAX_VALUE), // Beyond render distance - nothing
    LOADED(1, 0.80f, Integer.MAX_VALUE), // Far range - LOAD_DATA, ESSENTIAL_DATA (shell only)
    BASIC(3, 0.60f, Integer.MAX_VALUE), // Mid-far - + GENERATION_DATA (full blocks), NEIGHBOR_DATA
    BATCHED_ONLY(6, 0.40f, 32), // Mid range - + BUILD_DATA, MERGE_DATA, BATCH_DATA (mega batched) - max 32
                                // chunks
    READY_TO_SPLIT(7, 0.20f, 16), // Near range - + RENDER_DATA (can render individually) - max 16 chunks
    FULL(7, 0.00f, 8); // Closest - individual rendering, all data present - max 8 chunks

    // Internal
    public final int level;
    public final float distanceThreshold;
    public final int maxChunkDistance; // Max distance from center in chunks
    public final boolean[] requiredData;

    // Internal \\
    GridSlotDetailLevel(int level, float distanceThreshold, int maxChunkDistance) {
        // Internal
        this.level = level;
        this.distanceThreshold = distanceThreshold;
        this.maxChunkDistance = maxChunkDistance;
        this.requiredData = new boolean[ChunkData.LENGTH];
        for (ChunkData data : ChunkData.VALUES)
            requiredData[data.index] = (level >= data.minDetailLevel);
    }

    // Accessible \\
    public static GridSlotDetailLevel getDetailLevelForDistance(float distancePercent, float absoluteChunkDistance) {
        for (GridSlotDetailLevel level : values()) {
            // Skip this level if we exceed the max chunk distance cap
            if (absoluteChunkDistance > level.maxChunkDistance)
                continue;

            if (distancePercent >= level.distanceThreshold)
                return level;
        }
        return FULL;
    }

    public boolean requires(ChunkData dataType) {
        return requiredData[dataType.index];
    }

    public ChunkData getNextRequiredData(boolean[] currentData) {
        for (int i = 0; i < ChunkData.LENGTH; i++)
            if (requiredData[i] && !currentData[i])
                return ChunkData.VALUES[i];
        return null;
    }

    public ChunkData getNextDataToDump(boolean[] currentData) {
        // Scan backwards - dump highest-level data first
        for (int i = ChunkData.LENGTH - 1; i >= 0; i--) {
            // Never dump these permanent flags
            if (i == ChunkData.RENDER_DATA.index ||
                    i == ChunkData.ESSENTIAL_DATA.index)
                continue;
            // If we don't require it but chunk has it, dump it
            if (!requiredData[i] && currentData[i])
                return ChunkData.VALUES[i];
        }
        return null; // Nothing to dump
    }
}