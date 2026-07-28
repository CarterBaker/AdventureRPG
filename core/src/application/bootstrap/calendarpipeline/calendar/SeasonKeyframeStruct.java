package application.bootstrap.calendarpipeline.calendar;

import engine.root.StructPackage;

public class SeasonKeyframeStruct extends StructPackage {

    /*
     * A calendar's named seasons resolved into sorted, wrapped keyframe
     * centers — each season's center date expressed as a [0, 1) fraction
     * of the year. order[i] gives the original season index backing
     * sorted slot i, so callers can build their own per-season value
     * arrays (day length, color, anything else) in the same sorted order.
     */

    private final double[] centers;
    private final int[] order;
    private final int count;

    public SeasonKeyframeStruct(double[] centers, int[] order, int count) {
        this.centers = centers;
        this.order = order;
        this.count = count;
    }

    public double[] getCenters() {
        return centers;
    }

    public int[] getOrder() {
        return order;
    }

    public int getCount() {
        return count;
    }
}