package application.bootstrap.worldpipeline.structure;

import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class StructureSpawnData extends DataPackage {

    /*
     * Procedural spawn rule for one structure definition. The world is cut
     * into a grid of spacing-sized cells per definition; each cell rolls
     * frequency once and, on success, places one candidate at a jittered
     * position inside it. The candidate survives only if the dominant biome
     * there is one of the allowed biomes (an empty set allows every biome),
     * the ground under its footprint is flat enough, the height sits inside
     * [minHeight, maxHeight], and it does not sit in open water unless
     * allowWater. minDepth/maxDepth only apply to UNDERGROUND structures and
     * give the range the burial depth is rolled from.
     */

    private final ObjectOpenHashSet<String> biomeNames;
    private final float frequency;
    private final int spacingBlocks;
    private final int minHeightBlocks;
    private final int maxHeightBlocks;
    private final int minDepthBlocks;
    private final int maxDepthBlocks;
    private final int maxSlopeBlocks;
    private final boolean allowWater;

    public StructureSpawnData(
            ObjectOpenHashSet<String> biomeNames,
            float frequency,
            int spacingBlocks,
            int minHeightBlocks,
            int maxHeightBlocks,
            int minDepthBlocks,
            int maxDepthBlocks,
            int maxSlopeBlocks,
            boolean allowWater) {

        this.biomeNames = biomeNames;
        this.frequency = frequency;
        this.spacingBlocks = spacingBlocks;
        this.minHeightBlocks = minHeightBlocks;
        this.maxHeightBlocks = maxHeightBlocks;
        this.minDepthBlocks = minDepthBlocks;
        this.maxDepthBlocks = maxDepthBlocks;
        this.maxSlopeBlocks = maxSlopeBlocks;
        this.allowWater = allowWater;
    }

    public boolean allowsBiome(String biomeName) {
        return biomeNames.isEmpty() || biomeNames.contains(biomeName);
    }

    public ObjectOpenHashSet<String> getBiomeNames() {
        return biomeNames;
    }

    public float getFrequency() {
        return frequency;
    }

    public int getSpacingBlocks() {
        return spacingBlocks;
    }

    public int getMinHeightBlocks() {
        return minHeightBlocks;
    }

    public int getMaxHeightBlocks() {
        return maxHeightBlocks;
    }

    public int getMinDepthBlocks() {
        return minDepthBlocks;
    }

    public int getMaxDepthBlocks() {
        return maxDepthBlocks;
    }

    public int getMaxSlopeBlocks() {
        return maxSlopeBlocks;
    }

    public boolean allowsWater() {
        return allowWater;
    }
}
