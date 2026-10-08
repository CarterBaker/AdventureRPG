package application.bootstrap.worldpipeline.biome;

import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.registry.RegistryUtility;

public class ProbableBiomeStruct extends StructPackage {

    /*
     * One biome chained into a parent: its placement, chance, patch size and
     * arm ranges, and core scales when centered. Coverage is the part of what
     * earlier entries leave that keeps the chance a share of the whole; past
     * the inverted coverage the biome becomes the ground and its patches the
     * parent's pockets. Scatter density and cell size are resolved once here.
     */

    private final String biomeName;
    private final short biomeID;
    private final ProbableBiomePlacement placement;
    private final float chance;
    private final float coverage;
    private final float minSizeBlocks;
    private final float maxSizeBlocks;
    private final int minArms;
    private final int maxArms;
    private final float minCoreScale;
    private final float maxCoreScale;

    private final double sizeLogRatio;
    private final boolean inverted;
    private final double patchDensity;
    private final double cellSizeBlocks;

    public ProbableBiomeStruct(
            String biomeName,
            ProbableBiomePlacement placement,
            float chance,
            float coverage,
            float minSizeBlocks,
            float maxSizeBlocks,
            int minArms,
            int maxArms,
            float minCoreScale,
            float maxCoreScale) {

        this.biomeName = biomeName;
        this.biomeID = RegistryUtility.toShortID(biomeName);
        this.placement = placement;
        this.chance = chance;
        this.coverage = coverage;
        this.minSizeBlocks = minSizeBlocks;
        this.maxSizeBlocks = maxSizeBlocks;
        this.minArms = minArms;
        this.maxArms = maxArms;
        this.minCoreScale = minCoreScale;
        this.maxCoreScale = maxCoreScale;

        this.sizeLogRatio = Math.log((double) maxSizeBlocks / minSizeBlocks);
        this.inverted = coverage > EngineSetting.BIOME_PROBABLE_INVERTED_COVERAGE;

        double meanPatchAreaBlocks = BiomeFieldUtility.computeMeanPatchAreaBlocks(
                minSizeBlocks, maxSizeBlocks, minArms, maxArms);

        this.patchDensity = BiomeFieldUtility.computePatchDensity(
                inverted ? 1f - coverage : coverage, meanPatchAreaBlocks);
        this.cellSizeBlocks = BiomeFieldUtility.computeScatterCellSizeBlocks(patchDensity, maxSizeBlocks);
    }

    public String getBiomeName() {
        return biomeName;
    }

    public short getBiomeID() {
        return biomeID;
    }

    public ProbableBiomePlacement getPlacement() {
        return placement;
    }

    public boolean isCentered() {
        return placement == ProbableBiomePlacement.CENTER;
    }

    public float getChance() {
        return chance;
    }

    public float getCoverage() {
        return coverage;
    }

    public float getMinSizeBlocks() {
        return minSizeBlocks;
    }

    public float getMaxSizeBlocks() {
        return maxSizeBlocks;
    }

    public int getMinArms() {
        return minArms;
    }

    public int getMaxArms() {
        return maxArms;
    }

    public float getMinCoreScale() {
        return minCoreScale;
    }

    public float getMaxCoreScale() {
        return maxCoreScale;
    }

    public double getSizeLogRatio() {
        return sizeLogRatio;
    }

    public boolean isInverted() {
        return inverted;
    }

    public double getPatchDensity() {
        return patchDensity;
    }

    public double getCellSizeBlocks() {
        return cellSizeBlocks;
    }
}
