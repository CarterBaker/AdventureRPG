package engine.util.mathematics.curves;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class CurveUtility extends EngineUtility {

    /*
     * Stateless arithmetic every curve of keys shares: the key at or before
     * a moment by binary search, a cubic Bezier segment and its slope, the
     * share of the way along a segment at which its time reaches a moment,
     * and the slope through a key that keeps the curve from overshooting
     * either neighbour. None of it allocates.
     */

    // Search \\

    // The last time at or before the moment, -1 before the first
    public static int findAtOrBefore(FloatArrayList times, float time) {

        int low = 0;
        int high = times.size() - 1;
        int found = EngineSetting.INDEX_NOT_FOUND;

        while (low <= high) {

            int middle = (low + high) >>> 1;

            if (times.getFloat(middle) <= time) {
                found = middle;
                low = middle + 1;
            }

            else
                high = middle - 1;
        }

        return found;
    }

    // Bezier \\

    public static double bezier(double start, double startHandle, double endHandle, double end, double u) {

        double v = 1.0 - u;

        return v * v * v * start + 3.0 * v * v * u * startHandle + 3.0 * v * u * u * endHandle + u * u * u * end;
    }

    public static double bezierSlope(double start, double startHandle, double endHandle, double end, double u) {

        double v = 1.0 - u;

        return 3.0 * v * v * (startHandle - start) + 6.0 * v * u * (endHandle - startHandle)
                + 3.0 * u * u * (end - endHandle);
    }

    // The share of the way along a segment at which it reaches the moment, its time running forward throughout
    public static double solveBezier(
            float time,
            double startTime,
            double startHandle,
            double endHandle,
            double endTime) {

        double low = 0.0;
        double high = 1.0;
        double u = (time - startTime) / (endTime - startTime);

        for (int i = 0; i < EngineSetting.CURVE_SOLVE_STEPS; i++) {

            double miss = bezier(startTime, startHandle, endHandle, endTime, u) - time;

            if (Math.abs(miss) < EngineSetting.CURVE_SOLVE_EPSILON)
                return u;

            if (miss > 0.0)
                high = u;
            else
                low = u;

            double slope = bezierSlope(startTime, startHandle, endHandle, endTime, u);
            double next = slope != 0.0 ? u - miss / slope : low;

            u = next > low && next < high ? next : (low + high) / 2.0;
        }

        return u;
    }

    // Slopes \\

    // The slope through a key between two spans that never overshoots either neighbour: flat where the curve turns,
    // else a mean of the slopes either side weighted by their spans
    public static double resolveSmoothSlope(double before, double after, double spanBefore, double spanAfter) {

        if (before == 0.0 || after == 0.0 || before > 0.0 != after > 0.0)
            return 0.0;

        double weightBefore = 2.0 * spanAfter + spanBefore;
        double weightAfter = spanAfter + 2.0 * spanBefore;

        return (weightBefore + weightAfter) / (weightBefore / before + weightAfter / after);
    }
}
