package application.bootstrap.worldpipeline.tree;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class TreeCastStruct extends StructPackage {

    /*
     * Where a ray last met a tree: the tree, whether it struck wood or a leaf
     * cluster, how far along the ray, and the point it met in the caller's
     * chunk frame. Wood carries the sub-voxel it entered, from the centre of
     * corner of the block the tree's root stands in, the axis and side of the face it came in through, and
     * the node it lies in; a leaf hit carries the cluster's index in the
     * tree's current shape. Filled in place by every cast and reused.
     */

    // Hit
    private boolean hit;
    private TreeInstance tree;
    private boolean wood;
    private float distance;
    private float pointX;
    private float pointY;
    private float pointZ;

    // Wood — sub-voxels from the corner of the root's block
    private int cellX;
    private int cellY;
    private int cellZ;
    private int faceAxis;
    private boolean facePositive;
    private int node;

    // Leaf
    private int leaf;

    // Management \\

    public void clear() {
        this.hit = false;
        this.tree = null;
        this.wood = false;
        this.distance = Float.MAX_VALUE;
        this.node = EngineSetting.INDEX_NOT_FOUND;
        this.leaf = EngineSetting.INDEX_NOT_FOUND;
    }

    public void setWood(
            TreeInstance tree,
            float distance,
            int cellX,
            int cellY,
            int cellZ,
            int faceAxis,
            boolean facePositive,
            int node) {

        this.hit = true;
        this.tree = tree;
        this.wood = true;
        this.distance = distance;
        this.cellX = cellX;
        this.cellY = cellY;
        this.cellZ = cellZ;
        this.faceAxis = faceAxis;
        this.facePositive = facePositive;
        this.node = node;
        this.leaf = EngineSetting.INDEX_NOT_FOUND;
    }

    public void setLeaf(TreeInstance tree, float distance, int leaf) {
        this.hit = true;
        this.tree = tree;
        this.wood = false;
        this.distance = distance;
        this.node = EngineSetting.INDEX_NOT_FOUND;
        this.leaf = leaf;
    }

    public void setPoint(float pointX, float pointY, float pointZ) {
        this.pointX = pointX;
        this.pointY = pointY;
        this.pointZ = pointZ;
    }

    // Accessible \\

    public boolean isHit() {
        return hit;
    }

    public TreeInstance getTree() {
        return tree;
    }

    public boolean isWood() {
        return wood;
    }

    public float getDistance() {
        return distance;
    }

    public float getPointX() {
        return pointX;
    }

    public float getPointY() {
        return pointY;
    }

    public float getPointZ() {
        return pointZ;
    }

    public int getCellX() {
        return cellX;
    }

    public int getCellY() {
        return cellY;
    }

    public int getCellZ() {
        return cellZ;
    }

    public int getFaceAxis() {
        return faceAxis;
    }

    public boolean isFacePositive() {
        return facePositive;
    }

    public int getNode() {
        return node;
    }

    public int getLeaf() {
        return leaf;
    }
}
