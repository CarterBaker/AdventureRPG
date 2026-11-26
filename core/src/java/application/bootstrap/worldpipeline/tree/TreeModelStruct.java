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
     * in blocks from the centre of its root, each segment with the one it
     * sets out from and each cluster with the segment it hangs on, the reach
     * of the clusters every segment carries and whether it is drawn at the
     * level being drawn, the segment each skeleton segment was last gathered
     * into, the bounds of its clusters, the cells they merge into,
     * TREE_MODEL_CELL_FLOATS apiece, and the box each box of bark is written through. Nothing is ever cut or
     * broken off a
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
    private final IntArrayList parent;

    // Leaves — blocks
    private final FloatArrayList leafX;
    private final FloatArrayList leafY;
    private final FloatArrayList leafZ;
    private final FloatArrayList leafRadiusH;
    private final FloatArrayList leafRadiusV;
    private final FloatArrayList leafSeed;
    private final BooleanArrayList leafAccent;
    private final IntArrayList leafSegment;

    // Carry — per segment, and per skeleton segment
    private final FloatArrayList reach;
    private final BooleanArrayList kept;
    private final int[] source2Segment;
    private final IntArrayList sourcesSet;

    // Cells — the clusters' bounds, min corner then max, and every cell
    private final float[] bounds;
    private final float[] cells;

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
        this.parent = new IntArrayList();

        // Leaves
        this.leafX = new FloatArrayList();
        this.leafY = new FloatArrayList();
        this.leafZ = new FloatArrayList();
        this.leafRadiusH = new FloatArrayList();
        this.leafRadiusV = new FloatArrayList();
        this.leafSeed = new FloatArrayList();
        this.leafAccent = new BooleanArrayList();
        this.leafSegment = new IntArrayList();

        // Carry
        this.reach = new FloatArrayList();
        this.kept = new BooleanArrayList();
        this.source2Segment = new int[EngineSetting.TREE_MAX_SEGMENTS];
        this.sourcesSet = new IntArrayList();

        Arrays.fill(source2Segment, EngineSetting.INDEX_NOT_FOUND);

        // Cells
        this.bounds = new float[EngineSetting.AXIS_COUNT * 2];
        this.cells = new float[EngineSetting.TREE_LOD_MAX_CELLS * EngineSetting.TREE_MODEL_CELL_FLOATS];

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
            int segmentDepth,
            int parentSegment) {

        startX.add(fromX);
        startY.add(fromY);
        startZ.add(fromZ);
        endX.add(toX);
        endY.add(toY);
        endZ.add(toZ);
        startRadius.add(fromRadius);
        endRadius.add(toRadius);
        depth.add(segmentDepth);
        parent.add(parentSegment);
    }

    public void addLeaf(
            float x,
            float y,
            float z,
            float radiusH,
            float radiusV,
            float seed,
            boolean accent,
            int segment) {

        leafX.add(x);
        leafY.add(y);
        leafZ.add(z);
        leafRadiusH.add(radiusH);
        leafRadiusV.add(radiusV);
        leafSeed.add(seed);
        leafAccent.add(accent);
        leafSegment.add(segment);
    }

    // The segment a skeleton segment was last gathered into, or none
    public int getSegmentOfSource(int sourceSegment) {
        return sourceSegment >= 0 ? source2Segment[sourceSegment] : EngineSetting.INDEX_NOT_FOUND;
    }

    public void setSegmentOfSource(int sourceSegment, int segment) {

        if (source2Segment[sourceSegment] == EngineSetting.INDEX_NOT_FOUND)
            sourcesSet.add(sourceSegment);

        source2Segment[sourceSegment] = segment;
    }

    // Every segment's carried reach cleared and every segment marked hidden, ready to judge a level
    public void resetCarry() {

        int count = getSegmentCount();

        reach.size(count);
        kept.size(count);

        for (int segment = 0; segment < count; segment++) {
            reach.set(segment, 0f);
            kept.set(segment, false);
        }
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
        parent.clear();
        leafX.clear();
        leafY.clear();
        leafZ.clear();
        leafRadiusH.clear();
        leafRadiusV.clear();
        leafSeed.clear();
        leafAccent.clear();
        leafSegment.clear();

        for (int i = 0; i < sourcesSet.size(); i++)
            source2Segment[sourcesSet.getInt(i)] = EngineSetting.INDEX_NOT_FOUND;

        sourcesSet.clear();
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

    public int getParent(int segment) {
        return parent.getInt(segment);
    }

    public float getReach(int segment) {
        return reach.getFloat(segment);
    }

    public void setReach(int segment, float carried) {
        reach.set(segment, carried);
    }

    public boolean isKept(int segment) {
        return kept.getBoolean(segment);
    }

    public void setKept(int segment, boolean shown) {
        kept.set(segment, shown);
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

    public int getLeafSegment(int leaf) {
        return leafSegment.getInt(leaf);
    }

    public float[] getBounds() {
        return bounds;
    }

    public float[] getCells() {
        return cells;
    }

    public float[] getBox() {
        return box;
    }
}
