package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.worldpipeline.worldrendermanager.RenderType;

public enum GridSlotDetailLevel {

    IMMEDIATE(1, 8, RenderType.INDIVIDUAL),
    NEAR(2, 16, RenderType.BATCHED),
    DISTANT(3, Integer.MAX_VALUE, RenderType.BATCHED);

    public final int level;
    public final int maxChunkDistance;
    public final RenderType renderMode;

    GridSlotDetailLevel(int level, int maxChunkDistance, RenderType renderMode) {
        this.level = level;
        this.maxChunkDistance = maxChunkDistance;
        this.renderMode = renderMode;
    }

    public static GridSlotDetailLevel getDetailLevelForDistance(float absoluteChunkDistance) {
        for (GridSlotDetailLevel detailLevel : values())
            if (absoluteChunkDistance <= detailLevel.maxChunkDistance)
                return detailLevel;
        return DISTANT;
    }
}