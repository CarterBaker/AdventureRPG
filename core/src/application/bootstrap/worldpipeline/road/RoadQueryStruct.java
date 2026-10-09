package application.bootstrap.worldpipeline.road;

import engine.root.StructPackage;

public class RoadQueryStruct extends StructPackage {

    /*
     * Output container for where a point stands against a planned road: its
     * distance from the centreline, the nearest point of the centreline and
     * how far along the road it lies, the road's surface height and span
     * there, and the road's heading there. Written in place — never
     * allocated per query.
     */

    // Internal
    private double distance;
    private double centerX;
    private double centerZ;
    private float along;
    private float surfaceY;
    private RoadSpanType span;
    private double headingX;
    private double headingZ;

    // Management \\

    public void set(
            double distance,
            double centerX,
            double centerZ,
            float along,
            float surfaceY,
            RoadSpanType span,
            double headingX,
            double headingZ) {

        this.distance = distance;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.along = along;
        this.surfaceY = surfaceY;
        this.span = span;
        this.headingX = headingX;
        this.headingZ = headingZ;
    }

    // Accessible \\

    public double getDistance() {
        return distance;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public float getAlong() {
        return along;
    }

    public float getSurfaceY() {
        return surfaceY;
    }

    public RoadSpanType getSpan() {
        return span;
    }

    public double getHeadingX() {
        return headingX;
    }

    public double getHeadingZ() {
        return headingZ;
    }
}
