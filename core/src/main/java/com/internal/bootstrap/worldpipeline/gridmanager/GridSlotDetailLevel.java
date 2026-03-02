package com.internal.bootstrap.worldpipeline.gridmanager;

import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.worldrendermanager.RenderType;

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
        GridSlotDetailLevel[] levels = values();
        for (int i = levels.length - 1; i >= 0; i--) {
            if (absoluteChunkDistance <= levels[i].maxChunkDistance)
                return levels[i];
        }
        return levels[0];
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

    /*
     * Scans forward through ChunkData in pipeline order and returns the
     * first stage that is not yet satisfied. Order is the contract —
     * no stage can be reached without the previous one being complete.
     */
    public ChunkData getNextRequiredData(boolean[] currentData) {
        for (int i = 0; i < ChunkData.LENGTH; i++) {
            if (!currentData[i])
                return ChunkData.VALUES[i];
        }
        return null;
    }

    /*
     * Scans backward through ChunkData and returns the first stage that
     * is set, dumpable, and not required at this detail level.
     * Only fires after the full forward pipeline has completed.
     */
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