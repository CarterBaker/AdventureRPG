package application.bootstrap.worldpipeline.biome;

import engine.root.StructPackage;

public class BiomeCaveBiomeStruct extends StructPackage {

    /*
     * One cave biome a biome may hold beneath it: the cave biome, the share of
     * the underground regions below the biome it lines, the band of heights
     * it lies in and how far below the ground it may reach.
     */

    private final String caveBiomeName;
    private final float chance;
    private final int minHeightBlocks;
    private final int maxHeightBlocks;
    private final int maxDepthBlocks;

    public BiomeCaveBiomeStruct(
            String caveBiomeName,
            float chance,
            int minHeightBlocks,
            int maxHeightBlocks,
            int maxDepthBlocks) {

        this.caveBiomeName = caveBiomeName;
        this.chance = chance;
        this.minHeightBlocks = minHeightBlocks;
        this.maxHeightBlocks = maxHeightBlocks;
        this.maxDepthBlocks = maxDepthBlocks;
    }

    // Accessible \\

    public String getCaveBiomeName() {
        return caveBiomeName;
    }

    public float getChance() {
        return chance;
    }

    public int getMinHeightBlocks() {
        return minHeightBlocks;
    }

    public int getMaxHeightBlocks() {
        return maxHeightBlocks;
    }

    public int getMaxDepthBlocks() {
        return maxDepthBlocks;
    }
}
