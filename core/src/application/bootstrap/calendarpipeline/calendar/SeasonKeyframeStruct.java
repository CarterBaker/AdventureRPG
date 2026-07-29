package application.bootstrap.calendarpipeline.calendar;

import engine.root.StructPackage;

public class SeasonKeyframeStruct extends StructPackage {

    /*
     * A calendar's named seasons resolved into sorted, wrapped keyframe
     * centers — each season's center date expressed as a [0, 1) fraction
     * of the year. order[i] gives the original season index backing
     * sorted slot i, so callers can build their own per-season value
     * arrays (day length, color, anything else) in the same sorted order.
     * resolveEasedT() is the single shared entry point for blending any
     * such per-season value array against a point in the year.
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

    /*
     * Locates yearProgress between two keyframe centers and returns the
     * eased local blend factor between them. Writes the sorted indices of
     * the surrounding keyframes into indexOut[0] (previous) and
     * indexOut[1] (next) — indexOut is caller-owned scratch of at least
     * length 2, never allocated here. Handles wraparound at both ends of
     * the year and the degenerate single-season case.
     */
    public double resolveEasedT(double yearProgress, int[] indexOut) {

        double t = wrapFraction(yearProgress);

        if (count == 1) {
            indexOut[0] = 0;
            indexOut[1] = 0;
            return 0.0;
        }

        int nextIndex = -1;

        for (int i = 0; i < count; i++) {
            if (centers[i] > t) {
                nextIndex = i;
                break;
            }
        }

        int prevIndex;
        double prevCenter;
        double nextCenter;

        if (nextIndex == -1) {
            prevIndex = count - 1;
            nextIndex = 0;
            prevCenter = centers[prevIndex];
            nextCenter = centers[nextIndex] + 1.0;
        } else if (nextIndex == 0) {
            prevIndex = count - 1;
            prevCenter = centers[prevIndex] - 1.0;
            nextCenter = centers[nextIndex];
        } else {
            prevIndex = nextIndex - 1;
            prevCenter = centers[prevIndex];
            nextCenter = centers[nextIndex];
        }

        indexOut[0] = prevIndex;
        indexOut[1] = nextIndex;

        double span = nextCenter - prevCenter;
        double localT = (span <= 0.0) ? 0.0 : (t - prevCenter) / span;

        return smoothstep(localT);
    }

    private double wrapFraction(double value) {
        double wrapped = value % 1.0;
        if (wrapped < 0)
            wrapped += 1.0;
        return wrapped;
    }

    private double smoothstep(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }
}