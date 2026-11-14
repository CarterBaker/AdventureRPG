package application.bootstrap.worldpipeline.gridslot;

import application.bootstrap.worldpipeline.worldrendermanager.RenderType;

public enum GridSlotDetailLevel {

    /*
     * Defines the level of rendering detail applied to a grid slot based on
     * its distance from the active chunk. Used by WorldRenderManager to
     * select individual vs batched render paths per slot. A prioritized level
     * is the ground around the player, which the chunk and mega streams sweep
     * nearest first every frame ahead of their round robins.
     */

    IMMEDIATE(1, 8, RenderType.INDIVIDUAL, true),
    NEAR(2, 16, RenderType.BATCHED, true),
    DISTANT(3, Integer.MAX_VALUE, RenderType.BATCHED, false);

    public final int level;
    public final int maxChunkDistance;
    public final RenderType renderMode;
    public final boolean prioritized;

    GridSlotDetailLevel(int level, int maxChunkDistance, RenderType renderMode, boolean prioritized) {
        this.level = level;
        this.maxChunkDistance = maxChunkDistance;
        this.renderMode = renderMode;
        this.prioritized = prioritized;
    }

    public static GridSlotDetailLevel getDetailLevelForDistance(float absoluteChunkDistance) {

        for (GridSlotDetailLevel detailLevel : values())
            if (absoluteChunkDistance <= detailLevel.maxChunkDistance)
                return detailLevel;

        return DISTANT;
    }
}