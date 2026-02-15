package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;

public enum GridSlotDetailLevel {

    // Distance thresholds (percentage from center, 0.0 = center, 1.0 = edge)
    NONE(0, 1.00f), // Beyond render distance - nothing
    LOADED(1, 0.80f), // Far range - just loaded/generated data
    BASIC(3, 0.60f), // Mid-far - + neighbors (trivial to keep)
    BATCHED_ONLY(6, 0.40f), // Mid range - mega batched, no individual geometry
    READY_TO_SPLIT(7, 0.20f), // Near range - has all data, can render individually
    FULL(7, 0.00f); // Closest - individual rendering, all data present

    // Internal
    public final int level;
    public final float distanceThreshold;
    public final boolean[] requiredData;

    // Internal \\

    GridSlotDetailLevel(int level, float distanceThreshold) {

        // Internal
        this.level = level;
        this.distanceThreshold = distanceThreshold;
        this.requiredData = new boolean[ChunkData.LENGTH];
        for (ChunkData data : ChunkData.VALUES)
            requiredData[data.index] = (level >= data.minDetailLevel);
    }

    // Accessible \\

    public static GridSlotDetailLevel getDetailLevelForDistance(float distancePercent) {
        for (GridSlotDetailLevel level : values())
            if (distancePercent >= level.distanceThreshold)
                return level;
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

            // Never dump RENDER_DATA flag
            if (i == ChunkData.RENDER_DATA.index)
                continue;

            // If we don't require it but chunk has it, dump it
            if (!requiredData[i] && currentData[i])
                return ChunkData.VALUES[i];
        }

        return null; // Nothing to dump
    }
}