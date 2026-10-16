package application.bootstrap.worldpipeline.biome;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.registry.RegistryUtility;

public class BiomeVeinStruct extends StructPackage {

    /*
     * One vein threading a biome's ground: the content it sets into every
     * block it runs through that can hold it — an ore through rock, stones or
     * roots through soil — how much of the ground its seams reach, how thick
     * they run, the band of heights it lies in and how deep below the ground
     * it may run. The vein field is seeded by the content, so the same ore
     * runs on unbroken across every biome that carries it.
     */

    private final String contentName;
    private final long fieldSeed;
    private final float abundance;
    private final float thicknessBlocks;
    private final int minHeightBlocks;
    private final int maxHeightBlocks;
    private final int maxDepthBlocks;

    public BiomeVeinStruct(
            String contentName,
            float abundance,
            float thicknessBlocks,
            int minHeightBlocks,
            int maxHeightBlocks,
            int maxDepthBlocks) {

        this.contentName = contentName;
        this.fieldSeed = RegistryUtility.toNameSeed(contentName) * EngineSetting.HASH_FINALIZER_MULTIPLIER_1;
        this.abundance = abundance;
        this.thicknessBlocks = thicknessBlocks;
        this.minHeightBlocks = minHeightBlocks;
        this.maxHeightBlocks = maxHeightBlocks;
        this.maxDepthBlocks = maxDepthBlocks;
    }

    // Accessible \\

    public String getContentName() {
        return contentName;
    }

    public long getFieldSeed() {
        return fieldSeed;
    }

    public float getAbundance() {
        return abundance;
    }

    public float getThicknessBlocks() {
        return thicknessBlocks;
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
