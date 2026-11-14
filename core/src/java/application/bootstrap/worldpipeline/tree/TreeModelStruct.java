package application.bootstrap.worldpipeline.tree;

import java.util.Arrays;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class TreeModelStruct extends StructPackage {

    /*
     * One thread's scratch for drawing trees coarser than in full: a skeleton
     * grown into again for every tree, the growth, severs and leaf marks it is
     * evaluated at an age with, the wood and leaf clusters the tree stands as
     * in blocks from the centre of its root, the cells its clusters merge
     * into, TREE_MODEL_CELL_FLOATS apiece, the crown and lumps an outline is
     * sized and laid out by, and the box each box of bark is written through.
     * Nothing is ever cut or broken off a
     * tree drawn from its seed, so the severs stay whole and the marks clear.
     * Kept warm between trees, so drawing one allocates nothing once its lists
     * have grown.
     */

    // Skeleton
    private final TreeSkeletonStruct skeleton;
    private final float[] growth;
    private final float[] severs;
    private final boolean[] brokenLeaves;

    // Segments — blocks
    private final FloatArrayList startX;
    private final FloatArrayList startY;
    private final FloatArrayList startZ;
    private final FloatArrayList endX;
    private final FloatArrayList endY;
    private final FloatArrayList endZ;
    private final FloatArrayList startRadius;
    private final FloatArrayList endRadius;
    private final IntArrayList depth;

    // Leaves — blocks
    private final FloatArrayList leafX;
    private final FloatArrayList leafY;
    private final FloatArrayList leafZ;
    private final FloatArrayList leafRadiusH;
    private final FloatArrayList leafRadiusV;
    private final FloatArrayList leafSeed;
    private final BooleanArrayList leafAccent;

    // Cells
    private final float[] cells;

    // Outline
    private final float[] crown;
    private final float[] lumps;

    // Box — its corner, its size and the corner being written
    private final float[] box;

    // Constructor \\

    public TreeModelStruct() {

        // Skeleton
        this.skeleton = new TreeSkeletonStruct();
        this.growth = new float[EngineSetting.TREE_MAX_SEGMENTS];
        this.severs = new float[EngineSetting.TREE_MAX_SEGMENTS];
        this.brokenLeaves = new boolean[EngineSetting.TREE_MAX_LEAF_CLUSTERS];

        Arrays.fill(severs, 1f);

        // Segments
        this.startX = new FloatArrayList();
        this.startY = new FloatArrayList();
        this.startZ = new FloatArrayList();
        this.endX = new FloatArrayList();
        this.endY = new FloatArrayList();
        this.endZ = new FloatArrayList();
        this.startRadius = new FloatArrayList();
        this.endRadius = new FloatArrayList();
        this.depth = new IntArrayList();

        // Leaves
        this.leafX = new FloatArrayList();
        this.leafY = new FloatArrayList();
        this.leafZ = new FloatArrayList();
        this.leafRadiusH = new FloatArrayList();
        this.leafRadiusV = new FloatArrayList();
        this.leafSeed = new FloatArrayList();
        this.leafAccent = new BooleanArrayList();

        // Cells
        this.cells = new float[EngineSetting.TREE_MODEL_MAX_CELLS * EngineSetting.TREE_MODEL_CELL_FLOATS];

        // Outline
        this.crown = new float[EngineSetting.TREE_IMPOSTOR_CROWN_FLOATS];
        this.lumps = new float[EngineSetting.TREE_IMPOSTOR_LUMPS_MAX_FLOATS];

        // Box
        this.box = new float[EngineSetting.AXIS_COUNT * 3];
    }

    // Management \\

    public void addSegment(
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ,
            float fromRadius,
            float toRadius,
            int segmentDepth) {

        startX.add(fromX);
        startY.add(fromY);
        startZ.add(fromZ);
        endX.add(toX);
        endY.add(toY);
        endZ.add(toZ);
        startRadius.add(fromRadius);
        endRadius.add(toRadius);
        depth.add(segmentDepth);
    }

    public void addLeaf(float x, float y, float z, float radiusH, float radiusV, float seed, boolean accent) {
        leafX.add(x);
        leafY.add(y);
        leafZ.add(z);
        leafRadiusH.add(radiusH);
        leafRadiusV.add(radiusV);
        leafSeed.add(seed);
        leafAccent.add(accent);
    }

    // Every segment and leaf of the last tree let go
    public void clear() {

        startX.clear();
        startY.clear();
        startZ.clear();
        endX.clear();
        endY.clear();
        endZ.clear();
        startRadius.clear();
        endRadius.clear();
        depth.clear();
        leafX.clear();
        leafY.clear();
        leafZ.clear();
        leafRadiusH.clear();
        leafRadiusV.clear();
        leafSeed.clear();
        leafAccent.clear();
    }

    // Accessible \\

    public TreeSkeletonStruct getSkeleton() {
        return skeleton;
    }

    public float[] getGrowth() {
        return growth;
    }

    public float[] getSevers() {
        return severs;
    }

    public boolean[] getBrokenLeaves() {
        return brokenLeaves;
    }

    public int getSegmentCount() {
        return startX.size();
    }

    public float getStartX(int segment) {
        return startX.getFloat(segment);
    }

    public float getStartY(int segment) {
        return startY.getFloat(segment);
    }

    public float getStartZ(int segment) {
        return startZ.getFloat(segment);
    }

    public float getEndX(int segment) {
        return endX.getFloat(segment);
    }

    public float getEndY(int segment) {
        return endY.getFloat(segment);
    }

    public float getEndZ(int segment) {
        return endZ.getFloat(segment);
    }

    public float getStartRadius(int segment) {
        return startRadius.getFloat(segment);
    }

    public float getEndRadius(int segment) {
        return endRadius.getFloat(segment);
    }

    public int getDepth(int segment) {
        return depth.getInt(segment);
    }

    public int getLeafCount() {
        return leafX.size();
    }

    public float getLeafX(int leaf) {
        return leafX.getFloat(leaf);
    }

    public float getLeafY(int leaf) {
        return leafY.getFloat(leaf);
    }

    public float getLeafZ(int leaf) {
        return leafZ.getFloat(leaf);
    }

    public float getLeafRadiusH(int leaf) {
        return leafRadiusH.getFloat(leaf);
    }

    public float getLeafRadiusV(int leaf) {
        return leafRadiusV.getFloat(leaf);
    }

    public float getLeafSeed(int leaf) {
        return leafSeed.getFloat(leaf);
    }

    public boolean isLeafAccent(int leaf) {
        return leafAccent.getBoolean(leaf);
    }

    public float[] getCells() {
        return cells;
    }

    public float[] getCrown() {
        return crown;
    }

    public float[] getLumps() {
        return lumps;
    }

    public float[] getBox() {
        return box;
    }
}
