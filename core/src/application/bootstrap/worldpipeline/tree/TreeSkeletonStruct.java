package application.bootstrap.worldpipeline.tree;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class TreeSkeletonStruct extends StructPackage {

    /*
     * One tree grown to maturity, in blocks from the centre of its root at
     * ground level. Its wood is a list of tapered segments, each listed after
     * the segment it grows from, set out from it a share of the way along it,
     * at a depth counting forks from the trunk, and born at the age it first
     * appears. Its foliage is a list of leaf clusters, each an upright
     * ellipsoid held by one segment and born with it. Grown once per tree,
     * then only read, so any thread may share it.
     */

    // Segments — positions and radii in blocks at maturity
    private final FloatArrayList startX;
    private final FloatArrayList startY;
    private final FloatArrayList startZ;
    private final FloatArrayList endX;
    private final FloatArrayList endY;
    private final FloatArrayList endZ;
    private final FloatArrayList startRadius;
    private final FloatArrayList endRadius;
    private final FloatArrayList birth;
    private final FloatArrayList attach;
    private final IntArrayList parent;
    private final IntArrayList depth;

    // Leaves
    private final FloatArrayList leafX;
    private final FloatArrayList leafY;
    private final FloatArrayList leafZ;
    private final FloatArrayList leafRadiusH;
    private final FloatArrayList leafRadiusV;
    private final IntArrayList leafSegment;

    // Size
    private float matureHeight;

    // Constructor \\

    public TreeSkeletonStruct() {

        // Segments
        this.startX = new FloatArrayList();
        this.startY = new FloatArrayList();
        this.startZ = new FloatArrayList();
        this.endX = new FloatArrayList();
        this.endY = new FloatArrayList();
        this.endZ = new FloatArrayList();
        this.startRadius = new FloatArrayList();
        this.endRadius = new FloatArrayList();
        this.birth = new FloatArrayList();
        this.attach = new FloatArrayList();
        this.parent = new IntArrayList();
        this.depth = new IntArrayList();

        // Leaves
        this.leafX = new FloatArrayList();
        this.leafY = new FloatArrayList();
        this.leafZ = new FloatArrayList();
        this.leafRadiusH = new FloatArrayList();
        this.leafRadiusV = new FloatArrayList();
        this.leafSegment = new IntArrayList();
    }

    // Growth \\

    // A segment from start to end, returning its index — INDEX_NOT_FOUND once the tree holds as many as it may
    public int addSegment(
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ,
            float fromRadius,
            float toRadius,
            int parentSegment,
            float attachShare,
            int segmentDepth,
            float segmentBirth) {

        if (startX.size() >= EngineSetting.TREE_MAX_SEGMENTS)
            return EngineSetting.INDEX_NOT_FOUND;

        startX.add(fromX);
        startY.add(fromY);
        startZ.add(fromZ);
        endX.add(toX);
        endY.add(toY);
        endZ.add(toZ);
        startRadius.add(fromRadius);
        endRadius.add(toRadius);
        parent.add(parentSegment);
        attach.add(attachShare);
        depth.add(segmentDepth);
        birth.add(segmentBirth);

        return startX.size() - 1;
    }

    public void addLeaf(float x, float y, float z, float radiusH, float radiusV, int segment) {

        if (leafX.size() >= EngineSetting.TREE_MAX_LEAF_CLUSTERS || segment == EngineSetting.INDEX_NOT_FOUND)
            return;

        leafX.add(x);
        leafY.add(y);
        leafZ.add(z);
        leafRadiusH.add(radiusH);
        leafRadiusV.add(radiusV);
        leafSegment.add(segment);
    }

    public void setMatureHeight(float matureHeight) {
        this.matureHeight = matureHeight;
    }

    // Accessible \\

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

    public float getBirth(int segment) {
        return birth.getFloat(segment);
    }

    public float getAttach(int segment) {
        return attach.getFloat(segment);
    }

    public int getParent(int segment) {
        return parent.getInt(segment);
    }

    public int getDepth(int segment) {
        return depth.getInt(segment);
    }

    public float getLength(int segment) {

        float dx = getEndX(segment) - getStartX(segment);
        float dy = getEndY(segment) - getStartY(segment);
        float dz = getEndZ(segment) - getStartZ(segment);

        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // True when the segment grows, through any number of forks, from the given ancestor
    public boolean isDescendant(int segment, int ancestor) {

        int current = getParent(segment);

        while (current != EngineSetting.INDEX_NOT_FOUND) {

            if (current == ancestor)
                return true;

            current = getParent(current);
        }

        return false;
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

    public int getLeafSegment(int leaf) {
        return leafSegment.getInt(leaf);
    }

    public float getMatureHeight() {
        return matureHeight;
    }
}
