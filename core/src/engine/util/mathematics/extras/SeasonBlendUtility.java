package engine.util.mathematics.extras;

public final class SeasonBlendUtility {

    /*
     * Pure math for blending values keyed to keyframes spread across a
     * wrapped [0, 1) timeline — the shared core behind resolving a smooth
     * value (day length, color, or anything else) for a point in the year
     * from a calendar's own named season centers.
     */

    private SeasonBlendUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static double wrapFraction(double value) {
        double wrapped = value % 1.0;
        if (wrapped < 0)
            wrapped += 1.0;
        return wrapped;
    }

    public static double wrappedDiff(double a, double b) {
        double d = a - b;
        d = ((d + 0.5) % 1.0 + 1.0) % 1.0 - 0.5;
        return d;
    }

    public static double smoothstep(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    public static float lerp(float a, float b, double t) {
        return (float) (a + (b - a) * t);
    }

    /*
     * Locates `t` between two entries of a sorted, wrapped keyframe center
     * array and writes the eased local blend factor between them into
     * `out`. Handles both wraparound cases — t before the first center, and
     * t after the last. `sortedCenters` must be sorted ascending and
     * wrapped into [0, 1).
     */
    public static void resolve(double[] sortedCenters, int count, double t, SeasonBlendResultStruct out) {

        if (count == 1) {
            out.set(0, 0, 0.0);
            return;
        }

        int nextIndex = -1;

        for (int i = 0; i < count; i++) {
            if (sortedCenters[i] > t) {
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
            prevCenter = sortedCenters[prevIndex];
            nextCenter = sortedCenters[nextIndex] + 1.0;
        } else if (nextIndex == 0) {
            prevIndex = count - 1;
            prevCenter = sortedCenters[prevIndex] - 1.0;
            nextCenter = sortedCenters[nextIndex];
        } else {
            prevIndex = nextIndex - 1;
            prevCenter = sortedCenters[prevIndex];
            nextCenter = sortedCenters[nextIndex];
        }

        double span = nextCenter - prevCenter;
        double localT = (span <= 0.0) ? 0.0 : (t - prevCenter) / span;

        out.set(prevIndex, nextIndex, smoothstep(localT));
    }
}