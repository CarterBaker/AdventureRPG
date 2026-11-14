package application.bootstrap.worldpipeline.tree;

import engine.root.StructPackage;

public class TreeBranchStruct extends StructPackage {

    /*
     * How a species branches: the share of its height below the crown, how
     * many limbs or whorls set out, at what angle from upright, how long a
     * limb runs as a share of the tree's height, how many times limbs fork
     * and into how many twigs each, each fork shorter, steeper and thinner by
     * its ratios, how much every limb bends toward or away from the ground,
     * the turn between one limb and the next around the trunk, and how many
     * segments each limb is drawn with.
     */

    private final float crownStart;
    private final int minCount;
    private final int maxCount;
    private final float angleDegrees;
    private final float length;
    private final int levels;
    private final int children;
    private final float childLength;
    private final float childAngleDegrees;
    private final float radiusRatio;
    private final float gravity;
    private final float twistDegrees;
    private final int segments;

    public TreeBranchStruct(
            float crownStart,
            int minCount,
            int maxCount,
            float angleDegrees,
            float length,
            int levels,
            int children,
            float childLength,
            float childAngleDegrees,
            float radiusRatio,
            float gravity,
            float twistDegrees,
            int segments) {

        this.crownStart = crownStart;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.angleDegrees = angleDegrees;
        this.length = length;
        this.levels = levels;
        this.children = children;
        this.childLength = childLength;
        this.childAngleDegrees = childAngleDegrees;
        this.radiusRatio = radiusRatio;
        this.gravity = gravity;
        this.twistDegrees = twistDegrees;
        this.segments = segments;
    }

    // Accessible \\

    public float getCrownStart() {
        return crownStart;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public float getAngleDegrees() {
        return angleDegrees;
    }

    public float getLength() {
        return length;
    }

    public int getLevels() {
        return levels;
    }

    public int getChildren() {
        return children;
    }

    public float getChildLength() {
        return childLength;
    }

    public float getChildAngleDegrees() {
        return childAngleDegrees;
    }

    public float getRadiusRatio() {
        return radiusRatio;
    }

    public float getGravity() {
        return gravity;
    }

    public float getTwistDegrees() {
        return twistDegrees;
    }

    public int getSegments() {
        return segments;
    }
}
