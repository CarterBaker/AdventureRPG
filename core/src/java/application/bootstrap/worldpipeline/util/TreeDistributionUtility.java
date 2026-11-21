package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.biome.BiomeTreeStruct;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;

public final class TreeDistributionUtility extends EngineUtility {

    /*
     * How a biome's kinds of tree spread across its ground, the definition
     * tree placement rolls every tree by: a field kind keeps its trees inside
     * the patches of a slow noise, and every wild tree keeps room around its
     * root by the crown a typical wild tree of its kind spreads — its middle
     * mature height at its middle wild age.
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

    // Clearance \\

    // How far a wild tree of a species keeps its neighbours' roots from its own: a share of its typical crown, never
    // less than its trunk and stems spread with a margin, so no two trunks ever grow into one another
    public static float resolveClearance(TreeHandle treeHandle) {

        float matureHeight = resolveTypicalMatureHeight(treeHandle);
        float scale = TreeShapeUtility.resolveScale(matureHeight, resolveTypicalAge(treeHandle));
        float trunk = (treeHandle.getTrunk().getRadiusBlocks() + treeHandle.getTrunk().getStemSpreadBlocks()) * scale
                + EngineSetting.TREE_CLEARANCE_TRUNK_MARGIN_BLOCKS;

        return Math.max(trunk, resolveTypicalCrownRadius(treeHandle) * EngineSetting.TREE_CLEARANCE_CROWN_SHARE);
    }

    // Typical Tree \\

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
