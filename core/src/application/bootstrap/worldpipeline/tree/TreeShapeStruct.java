package application.bootstrap.worldpipeline.tree;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class TreeShapeStruct extends StructPackage {

    /*
     * What of one tree stands at one moment, in blocks from the centre of its
     * root: every living segment grown to its age and cut where an axe severed
     * it, each with the stretch of its skeleton segment it covers, the block
     * nodes those segments lay out, every living leaf cluster,
     * whether it carries the accent and the seed its look is rolled from, every notch struck into it, and the
     * box around all of it. A cut end is flat, so a stump and the log felled
     * from it meet face to face. Immutable — a change makes a new shape — so
     * chunk builds on any thread read it without a lock.
     */

    // Species
    private final TreeHandle treeHandle;
    private final long seed;

    // Segments — blocks
    private final int segmentCount;
    private final float[] startX;
    private final float[] startY;
    private final float[] startZ;
    private final float[] endX;
    private final float[] endY;
    private final float[] endZ;
    private final float[] startRadius;
    private final float[] endRadius;
    private final int[] sourceSegment;
    private final float[] sourceStart;
    private final float[] sourceEnd;
    private final int[] depth;
    private final boolean[] cutStart;
    private final boolean[] cutEnd;

    // Nodes
    private final TreeNodeStruct nodes;

    // Leaves — blocks
    private final int leafCount;
    private final float[] leafX;
    private final float[] leafY;
    private final float[] leafZ;
    private final float[] leafRadiusH;
    private final float[] leafRadiusV;
    private final boolean[] leafAccent;
    private final float[] leafSeed;
    private final int[] leafSource;

    // Notches
    private final TreeCarveStruct[] carves;

    // Bounds — blocks
    private final float minX;
    private final float minY;
    private final float minZ;
    private final float maxX;
    private final float maxY;
    private final float maxZ;

    // Constructor \\

    public TreeShapeStruct(
            TreeHandle treeHandle,
            long seed,
            int segmentCount,
            float[] startX,
            float[] startY,
            float[] startZ,
            float[] endX,
            float[] endY,
            float[] endZ,
            float[] startRadius,
            float[] endRadius,
            int[] sourceSegment,
            float[] sourceStart,
            float[] sourceEnd,
            int[] depth,
            boolean[] cutStart,
            boolean[] cutEnd,
            TreeNodeStruct nodes,
            int leafCount,
            float[] leafX,
            float[] leafY,
            float[] leafZ,
            float[] leafRadiusH,
            float[] leafRadiusV,
            boolean[] leafAccent,
            float[] leafSeed,
            int[] leafSource,
            TreeCarveStruct[] carves) {

        // Species
        this.treeHandle = treeHandle;
        this.seed = seed;

        // Segments
        this.segmentCount = segmentCount;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.endX = endX;
        this.endY = endY;
        this.endZ = endZ;
        this.startRadius = startRadius;
        this.endRadius = endRadius;
        this.sourceSegment = sourceSegment;
        this.sourceStart = sourceStart;
        this.sourceEnd = sourceEnd;
        this.depth = depth;
        this.cutStart = cutStart;
        this.cutEnd = cutEnd;

        // Nodes
        this.nodes = nodes;

        // Leaves
        this.leafCount = leafCount;
        this.leafX = leafX;
        this.leafY = leafY;
        this.leafZ = leafZ;
        this.leafRadiusH = leafRadiusH;
        this.leafRadiusV = leafRadiusV;
        this.leafAccent = leafAccent;
        this.leafSeed = leafSeed;
        this.leafSource = leafSource;

        // Notches
        this.carves = carves;

        // Bounds
        float[] bounds = measureBounds();

        this.minX = bounds[0];
        this.minY = bounds[1];
        this.minZ = bounds[2];
        this.maxX = bounds[3];
        this.maxY = bounds[4];
        this.maxZ = bounds[5];
    }

    // The same wood and leaves with a different set of notches — every array shared, nothing copied
    public TreeShapeStruct withCarves(TreeCarveStruct[] newCarves) {
        return new TreeShapeStruct(
                treeHandle, seed,
                segmentCount, startX, startY, startZ, endX, endY, endZ, startRadius, endRadius,
                sourceSegment, sourceStart, sourceEnd, depth, cutStart, cutEnd, nodes,
                leafCount, leafX, leafY, leafZ, leafRadiusH, leafRadiusV, leafAccent, leafSeed, leafSource,
                newCarves);
    }

    // Bounds \\

    // The nodes' wood and the leaf clusters, each a node block's centre lying half a block above the root's level
    // and half its width reaching at least the block's faces
    private float[] measureBounds() {

        float[] bounds = {
                Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };

        for (int node = 0; node < nodes.getCount(); node++) {

            float half = Math.max(nodes.getRadius(node), EngineSetting.SUB_VOXEL_RESOLUTION / 2)
                    / (float) EngineSetting.SUB_VOXEL_RESOLUTION;

            include(bounds, nodes.getX(node), nodes.getY(node) + 0.5f, nodes.getZ(node), half, half);
        }

        for (int i = 0; i < leafCount; i++)
            include(bounds, leafX[i], leafY[i], leafZ[i], leafRadiusH[i], leafRadiusV[i]);

        if (nodes.isEmpty() && leafCount == 0)
            return new float[EngineSetting.BOX_INT_STRIDE];

        return bounds;
    }

    private void include(float[] bounds, float x, float y, float z, float radiusH, float radiusV) {
        bounds[0] = Math.min(bounds[0], x - radiusH);
        bounds[1] = Math.min(bounds[1], y - radiusV);
        bounds[2] = Math.min(bounds[2], z - radiusH);
        bounds[3] = Math.max(bounds[3], x + radiusH);
        bounds[4] = Math.max(bounds[4], y + radiusV);
        bounds[5] = Math.max(bounds[5], z + radiusH);
    }

    // Accessible \\

    public TreeHandle getTreeHandle() {
        return treeHandle;
    }

    public long getSeed() {
        return seed;
    }

    public boolean isEmpty() {
        return nodes.isEmpty() && leafCount == 0;
    }

    public int getSegmentCount() {
        return segmentCount;
    }

    public float getStartX(int segment) {
        return startX[segment];
    }

    public float getStartY(int segment) {
        return startY[segment];
    }

    public float getStartZ(int segment) {
        return startZ[segment];
    }

    public float getEndX(int segment) {
        return endX[segment];
    }

    public float getEndY(int segment) {
        return endY[segment];
    }

    public float getEndZ(int segment) {
        return endZ[segment];
    }

    public float getStartRadius(int segment) {
        return startRadius[segment];
    }

    public float getEndRadius(int segment) {
        return endRadius[segment];
    }

    // The skeleton segment this one was grown from
    public int getSourceSegment(int segment) {
        return sourceSegment[segment];
    }

    // The share of its skeleton segment this segment starts at
    public float getSourceStart(int segment) {
        return sourceStart[segment];
    }

    // The share of its skeleton segment this segment ends at
    public float getSourceEnd(int segment) {
        return sourceEnd[segment];
    }

    public int getDepth(int segment) {
        return depth[segment];
    }

    public boolean isCutStart(int segment) {
        return cutStart[segment];
    }

    public boolean isCutEnd(int segment) {
        return cutEnd[segment];
    }

    public TreeNodeStruct getNodes() {
        return nodes;
    }

    public int getLeafCount() {
        return leafCount;
    }

    public float getLeafX(int leaf) {
        return leafX[leaf];
    }

    public float getLeafY(int leaf) {
        return leafY[leaf];
    }

    public float getLeafZ(int leaf) {
        return leafZ[leaf];
    }

    public float getLeafRadiusH(int leaf) {
        return leafRadiusH[leaf];
    }

    public float getLeafRadiusV(int leaf) {
        return leafRadiusV[leaf];
    }

    public boolean isLeafAccent(int leaf) {
        return leafAccent[leaf];
    }

    // A cluster's own value between zero and one, so each clump grows its own lumps and sways out of step
    public float getLeafSeed(int leaf) {
        return leafSeed[leaf];
    }

    // The skeleton cluster this one was grown from
    public int getLeafSource(int leaf) {
        return leafSource[leaf];
    }

    public TreeCarveStruct[] getCarves() {
        return carves;
    }

    public float getMinX() {
        return minX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMinZ() {
        return minZ;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMaxY() {
        return maxY;
    }

    public float getMaxZ() {
        return maxZ;
    }
}
