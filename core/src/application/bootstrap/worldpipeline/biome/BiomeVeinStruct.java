package application.bootstrap.worldpipeline.biome;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.registry.RegistryUtility;

public class BiomeVeinStruct extends StructPackage {

    /*
     * One mineral vein threading a biome's rock: the block it is made of, how
     * much of the rock its seams reach, how thick they run, the band of
     * heights it lies in and how deep below the ground it may run. The vein
     * field is seeded by the block, so the same ore runs on unbroken across
     * every biome that carries it.
     */

    private final String blockName;
    private final long fieldSeed;
    private final float abundance;
    private final float thicknessBlocks;
    private final int minHeightBlocks;
    private final int maxHeightBlocks;
    private final int maxDepthBlocks;

    public BiomeVeinStruct(
            String blockName,
            float abundance,
            float thicknessBlocks,
            int minHeightBlocks,
            int maxHeightBlocks,
            int maxDepthBlocks) {

        this.blockName = blockName;
        this.fieldSeed = RegistryUtility.toShortID(blockName) * EngineSetting.HASH_FINALIZER_MULTIPLIER_1;
        this.abundance = abundance;
        this.thicknessBlocks = thicknessBlocks;
        this.minHeightBlocks = minHeightBlocks;
        this.maxHeightBlocks = maxHeightBlocks;
        this.maxDepthBlocks = maxDepthBlocks;
    }

    // Accessible \\

    public String getBlockName() {
        return blockName;
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
