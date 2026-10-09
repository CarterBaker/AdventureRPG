package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.biome.BiomeTreeStruct;
import application.bootstrap.worldpipeline.tree.TreeDistribution;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;

public final class TreeDistributionUtility extends EngineUtility {

    /*
     * How a biome's kinds of tree spread across its ground, as the one
     * definition both ends of the world read: placement, which rolls every
     * tree a chunk holds, and distant terrain, which only needs the woods they
     * add up to. A field kind keeps its trees inside the patches of a slow
     * noise; every kind covers a share of the ground set by its spacing, its
     * chance and the size of its groves, against the crown a typical wild tree
     * of it spreads — its middle mature height at its middle wild age.
     */

    // Patch \\

    // True where a field kind's slow patch noise keeps woods rather than clearings
    public static boolean isInPatch(long worldSeed, BiomeTreeStruct kind, double x, double z) {

        float noise = NoiseUtility.noise2(
                worldSeed ^ EngineSetting.TREE_PATCH_SALT ^ kind.getNameSeed(),
                x / kind.getPatchWavelengthBlocks(),
                z / kind.getPatchWavelengthBlocks());

        return (noise + 1f) * 0.5f < kind.getPatchCoverage();
    }

    // Coverage \\

    // The share of the ground a kind's crowns cover around a place, from none to all of it
    public static float resolveCoverage(
            long worldSeed,
            BiomeTreeStruct kind,
            TreeHandle treeHandle,
            double x,
            double z) {

        if (kind.getDistribution() == TreeDistribution.FIELD && !isInPatch(worldSeed, kind, x, z))
            return 0f;

        float trees = kind.getDistribution() == TreeDistribution.CLUSTERED
                ? kind.getChance() * (kind.getMinClusterTrees() + kind.getMaxClusterTrees()) * 0.5f
                : kind.getChance();
        float radius = resolveTypicalCrownRadius(treeHandle);
        float spacing = kind.getSpacingBlocks();

        return Math.min(1f, trees * (float) Math.PI * radius * radius / (spacing * spacing));
    }

    // Typical Tree \\

    // How high a typical wild tree of a species stands, crown included
    public static float resolveTypicalHeight(TreeHandle treeHandle) {

        float matureHeight = resolveTypicalMatureHeight(treeHandle);
        float scale = TreeShapeUtility.resolveScale(matureHeight, resolveTypicalAge(treeHandle));

        return (matureHeight + treeHandle.getLeaves().getRadiusBlocks()) * scale;
    }

    private static float resolveTypicalCrownRadius(TreeHandle treeHandle) {

        float matureHeight = resolveTypicalMatureHeight(treeHandle);
        float scale = TreeShapeUtility.resolveScale(matureHeight, resolveTypicalAge(treeHandle));

        return TreeShapeUtility.resolveCrownRadius(treeHandle, matureHeight, scale);
    }

    private static float resolveTypicalMatureHeight(TreeHandle treeHandle) {
        return (treeHandle.getTrunk().getMinHeightBlocks() + treeHandle.getTrunk().getMaxHeightBlocks()) * 0.5f;
    }

    private static float resolveTypicalAge(TreeHandle treeHandle) {
        return (treeHandle.getGrowth().getWildMinAge() + treeHandle.getGrowth().getWildMaxAge()) * 0.5f;
    }
}
