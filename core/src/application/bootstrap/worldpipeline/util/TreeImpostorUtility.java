package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.tree.TreeForm;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeInstance;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public final class TreeImpostorUtility extends EngineUtility {

    /*
     * The cheap stand-in drawn for a tree too far off to be worth growing: a
     * trunk box rising into a crown of a few leaf clusters, in the very bark
     * and leaf layouts the full tree uses, so it draws with the same
     * materials. A tree nothing has altered is sized from its mature height
     * and age alone and never grown; its form shapes the crown — one round
     * lump for a broadleaf, weeping tree or shrub, a stack of narrowing lumps
     * that rises to a point for a conifer or columnar tree, and one flat
     * spread for a palm. A tree an axe or a hand altered is sized from its own
     * shape, so a stump stands as a stump. Positions are in blocks of the
     * caller's frame. Macro terrain lays its own flat stand-ins out by the
     * same sizes and crown, so a tree keeps its place and bulk as it passes
     * from one to the other.
     */

    // Emit \\

    // One tree whose root centre stands at a point of the frame, its height the world's own
    public static void emit(
            TreeInstance tree,
            float rootX,
            float rootZ,
            FloatArrayList bark,
            FloatArrayList leaves) {

        TreeHandle treeHandle = tree.getTreeHandle();
        float[] crown = new float[EngineSetting.TREE_IMPOSTOR_CROWN_FLOATS];

        if (tree.isAltered())
            measureShape(tree.getShape(), crown);
        else
            estimateCrown(treeHandle, tree.getMatureHeight(), tree.getAge(), crown);

        float rootY = tree.getBaseY();
        float halfWidth = crown[EngineSetting.TREE_IMPOSTOR_TRUNK_HALF_WIDTH];
        float trunkTop = crown[EngineSetting.TREE_IMPOSTOR_TRUNK_TOP];
        float[] corners = treeHandle.getPartCorners();
        int[] colors = treeHandle.getPartColors();
        int barkCorner = EngineSetting.TREE_PART_BARK * EngineSetting.TREE_PART_CORNER_FLOATS;

        if (trunkTop > 0f)
            TreeMeshUtility.emitWoodBox(
                    rootX - halfWidth, rootY - EngineSetting.TREE_TRUNK_SINK_BLOCKS, rootZ - halfWidth,
                    rootX + halfWidth, rootY + trunkTop, rootZ + halfWidth,
                    corners[barkCorner], corners[barkCorner + 1], colors[EngineSetting.TREE_PART_BARK], bark);

        if (crown[EngineSetting.TREE_IMPOSTOR_CROWN_RADIUS] <= 0f)
            return;

        emitCrown(tree, crown, rootX, rootY, rootZ, leaves);
    }

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

    // An altered tree's crown measured from what stands of it — its leaf clusters, or none for a bare stump
    private static void measureShape(TreeShapeStruct shape, float[] crown) {

        float bottom = Float.MAX_VALUE;
        float top = 0f;
        float radius = 0f;

        for (int leaf = 0; leaf < shape.getLeafCount(); leaf++) {

            float leafX = shape.getLeafX(leaf);
            float leafZ = shape.getLeafZ(leaf);

            bottom = Math.min(bottom, shape.getLeafY(leaf) - shape.getLeafRadiusV(leaf));
            top = Math.max(top, shape.getLeafY(leaf) + shape.getLeafRadiusV(leaf));
            radius = Math.max(radius,
                    (float) Math.sqrt(leafX * leafX + leafZ * leafZ) + shape.getLeafRadiusH(leaf));
        }

        boolean bare = shape.getLeafCount() == 0;

        crown[EngineSetting.TREE_IMPOSTOR_CROWN_BOTTOM] = bare ? 0f : Math.max(0f, bottom);
        crown[EngineSetting.TREE_IMPOSTOR_CROWN_TOP] = top;
        crown[EngineSetting.TREE_IMPOSTOR_CROWN_RADIUS] = radius;
        crown[EngineSetting.TREE_IMPOSTOR_TRUNK_TOP] = bare
                ? shape.getMaxY()
                : bottom + (top - bottom) * EngineSetting.TREE_IMPOSTOR_TRUNK_REACH_SHARE;
        crown[EngineSetting.TREE_IMPOSTOR_TRUNK_HALF_WIDTH] = resolveHalfWidth(
                shape.getSegmentCount() > 0 ? shape.getStartRadius(0) : 0f);
    }

    // Never thinner than one sub-voxel each side, so a sapling's stem still shows
    private static float resolveHalfWidth(float radius) {
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

    private static void emitCrown(
            TreeInstance tree,
            float[] crown,
            float rootX,
            float rootY,
            float rootZ,
            FloatArrayList leaves) {

        float[] lumps = new float[EngineSetting.TREE_IMPOSTOR_LUMPS_MAX_FLOATS];
        int count = layoutCrown(tree.getTreeHandle().getForm(), crown, lumps);

        for (int lump = 0; lump < count; lump++) {

            int offset = lump * EngineSetting.TREE_IMPOSTOR_LUMP_FLOATS;

            emitLump(tree, lump, rootX, rootY + lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_CENTER_Y], rootZ,
                    lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_RADIUS_H],
                    lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_RADIUS_V], leaves);
        }
    }

    // One leaf cluster of the tree's own leaf part, its lumps rolled from the tree's seed
    private static void emitLump(
            TreeInstance tree,
            int lump,
            float centerX,
            float centerY,
            float centerZ,
            float radiusH,
            float radiusV,
            FloatArrayList leaves) {

        TreeHandle treeHandle = tree.getTreeHandle();
        int leafCorner = EngineSetting.TREE_PART_LEAF * EngineSetting.TREE_PART_CORNER_FLOATS;
        float[] corners = treeHandle.getPartCorners();

        TreeMeshUtility.emitCluster(
                centerX, centerY, centerZ, radiusH, radiusV,
                corners[leafCorner], corners[leafCorner + 1],
                treeHandle.getPartColors()[EngineSetting.TREE_PART_LEAF],
                BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(
                        tree.getSeed() ^ EngineSetting.TREE_IMPOSTOR_SEED_SALT, lump, 0)),
                leaves);
    }
}
