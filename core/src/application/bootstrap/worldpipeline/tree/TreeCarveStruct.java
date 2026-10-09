package application.bootstrap.worldpipeline.tree;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class TreeCarveStruct extends StructPackage {

    /*
     * One notch an axe struck out of a tree, in sub-voxels from the centre of
     * the tree's root. The axe swings level, so a notch is a wedge lying flat:
     * it runs from where the blade met the wood along the swing's heading as
     * deep as the stroke bit, as wide as the stroke swept across, and as tall
     * at its mouth as the blade, narrowing toward its back. Immutable.
     */

    // Mouth — the middle of the notch's opening
    private final float centerX;
    private final float centerY;
    private final float centerZ;

    // Heading — level and unit length
    private final float forwardX;
    private final float forwardZ;

    // Size — sub-voxels
    private final float halfWidth;
    private final float halfHeight;
    private final float depth;

    // Constructor \\

    public TreeCarveStruct(
            float centerX,
            float centerY,
            float centerZ,
            float forwardX,
            float forwardZ,
            float halfWidth,
            float halfHeight,
            float depth) {

        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.forwardX = forwardX;
        this.forwardZ = forwardZ;
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.depth = depth;
    }

    // Containment \\

    // True when a point, in sub-voxels from the root, lies inside the notch
    public boolean contains(float x, float y, float z) {

        float dx = x - centerX;
        float dy = y - centerY;
        float dz = z - centerZ;
        float along = dx * forwardX + dz * forwardZ;

        if (along < 0f || along > depth)
            return false;

        float side = dz * forwardX - dx * forwardZ;

        if (Math.abs(side) > halfWidth)
            return false;

        float taper = 1f - along / depth * EngineSetting.TREE_CHOP_WEDGE_TAPER;

        return Math.abs(dy) <= halfHeight * taper;
    }

    // Bounds \\

    // How far from its mouth the notch can reach on any level axis
    public float getReach() {
        return depth + halfWidth;
    }

    // Accessible \\

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getCenterZ() {
        return centerZ;
    }

    public float getForwardX() {
        return forwardX;
    }

    public float getForwardZ() {
        return forwardZ;
    }

    public float getHalfWidth() {
        return halfWidth;
    }

    public float getHalfHeight() {
        return halfHeight;
    }

    public float getDepth() {
        return depth;
    }
}
