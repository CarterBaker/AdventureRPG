package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.tree.TreeForm;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class TreeImpostorUtility extends EngineUtility {

    /*
     * A tree's bulk judged without growing it: a tree nothing has altered is
     * sized from its species, mature height and age alone, and its form lays
     * its crown out as a few lumps — one round lump for a broadleaf, weeping
     * tree or shrub, a stack of narrowing lumps that rises to a point for a
     * conifer or columnar tree, and one flat spread for a palm. The farthest
     * megas draw these lumps as leaf clusters over a trunk box, and macro
     * terrain shapes its flat stand-ins on the very same sizes and crown, so a
     * tree keeps its place and bulk as it passes from one to the other.
     */

    // Sizes \\

    // A grown tree's crown judged from its species, mature height and age, never growing it
    public static void estimateCrown(TreeHandle treeHandle, float matureHeight, float age, float[] crown) {

        float scale = TreeShapeUtility.resolveScale(matureHeight, age);
        float height = matureHeight * scale;
        float bottom = height * treeHandle.getBranches().getCrownStart();
        float top = height + treeHandle.getLeaves().getRadiusBlocks() * scale;

        crown[EngineSetting.TREE_IMPOSTOR_CROWN_BOTTOM] = bottom;
        crown[EngineSetting.TREE_IMPOSTOR_CROWN_TOP] = top;
        crown[EngineSetting.TREE_IMPOSTOR_CROWN_RADIUS] = TreeShapeUtility.resolveCrownRadius(
                treeHandle, matureHeight, scale);
        crown[EngineSetting.TREE_IMPOSTOR_TRUNK_TOP] = bottom + (top - bottom)
                * EngineSetting.TREE_IMPOSTOR_TRUNK_REACH_SHARE;
        crown[EngineSetting.TREE_IMPOSTOR_TRUNK_HALF_WIDTH] = resolveHalfWidth(
                treeHandle.getTrunk().getRadiusBlocks() * scale);
    }

    // Never thinner than one sub-voxel each side, so a sapling's stem still shows
    public static float resolveHalfWidth(float radius) {
        return Math.max(radius, 1f / EngineSetting.SUB_VOXEL_RESOLUTION);
    }

    // Crown \\

    // The lumps a crown is drawn as by its form, each its centre's height above the root and its two radii,
    // TREE_IMPOSTOR_LUMP_FLOATS apiece — the number laid out
    public static int layoutCrown(TreeForm form, float[] crown, float[] lumps) {

        float bottom = crown[EngineSetting.TREE_IMPOSTOR_CROWN_BOTTOM];
        float top = crown[EngineSetting.TREE_IMPOSTOR_CROWN_TOP];
        float radius = crown[EngineSetting.TREE_IMPOSTOR_CROWN_RADIUS];

        return switch (form) {
            case CONIFER -> layoutCone(bottom, top, radius, EngineSetting.TREE_IMPOSTOR_CONIFER_TAPER, lumps);
            case COLUMNAR -> layoutCone(bottom, top, radius, EngineSetting.TREE_IMPOSTOR_COLUMNAR_TAPER, lumps);
            case PALM -> {
                float squashed = Math.max(radius * EngineSetting.TREE_IMPOSTOR_PALM_SQUASH,
                        EngineSetting.TREE_MIN_LEAF_RADIUS_BLOCKS);
                yield layoutLump(0, top - squashed, radius, squashed, lumps);
            }
            default -> layoutLump(0, (bottom + top) * 0.5f, radius,
                    Math.max((top - bottom) * 0.5f, EngineSetting.TREE_MIN_LEAF_RADIUS_BLOCKS), lumps);
        };
    }

    // A stack of lumps from the crown's foot to its top, each narrower than the one below by the taper's share
    private static int layoutCone(float bottom, float top, float radius, float taper, float[] lumps) {

        int tiers = EngineSetting.TREE_IMPOSTOR_CONE_TIERS;
        float tierHeight = (top - bottom) / tiers;
        float radiusV = Math.max(tierHeight * EngineSetting.TREE_IMPOSTOR_TIER_RADIUS_SHARE,
                EngineSetting.TREE_MIN_LEAF_RADIUS_BLOCKS);

        for (int tier = 0; tier < tiers; tier++)
            layoutLump(tier, bottom + (tier + 0.5f) * tierHeight,
                    Math.max(radius * (1f - taper * tier / tiers), EngineSetting.TREE_MIN_LEAF_RADIUS_BLOCKS),
                    radiusV, lumps);

        return tiers;
    }

    // One lump written in at its index — the number laid out up to and including it
    private static int layoutLump(int lump, float centerY, float radiusH, float radiusV, float[] lumps) {

        int offset = lump * EngineSetting.TREE_IMPOSTOR_LUMP_FLOATS;

        lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_CENTER_Y] = centerY;
        lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_RADIUS_H] = radiusH;
        lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_RADIUS_V] = radiusV;

        return lump + 1;
    }
}
