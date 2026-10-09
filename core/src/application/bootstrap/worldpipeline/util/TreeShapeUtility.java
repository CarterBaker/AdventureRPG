package application.bootstrap.worldpipeline.util;

import java.util.Arrays;

import application.bootstrap.worldpipeline.tree.TreeCarveStruct;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeLeafStruct;
import application.bootstrap.worldpipeline.tree.TreeNodeStruct;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import application.bootstrap.worldpipeline.tree.TreeSkeletonStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class TreeShapeUtility extends EngineUtility {

    /*
     * Turns a grown skeleton into the shape that stands at one age. A tree
     * grows as its mature self in miniature, scaled up from a sapling's
     * height, while every branch appears at its birth and lengthens over a
     * short span; a fork only sprouts once the stretch of its parent it sets
     * out from has grown. Severs, one share of each segment's length kept, cut
     * a segment short with a flat end and drop everything that set out beyond
     * the cut. extract() gives the piece one cut frees — the rest of the
     * severed segment and everything growing from it — so a felled trunk or a
     * lopped limb carries exactly the wood and leaves the tree just lost. The
     * standing segments are laid out as block nodes, and each leaf cluster is
     * kept by the species' density and given the accent by its chance, both
     * rolled from the tree's seed and the cluster, so a cluster keeps its
     * look as the tree grows and loses limbs.
     */

    // Scale \\

    // The size of a tree at an age, from a sapling's height at zero to full size at one
    public static float resolveScale(TreeSkeletonStruct skeleton, float age) {

        float saplingShare = Math.min(1f,
                EngineSetting.TREE_SAPLING_HEIGHT_BLOCKS / Math.max(skeleton.getMatureHeight(),
                        EngineSetting.TREE_MIN_SEGMENT_BLOCKS));
        float growth = (float) Math.pow(clamp01(age), EngineSetting.TREE_GROWTH_CURVE);

        return saplingShare + (1f - saplingShare) * growth;
    }

    // A full set of leaf marks with nothing broken off
    public static boolean[] createBrokenLeaves(TreeSkeletonStruct skeleton) {
        return new boolean[skeleton.getLeafCount()];
    }

    // A full set of severs that cut nothing
    public static float[] createSevers(TreeSkeletonStruct skeleton) {

        float[] severs = new float[skeleton.getSegmentCount()];

        Arrays.fill(severs, 1f);

        return severs;
    }

    // Evaluate \\

    // Everything still standing at an age
    public static TreeShapeStruct evaluate(
            TreeHandle treeHandle,
            long seed,
            TreeSkeletonStruct skeleton,
            float age,
            float[] severs,
            boolean[] brokenLeaves,
            TreeCarveStruct[] carves) {

        float[] grown = computeGrowth(skeleton, age, severs);

        return buildShape(
                treeHandle, seed, skeleton, grown, resolveScale(skeleton, age), severs, brokenLeaves,
                EngineSetting.INDEX_NOT_FOUND, 0f, carves);
    }

    // The piece a cut through one segment frees, before the cut is recorded among the severs
    public static TreeShapeStruct extract(
            TreeHandle treeHandle,
            long seed,
            TreeSkeletonStruct skeleton,
            float age,
            float[] severs,
            boolean[] brokenLeaves,
            int root,
            float rootShare,
            TreeCarveStruct[] carves) {

        float[] grown = computeGrowth(skeleton, age, severs);

        for (int segment = 0; segment < grown.length; segment++)
            if (segment != root && !skeleton.isDescendant(segment, root))
                grown[segment] = -1f;

        if (grown[root] <= rootShare)
            grown[root] = -1f;

        return buildShape(
                treeHandle, seed, skeleton, grown, resolveScale(skeleton, age), severs, brokenLeaves,
                root, rootShare, carves);
    }

    // Growth \\

    // How much of each segment has grown and is still attached, a share of its mature length — below zero when it
    // has not sprouted or has been cut away
    private static float[] computeGrowth(TreeSkeletonStruct skeleton, float age, float[] severs) {

        int count = skeleton.getSegmentCount();
        float[] grown = new float[count];

        for (int segment = 0; segment < count; segment++) {

            grown[segment] = -1f;

            if (skeleton.getBirth(segment) > age)
                continue;

            int parent = skeleton.getParent(segment);

            if (parent != EngineSetting.INDEX_NOT_FOUND && (grown[parent] < 0f
                    || skeleton.getAttach(segment) > grown[parent] + EngineSetting.TREE_SHARE_EPSILON))
                continue;

            float share = skeleton.getDepth(segment) == 0
                    ? 1f
                    : clamp01((age - skeleton.getBirth(segment)) / EngineSetting.TREE_BRANCH_GROW_SPAN);

            share = Math.min(share, severs[segment]);

            if (share > EngineSetting.TREE_SHARE_EPSILON)
                grown[segment] = share;
        }

        return grown;
    }

    // Build \\

    private static TreeShapeStruct buildShape(
            TreeHandle treeHandle,
            long seed,
            TreeSkeletonStruct skeleton,
            float[] grown,
            float scale,
            float[] severs,
            boolean[] brokenLeaves,
            int cutRoot,
            float cutShare,
            TreeCarveStruct[] carves) {

        int segmentCount = 0;
        int leafCount = 0;

        for (int segment = 0; segment < grown.length; segment++)
            if (grown[segment] > 0f)
                segmentCount++;

        for (int leaf = 0; leaf < skeleton.getLeafCount(); leaf++)
            if (isLeafAlive(treeHandle, seed, skeleton, grown, severs, brokenLeaves, leaf))
                leafCount++;

        float[] startX = new float[segmentCount];
        float[] startY = new float[segmentCount];
        float[] startZ = new float[segmentCount];
        float[] endX = new float[segmentCount];
        float[] endY = new float[segmentCount];
        float[] endZ = new float[segmentCount];
        float[] startRadius = new float[segmentCount];
        float[] endRadius = new float[segmentCount];
        int[] sourceSegment = new int[segmentCount];
        float[] sourceStart = new float[segmentCount];
        float[] sourceEnd = new float[segmentCount];
        int[] depth = new int[segmentCount];
        boolean[] cutStart = new boolean[segmentCount];
        boolean[] cutEnd = new boolean[segmentCount];
        int index = 0;

        for (int segment = 0; segment < grown.length; segment++) {

            float share = grown[segment];

            if (share <= 0f)
                continue;

            float from = segment == cutRoot ? cutShare : 0f;

            startX[index] = scale * along(skeleton.getStartX(segment), skeleton.getEndX(segment), from);
            startY[index] = scale * along(skeleton.getStartY(segment), skeleton.getEndY(segment), from);
            startZ[index] = scale * along(skeleton.getStartZ(segment), skeleton.getEndZ(segment), from);
            endX[index] = scale * along(skeleton.getStartX(segment), skeleton.getEndX(segment), share);
            endY[index] = scale * along(skeleton.getStartY(segment), skeleton.getEndY(segment), share);
            endZ[index] = scale * along(skeleton.getStartZ(segment), skeleton.getEndZ(segment), share);
            startRadius[index] = Math.max(EngineSetting.TREE_MIN_RADIUS_BLOCKS,
                    scale * along(skeleton.getStartRadius(segment), skeleton.getEndRadius(segment), from));
            endRadius[index] = Math.max(EngineSetting.TREE_MIN_RADIUS_BLOCKS,
                    scale * along(skeleton.getStartRadius(segment), skeleton.getEndRadius(segment), share));
            sourceSegment[index] = segment;
            sourceStart[index] = from;
            sourceEnd[index] = share;
            depth[index] = skeleton.getDepth(segment);
            cutStart[index] = segment == cutRoot;
            cutEnd[index] = severs[segment] < 1f && share >= severs[segment];
            index++;
        }

        float[] leafX = new float[leafCount];
        float[] leafY = new float[leafCount];
        float[] leafZ = new float[leafCount];
        float[] leafRadiusH = new float[leafCount];
        float[] leafRadiusV = new float[leafCount];
        boolean[] leafAccent = new boolean[leafCount];
        float[] leafSeed = new float[leafCount];
        int[] leafSource = new int[leafCount];
        TreeLeafStruct leaves = treeHandle.getLeaves();
        float minLeafRadius = EngineSetting.TREE_MIN_LEAF_RADIUS_BLOCKS;

        index = 0;

        for (int leaf = 0; leaf < skeleton.getLeafCount(); leaf++) {

            if (!isLeafAlive(treeHandle, seed, skeleton, grown, severs, brokenLeaves, leaf))
                continue;

            leafX[index] = scale * skeleton.getLeafX(leaf);
            leafY[index] = scale * skeleton.getLeafY(leaf);
            leafZ[index] = scale * skeleton.getLeafZ(leaf);
            leafRadiusH[index] = Math.max(minLeafRadius, scale * skeleton.getLeafRadiusH(leaf));
            leafRadiusV[index] = Math.max(minLeafRadius, scale * skeleton.getLeafRadiusV(leaf));
            leafAccent[index] = rollLeaf(seed, EngineSetting.TREE_LEAF_ACCENT_SALT, leaf) < leaves.getAccentChance();
            leafSeed[index] = rollLeaf(seed, EngineSetting.TREE_LEAF_SEED_SALT, leaf);
            leafSource[index] = leaf;
            index++;
        }

        TreeNodeStruct nodes = TreeNodeUtility.layOut(
                segmentCount, startX, startY, startZ, endX, endY, endZ, startRadius, endRadius, cutStart, cutEnd);

        return new TreeShapeStruct(
                treeHandle, seed,
                segmentCount, startX, startY, startZ, endX, endY, endZ, startRadius, endRadius,
                sourceSegment, sourceStart, sourceEnd, depth, cutStart, cutEnd, nodes,
                leafCount, leafX, leafY, leafZ, leafRadiusH, leafRadiusV, leafAccent, leafSeed, leafSource,
                carves);
    }

    // A leaf lives while the species' density keeps it, nothing has broken it off, and the segment holding it has
    // fully grown and keeps its end
    private static boolean isLeafAlive(
            TreeHandle treeHandle,
            long seed,
            TreeSkeletonStruct skeleton,
            float[] grown,
            float[] severs,
            boolean[] brokenLeaves,
            int leaf) {

        int segment = skeleton.getLeafSegment(leaf);

        return !brokenLeaves[leaf] && grown[segment] >= 1f - EngineSetting.TREE_SHARE_EPSILON && severs[segment] >= 1f
                && rollLeaf(seed, EngineSetting.TREE_LEAF_KEEP_SALT, leaf) < treeHandle.getLeaves().getDensity();
    }

    // One cluster's roll for one choice, from the tree's seed and the cluster's place in the skeleton
    private static float rollLeaf(long seed, long salt, int leaf) {
        return BiomeFieldUtility.hash01(BiomeFieldUtility.hashCell(seed ^ salt, leaf, 0));
    }

    // Utility \\

    private static float along(float from, float to, float share) {
        return from + (to - from) * share;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
