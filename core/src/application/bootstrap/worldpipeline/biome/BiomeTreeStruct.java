package application.bootstrap.worldpipeline.biome;

import application.bootstrap.worldpipeline.tree.TreeDistribution;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.registry.RegistryUtility;

public class BiomeTreeStruct extends StructPackage {

    /*
     * One kind of tree a biome grows and how it spreads them: the tree, its
     * distribution, the cell spacing and the chance each cell rolls, a grove's
     * radius and the range of trees in it, and for a field the wavelength of
     * the patches that thin it into woods and clearings and the share of the
     * ground those woods cover. Every roll is salted by the tree's name, so
     * the same tree spreads the same way wherever it is linked.
     */

    private final String treeName;
    private final int nameSeed;
    private final TreeDistribution distribution;
    private final int spacingBlocks;
    private final float chance;
    private final float clusterRadiusBlocks;
    private final int minClusterTrees;
    private final int maxClusterTrees;
    private final float patchWavelengthBlocks;
    private final float patchCoverage;

    public BiomeTreeStruct(
            String treeName,
            TreeDistribution distribution,
            int spacingBlocks,
            float chance,
            float clusterRadiusBlocks,
            int minClusterTrees,
            int maxClusterTrees,
            float patchWavelengthBlocks,
            float patchCoverage) {

        this.treeName = treeName;
        this.nameSeed = RegistryUtility.toNameSeed(treeName);
        this.distribution = distribution;
        this.spacingBlocks = spacingBlocks;
        this.chance = chance;
        this.clusterRadiusBlocks = clusterRadiusBlocks;
        this.minClusterTrees = minClusterTrees;
        this.maxClusterTrees = maxClusterTrees;
        this.patchWavelengthBlocks = patchWavelengthBlocks;
        this.patchCoverage = patchCoverage;
    }

    // Accessible \\

    public String getTreeName() {
        return treeName;
    }

    public int getNameSeed() {
        return nameSeed;
    }

    public TreeDistribution getDistribution() {
        return distribution;
    }

    public int getSpacingBlocks() {
        return spacingBlocks;
    }

    public float getChance() {
        return chance;
    }

    public float getClusterRadiusBlocks() {
        return clusterRadiusBlocks;
    }

    public int getMinClusterTrees() {
        return minClusterTrees;
    }

    public int getMaxClusterTrees() {
        return maxClusterTrees;
    }

    public float getPatchWavelengthBlocks() {
        return patchWavelengthBlocks;
    }

    public float getPatchCoverage() {
        return patchCoverage;
    }

    // How far beyond its anchor cell a placement of this kind can stand
    public float getSpreadBlocks() {
        return distribution == TreeDistribution.CLUSTERED ? clusterRadiusBlocks : EngineSetting.TREE_NO_SPREAD;
    }
}
