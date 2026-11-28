package application.bootstrap.worldpipeline.world;

import application.bootstrap.worldpipeline.util.GlobeUtility;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3Double;

public class GlobeCapStruct extends StructPackage {

    /*
     * A round patch of a globe, its middle a globe-frame direction and its
     * reach an angle from it: what a run of edits to a globe's image touches.
     * A span of pixels on one image face is taken as the least cap holding
     * its corners, widened by a margin, and every span joined is held in the
     * least cap holding both; a cap reaching past a hemisphere gives up and
     * reaches everything. A place is reached when its direction lies within
     * the cap's reach widened by the place's own.
     */

    // Cap
    private final Vector3Double center = new Vector3Double();
    private double radius;
    private boolean empty;
    private boolean everything;

    // Scratch
    private final Vector3Double directionScratch = new Vector3Double();

    // Constructor \\

    public GlobeCapStruct() {
        clear();
    }

    // Management \\

    public void clear() {
        this.radius = 0.0;
        this.empty = true;
        this.everything = false;
    }

    public void includeEverything() {
        this.everything = true;
        this.empty = false;
    }

    // A span of image pixels, inclusive, widened by a margin in radians; a span over more than one image face reaches
    // everything
    public void includeImagePixels(
            WorldHandle worldHandle,
            int minPixelX,
            int minPixelZ,
            int maxPixelX,
            int maxPixelZ,
            double marginRadians) {

        int faceTexels = worldHandle.getWorld().getHeight();

        if (Math.floorDiv(minPixelX, faceTexels) != Math.floorDiv(maxPixelX, faceTexels)) {
            includeEverything();
            return;
        }

        GlobeUtility.resolveImageDirection(
                faceTexels, (minPixelX + maxPixelX + 1) * 0.5, (minPixelZ + maxPixelZ + 1) * 0.5, directionScratch);
        directionScratch.normalize();

        double centerX = directionScratch.x;
        double centerY = directionScratch.y;
        double centerZ = directionScratch.z;
        double reach = Math.max(
                Math.max(cornerAngle(faceTexels, minPixelX, minPixelZ, centerX, centerY, centerZ),
                        cornerAngle(faceTexels, maxPixelX + 1, minPixelZ, centerX, centerY, centerZ)),
                Math.max(cornerAngle(faceTexels, minPixelX, maxPixelZ + 1, centerX, centerY, centerZ),
                        cornerAngle(faceTexels, maxPixelX + 1, maxPixelZ + 1, centerX, centerY, centerZ)))
                + marginRadians;

        join(centerX, centerY, centerZ, reach);
    }

    private double cornerAngle(int faceTexels, int pixelX, int pixelZ, double centerX, double centerY, double centerZ) {

        GlobeUtility.resolveImageDirection(faceTexels, pixelX, pixelZ, directionScratch);
        directionScratch.normalize();

        return angleBetween(directionScratch.x, directionScratch.y, directionScratch.z, centerX, centerY, centerZ);
    }

    // The least cap holding the cap held and another, everything once it would reach past a hemisphere
    private void join(double centerX, double centerY, double centerZ, double reach) {

        if (everything)
            return;

        if (empty) {
            center.set(centerX, centerY, centerZ);
            this.radius = reach;
            this.empty = false;
            return;
        }

        double apart = angleBetween(center.x, center.y, center.z, centerX, centerY, centerZ);

        if (apart + reach <= radius)
            return;

        if (apart + radius <= reach) {
            center.set(centerX, centerY, centerZ);
            this.radius = reach;
            return;
        }

        double joined = (apart + radius + reach) * 0.5;

        if (joined >= Math.PI * 0.5) {
            includeEverything();
            return;
        }

        double share = (joined - radius) / apart;
        double sinApart = Math.sin(apart);
        double from = Math.sin((1.0 - share) * apart) / sinApart;
        double to = Math.sin(share * apart) / sinApart;

        center.set(
                center.x * from + centerX * to,
                center.y * from + centerY * to,
                center.z * from + centerZ * to).normalize();
        this.radius = joined;
    }

    // Reach \\

    // Whether a place whose unit direction is given, reaching an angle of its own, meets the cap
    public boolean reaches(double x, double y, double z, double placeRadians) {

        if (empty)
            return false;

        return everything || angleBetween(x, y, z, center.x, center.y, center.z) <= radius + placeRadians;
    }

    private static double angleBetween(double ax, double ay, double az, double bx, double by, double bz) {
        return Math.acos(Math.max(-1.0, Math.min(1.0, ax * bx + ay * by + az * bz)));
    }

    // Accessible \\

    public boolean isEmpty() {
        return empty;
    }
}
