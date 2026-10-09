package application.bootstrap.worldpipeline.road;

import engine.root.StructPackage;

public class RoadPathStruct extends StructPackage {

    /*
     * One planned run of road, immutable once planned: its road type and its
     * centreline as points a few blocks apart, in the unwrapped blocks of the
     * plan that laid it, each with the height of the road's surface there,
     * how the road meets the land there and how far along the road it lies.
     * The surface height counts in blocks like the ground does, its whole part
     * the block the surface is laid in and its fraction the half step a
     * smoothed slope lays above it. The bounds take in the road's half width
     * and shoulder, so a chunk tells whether the road can reach it at a
     * glance.
     */

    // Road
    private final RoadHandle roadHandle;

    // Points
    private final double[] pointX;
    private final double[] pointZ;
    private final float[] pointY;
    private final RoadSpanType[] pointSpans;
    private final float[] pointDistances;

    // Bounds — unwrapped blocks
    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;

    // Constructor \\

    public RoadPathStruct(
            RoadHandle roadHandle,
            double[] pointX,
            double[] pointZ,
            float[] pointY,
            RoadSpanType[] pointSpans,
            float[] pointDistances,
            double minX,
            double maxX,
            double minZ,
            double maxZ) {

        // Road
        this.roadHandle = roadHandle;

        // Points
        this.pointX = pointX;
        this.pointZ = pointZ;
        this.pointY = pointY;
        this.pointSpans = pointSpans;
        this.pointDistances = pointDistances;

        // Bounds
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    // Accessible \\

    public RoadHandle getRoadHandle() {
        return roadHandle;
    }

    public int getPointCount() {
        return pointX.length;
    }

    public double getPointX(int index) {
        return pointX[index];
    }

    public double getPointZ(int index) {
        return pointZ[index];
    }

    public float getPointY(int index) {
        return pointY[index];
    }

    public RoadSpanType getPointSpan(int index) {
        return pointSpans[index];
    }

    public float getPointDistance(int index) {
        return pointDistances[index];
    }

    public float getLength() {
        return pointDistances[pointDistances.length - 1];
    }

    public double getMinX() {
        return minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMaxZ() {
        return maxZ;
    }
}
