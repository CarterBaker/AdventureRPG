package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.worldrendersystem.RenderType;

public enum GridSlotDetailLevel {

    DISTANT(3, Integer.MAX_VALUE, RenderType.BATCHED),
    NEAR(2, 16, RenderType.BATCHED),
    IMMEDIATE(1, 8, RenderType.INDIVIDUAL);

    public final int level;
    public final int maxChunkDistance;
    public final RenderType renderMode;

    private boolean[] requiredDataCache;

    GridSlotDetailLevel(int level, int maxChunkDistance, RenderType renderMode) {
        this.level = level;
        this.maxChunkDistance = maxChunkDistance;
        this.renderMode = renderMode;
    }

    public static GridSlotDetailLevel getDetailLevelForDistance(float absoluteChunkDistance) {
        for (GridSlotDetailLevel detailLevel : values()) {
            if (absoluteChunkDistance > detailLevel.maxChunkDistance)
                return detailLevel;
        }
        return IMMEDIATE;
    }

    private boolean[] computeRequiredData() {
        boolean[] required = new boolean[ChunkData.LENGTH];

        for (ChunkData data : ChunkData.VALUES) {

            if (!data.isDumpable()) {
                required[data.index] = true;
                continue;
            }

            required[data.index] = data.minimumDetailLevelRequired >= this.level;
        }

        return required;
    }

    public boolean[] getRequiredData() {
        if (requiredDataCache == null)
            requiredDataCache = computeRequiredData();
        return requiredDataCache;
    }

    public ChunkData getNextRequiredData(boolean[] currentData) {
        boolean[] required = getRequiredData();

        for (int i = 0; i < ChunkData.LENGTH; i++) {
            if (required[i] && !currentData[i])
                return ChunkData.VALUES[i];
        }

        return null;
    }

    public ChunkData getNextDataToDump(boolean[] currentData) {
        boolean[] required = getRequiredData();

        for (int i = ChunkData.LENGTH - 1; i >= 0; i--) {
            ChunkData data = ChunkData.VALUES[i];

            if (!data.isDumpable())
                continue;

            if (!required[i] && currentData[i])
                return data;
        }

        return null;
    }
}