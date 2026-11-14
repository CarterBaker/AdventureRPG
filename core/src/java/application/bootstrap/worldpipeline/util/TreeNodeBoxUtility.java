package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.tree.TreeNodeStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class TreeNodeBoxUtility extends EngineUtility {

    /*
     * The boxes one node of wood is made of, the one definition both drawing
     * and collision read, in sub-voxels from the node block's corner. A node
     * no thicker than a block is a cube about the block's centre with an arm
     * out to each joined face, as thick as the thinner of the two, the way
     * Dynamic Trees shapes a branch block; a thicker node is one box its
     * radius wide that stops flush at every face it shares with another thick
     * node. An inset shrinks every outer side and an arm's sides by that many
     * sub-voxels, leaving only the faces that run on into wood at least as
     * thick, so the heartwood inside the bark stays one piece and never shows
     * where a limb narrows. A node a cut runs through is
     * clipped on its cut axis.
     */

    private static final int BLOCK = EngineSetting.SUB_VOXEL_RESOLUTION;
    private static final int CENTER = EngineSetting.SUB_VOXEL_RESOLUTION / 2;
    private static final int AXES = EngineSetting.AXIS_COUNT;
    private static final int STRIDE = EngineSetting.BOX_INT_STRIDE;

    // Boxes \\

    // Writes the node's boxes into out, BOX_INT_STRIDE ints each, and returns how many it wrote — out holds
    // TREE_NODE_MAX_BOXES boxes; an empty box is never written
    public static int resolveBoxes(TreeNodeStruct nodes, int node, int inset, int[] out) {

        int radius = nodes.getRadius(node);

        if (radius > CENTER) {

            for (int axis = 0; axis < AXES; axis++) {
                out[axis] = isJoinedThick(nodes, node, axis, false)
                        ? resolveInset(nodes, node, axis, false, inset)
                        : CENTER - radius + inset;
                out[axis + AXES] = isJoinedThick(nodes, node, axis, true)
                        ? BLOCK - resolveInset(nodes, node, axis, true, inset)
                        : CENTER + radius - inset;
            }

            return keep(nodes, node, out, 0);
        }

        for (int axis = 0; axis < AXES; axis++) {
            out[axis] = CENTER - radius + resolveInset(nodes, node, axis, false, inset);
            out[axis + AXES] = CENTER + radius - resolveInset(nodes, node, axis, true, inset);
        }

        int count = keep(nodes, node, out, 0);

        if (radius == CENTER)
            return count;

        for (int axis = 0; axis < AXES; axis++)
            for (int side = 0; side < 2; side++) {

                boolean positive = side == 1;
                int neighbor = nodes.getNeighbor(node, axis, positive);

                if (neighbor == EngineSetting.INDEX_NOT_FOUND)
                    continue;

                int arm = Math.min(radius, nodes.getRadius(neighbor)) - inset;
                int base = count * STRIDE;

                for (int other = 0; other < AXES; other++) {
                    out[base + other] = CENTER - arm;
                    out[base + other + AXES] = CENTER + arm;
                }

                out[base + axis] = positive ? CENTER + radius : 0;
                out[base + axis + AXES] = positive ? BLOCK : CENTER - radius;

                count = keep(nodes, node, out, count);
            }

        return count;
    }

    // A side runs on into its neighbour uninset only when the neighbour's wood is at least as thick, so heartwood
    // never reaches a face a thinner neighbour leaves open
    private static int resolveInset(TreeNodeStruct nodes, int node, int axis, boolean positive, int inset) {

        int neighbor = nodes.getNeighbor(node, axis, positive);

        if (neighbor != EngineSetting.INDEX_NOT_FOUND && nodes.getRadius(neighbor) >= nodes.getRadius(node))
            return 0;

        return inset;
    }

    private static boolean isJoinedThick(TreeNodeStruct nodes, int node, int axis, boolean positive) {

        int neighbor = nodes.getNeighbor(node, axis, positive);

        return neighbor != EngineSetting.INDEX_NOT_FOUND && nodes.getRadius(neighbor) > CENTER;
    }

    // Clips the box at index count by the node's cut, returning count plus one when anything of it is left
    private static int keep(TreeNodeStruct nodes, int node, int[] out, int count) {

        int base = count * STRIDE;
        int clipAxis = nodes.getClipAxis(node);

        if (clipAxis != EngineSetting.INDEX_NOT_FOUND) {
            out[base + clipAxis] = Math.max(out[base + clipAxis], nodes.getClipLow(node));
            out[base + clipAxis + AXES] = Math.min(out[base + clipAxis + AXES], nodes.getClipHigh(node));
        }

        for (int axis = 0; axis < AXES; axis++)
            if (out[base + axis] >= out[base + axis + AXES])
                return count;

        return count + 1;
    }
}
