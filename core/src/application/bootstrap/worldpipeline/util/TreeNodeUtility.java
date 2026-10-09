package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.tree.TreeNodeStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class TreeNodeUtility extends EngineUtility {

    /*
     * Lays the segments of a tree shape out as block nodes. Each segment walks
     * the blocks its axis passes through, one face at a time, so every step
     * joins two face-adjacent blocks and a branch at any angle becomes a
     * stair of straight runs. Each block takes the segment's radius at the
     * point nearest its centre, in whole sub-voxels; a block several segments
     * pass through keeps the thickest. A segment starts in the block its
     * parent passes through at the point it sets out from, so a fork always
     * joins its parent; should rounding set it beside the parent instead, it
     * joins the thickest node next to it. A cut end clips its block flat
     * across the segment's main axis at the exact cut.
     */

    private static final float RESOLUTION = EngineSetting.SUB_VOXEL_RESOLUTION;
    private static final float HALF = 0.5f;

    // Layout \\

    // Positions in blocks from the centre of the root at ground level, the root standing in block (0, 0, 0)
    public static TreeNodeStruct layOut(
            int segmentCount,
            float[] startX,
            float[] startY,
            float[] startZ,
            float[] endX,
            float[] endY,
            float[] endZ,
            float[] startRadius,
            float[] endRadius,
            boolean[] cutStart,
            boolean[] cutEnd) {

        TreeNodeStruct nodes = new TreeNodeStruct();
        float[] from = new float[EngineSetting.AXIS_COUNT];
        float[] to = new float[EngineSetting.AXIS_COUNT];

        for (int segment = 0; segment < segmentCount; segment++) {

            toLattice(startX[segment], startY[segment], startZ[segment], from);
            toLattice(endX[segment], endY[segment], endZ[segment], to);

            int first = walk(nodes, segment, from, to, startRadius[segment], endRadius[segment]);

            if (cutStart[segment])
                clipCut(nodes, first, from, to, false);

            if (cutEnd[segment])
                clipCut(nodes, findBlock(nodes, to), from, to, true);
        }

        return nodes;
    }

    // Walk \\

    // Every block the segment's axis passes through, joined face to face — returns the node it starts in
    private static int walk(
            TreeNodeStruct nodes,
            int segment,
            float[] from,
            float[] to,
            float fromRadius,
            float toRadius) {

        int blockX = (int) Math.floor(from[0]);
        int blockY = (int) Math.floor(from[1]);
        int blockZ = (int) Math.floor(from[2]);
        int endX = (int) Math.floor(to[0]);
        int endY = (int) Math.floor(to[1]);
        int endZ = (int) Math.floor(to[2]);

        float dx = to[0] - from[0];
        float dy = to[1] - from[1];
        float dz = to[2] - from[2];
        float lengthSquared = dx * dx + dy * dy + dz * dz;

        int stepX = Integer.signum(endX - blockX);
        int stepY = Integer.signum(endY - blockY);
        int stepZ = Integer.signum(endZ - blockZ);
        float maxX = firstCrossing(from[0], dx, blockX, stepX);
        float maxY = firstCrossing(from[1], dy, blockY, stepY);
        float maxZ = firstCrossing(from[2], dz, blockZ, stepZ);
        float deltaX = stepX == 0 ? Float.MAX_VALUE : Math.abs(1f / dx);
        float deltaY = stepY == 0 ? Float.MAX_VALUE : Math.abs(1f / dy);
        float deltaZ = stepZ == 0 ? Float.MAX_VALUE : Math.abs(1f / dz);

        boolean existed = nodes.findNode(blockX, blockY, blockZ) != EngineSetting.INDEX_NOT_FOUND;
        int first = nodes.addNode(blockX, blockY, blockZ,
                resolveRadius(from, dx, dy, dz, lengthSquared, blockX, blockY, blockZ, fromRadius, toRadius),
                segment);

        if (!existed)
            joinBeside(nodes, first);

        int previous = first;
        int steps = Math.abs(endX - blockX) + Math.abs(endY - blockY) + Math.abs(endZ - blockZ);

        for (int step = 0; step < steps; step++) {

            boolean canX = blockX != endX;
            boolean canY = blockY != endY;
            boolean canZ = blockZ != endZ;

            if (canX && (!canY || maxX <= maxY) && (!canZ || maxX <= maxZ)) {
                blockX += stepX;
                maxX += deltaX;
            } else if (canY && (!canZ || maxY <= maxZ)) {
                blockY += stepY;
                maxY += deltaY;
            } else {
                blockZ += stepZ;
                maxZ += deltaZ;
            }

            int node = nodes.addNode(blockX, blockY, blockZ,
                    resolveRadius(from, dx, dy, dz, lengthSquared, blockX, blockY, blockZ, fromRadius, toRadius),
                    segment);

            nodes.connect(previous, node);
            previous = node;
        }

        return first;
    }

    // The share of the segment at which its axis first leaves the starting block along one axis
    private static float firstCrossing(float from, float delta, int block, int step) {

        if (step == 0)
            return Float.MAX_VALUE;

        return step > 0 ? (block + 1 - from) / delta : (from - block) / -delta;
    }

    // The segment's radius at the point of its axis nearest a block's centre, in whole sub-voxels, never less than one
    private static int resolveRadius(
            float[] from,
            float dx,
            float dy,
            float dz,
            float lengthSquared,
            int blockX,
            int blockY,
            int blockZ,
            float fromRadius,
            float toRadius) {

        float share = lengthSquared > 0f
                ? ((blockX + HALF - from[0]) * dx + (blockY + HALF - from[1]) * dy + (blockZ + HALF - from[2]) * dz)
                        / lengthSquared
                : 0f;

        share = Math.max(0f, Math.min(1f, share));

        int radius = Math.round((fromRadius + (toRadius - fromRadius) * share) * RESOLUTION);

        return Math.max(EngineSetting.TREE_MIN_NODE_RADIUS, Math.min(EngineSetting.TREE_MAX_NODE_RADIUS, radius));
    }

    // A segment that starts beside its parent rather than in it joins the thickest node next to it
    private static void joinBeside(TreeNodeStruct nodes, int node) {

        int best = EngineSetting.INDEX_NOT_FOUND;

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
            for (int side = -1; side <= 1; side += 2) {

                int neighbor = nodes.findNode(
                        nodes.getX(node) + (axis == EngineSetting.AXIS_X ? side : 0),
                        nodes.getY(node) + (axis == EngineSetting.AXIS_Y ? side : 0),
                        nodes.getZ(node) + (axis == EngineSetting.AXIS_Z ? side : 0));

                if (neighbor != EngineSetting.INDEX_NOT_FOUND
                        && (best == EngineSetting.INDEX_NOT_FOUND || nodes.getRadius(neighbor) > nodes.getRadius(best)))
                    best = neighbor;
            }

        if (best != EngineSetting.INDEX_NOT_FOUND)
            nodes.connect(best, node);
    }

    // Cuts \\

    // Clips the block a cut lies in flat across the segment's main axis, keeping the side toward the segment
    private static void clipCut(TreeNodeStruct nodes, int node, float[] from, float[] to, boolean atEnd) {

        if (node == EngineSetting.INDEX_NOT_FOUND)
            return;

        int axis = resolveMainAxis(from, to);
        float[] cut = atEnd ? to : from;
        int plane = Math.round((cut[axis] - nodes.getCoordinate(node, axis)) * RESOLUTION);
        boolean rising = to[axis] > from[axis];

        if (rising == atEnd)
            nodes.clip(node, axis, Short.MIN_VALUE, plane);
        else
            nodes.clip(node, axis, plane, Short.MAX_VALUE);
    }

    private static int resolveMainAxis(float[] from, float[] to) {

        float dx = Math.abs(to[0] - from[0]);
        float dy = Math.abs(to[1] - from[1]);
        float dz = Math.abs(to[2] - from[2]);

        if (dx >= dy && dx >= dz)
            return EngineSetting.AXIS_X;

        return dy >= dz ? EngineSetting.AXIS_Y : EngineSetting.AXIS_Z;
    }

    // Utility \\

    // A point in blocks from the root centre moved onto the block lattice, where the root block spans 0 to 1
    private static void toLattice(float x, float y, float z, float[] lattice) {
        lattice[0] = x + HALF;
        lattice[1] = y;
        lattice[2] = z + HALF;
    }

    private static int findBlock(TreeNodeStruct nodes, float[] lattice) {
        return nodes.findNode(
                (int) Math.floor(lattice[0]),
                (int) Math.floor(lattice[1]),
                (int) Math.floor(lattice[2]));
    }
}
