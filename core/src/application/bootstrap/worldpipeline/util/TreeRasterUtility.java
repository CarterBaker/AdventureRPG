package application.bootstrap.worldpipeline.util;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.worldpipeline.tree.TreeCarveStruct;
import application.bootstrap.worldpipeline.tree.TreeNodeStruct;
import application.bootstrap.worldpipeline.tree.TreeShapeStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class TreeRasterUtility extends EngineUtility {

    /*
     * Lays the wood of one tree shape onto a sub-voxel grid inside a
     * block-aligned region, so only the stretch of tree that region needs is
     * ever filled. Every node is a handful of axis-aligned boxes, the way
     * Dynamic Trees builds a branch block, so a straight run merges into four
     * long faces; TreeNodeBoxUtility is the one definition of those boxes.
     * They are filled with bark, then again inset by the bark's depth with
     * heartwood, so only a notch or a cut ever shows the heartwood. Notches
     * are cut last, sub-voxel by sub-voxel. Leaves are never laid on the
     * grid; they are drawn as rounded clusters of their own. The grid must lie
     * block-aligned with the world, so every chunk lays down the very same
     * wood.
     */

    private static final int BLOCK = EngineSetting.SUB_VOXEL_RESOLUTION;
    private static final int CENTER = EngineSetting.SUB_VOXEL_RESOLUTION / 2;

    // Rasterize \\

    // The root stands in the given grid block, its centre at that block's middle on its floor; the region is min
    // inclusive, max exclusive and block aligned in grid sub-voxels; partBase is the first of the two parts this
    // tree's wood fills with
    public static void rasterize(
            TreeShapeStruct shape,
            int rootBlockX,
            int rootBlockY,
            int rootBlockZ,
            int regionMinX,
            int regionMinY,
            int regionMinZ,
            int regionMaxX,
            int regionMaxY,
            int regionMaxZ,
            int partBase,
            SubVoxelGridStruct grid) {

        int[] region = { regionMinX, regionMinY, regionMinZ, regionMaxX, regionMaxY, regionMaxZ };
        int[] root = { rootBlockX, rootBlockY, rootBlockZ };
        int[] boxes = new int[EngineSetting.TREE_NODE_MAX_BOXES * EngineSetting.BOX_INT_STRIDE];
        int[] box = new int[EngineSetting.BOX_INT_STRIDE];
        TreeNodeStruct nodes = shape.getNodes();
        int bark = partBase + EngineSetting.TREE_PART_BARK;
        int wood = partBase + EngineSetting.TREE_PART_WOOD;
        int barkDepth = shape.getTreeHandle().getWood().getBarkSubVoxels();

        for (int node = 0; node < nodes.getCount(); node++)
            fillNode(nodes, node, root, region, 0, bark, boxes, grid);

        for (int node = 0; node < nodes.getCount(); node++)
            fillNode(nodes, node, root, region, barkDepth, wood, boxes, grid);

        TreeCarveStruct[] carves = shape.getCarves();

        for (int i = 0; i < carves.length; i++)
            cutCarve(carves[i], root, region, box, grid);
    }

    // Wood \\

    // One node's boxes, inset on every outer side by the inset, filled with one part
    private static void fillNode(
            TreeNodeStruct nodes,
            int node,
            int[] root,
            int[] region,
            int inset,
            int part,
            int[] boxes,
            SubVoxelGridStruct grid) {

        int reach = Math.max(nodes.getRadius(node), CENTER);
        int originX = (root[0] + nodes.getX(node)) * BLOCK;
        int originY = (root[1] + nodes.getY(node)) * BLOCK;
        int originZ = (root[2] + nodes.getZ(node)) * BLOCK;

        if (originX + CENTER + reach <= region[0] || originX + CENTER - reach >= region[3]
                || originY + CENTER + reach <= region[1] || originY + CENTER - reach >= region[4]
                || originZ + CENTER + reach <= region[2] || originZ + CENTER - reach >= region[5])
            return;

        int count = TreeNodeBoxUtility.resolveBoxes(nodes, node, inset, boxes);

        for (int box = 0; box < count; box++) {

            int base = box * EngineSetting.BOX_INT_STRIDE;

            grid.fillBox(
                    Math.max(region[0], originX + boxes[base + EngineSetting.BOX_MIN_X]),
                    Math.max(region[1], originY + boxes[base + EngineSetting.BOX_MIN_Y]),
                    Math.max(region[2], originZ + boxes[base + EngineSetting.BOX_MIN_Z]),
                    Math.min(region[3], originX + boxes[base + EngineSetting.BOX_MAX_X]),
                    Math.min(region[4], originY + boxes[base + EngineSetting.BOX_MAX_Y]),
                    Math.min(region[5], originZ + boxes[base + EngineSetting.BOX_MAX_Z]),
                    part,
                    false);
        }
    }

    // Notches \\

    private static void cutCarve(
            TreeCarveStruct carve,
            int[] root,
            int[] region,
            int[] box,
            SubVoxelGridStruct grid) {

        float rootX = root[0] * BLOCK + CENTER;
        float rootY = root[1] * BLOCK;
        float rootZ = root[2] * BLOCK + CENTER;
        float reach = carve.getReach();

        box[0] = Math.max(region[0], (int) Math.floor(rootX + carve.getCenterX() - reach));
        box[1] = Math.max(region[1], (int) Math.floor(rootY + carve.getCenterY() - carve.getHalfHeight()));
        box[2] = Math.max(region[2], (int) Math.floor(rootZ + carve.getCenterZ() - reach));
        box[3] = Math.min(region[3], (int) Math.ceil(rootX + carve.getCenterX() + reach));
        box[4] = Math.min(region[4], (int) Math.ceil(rootY + carve.getCenterY() + carve.getHalfHeight()));
        box[5] = Math.min(region[5], (int) Math.ceil(rootZ + carve.getCenterZ() + reach));

        for (int z = box[2]; z < box[5]; z++)
            for (int y = box[1]; y < box[4]; y++)
                for (int x = box[0]; x < box[3]; x++)
                    if (carve.contains(x + 0.5f - rootX, y + 0.5f - rootY, z + 0.5f - rootZ))
                        grid.clearCell(x, y, z);
    }
}
