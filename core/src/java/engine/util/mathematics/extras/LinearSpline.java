package engine.util.mathematics.extras;

import engine.root.EngineUtility;

public final class LinearSpline extends EngineUtility {

    /*
     * Piecewise-linear curve evaluator over a small set of ascending control
     * points — used to translate a raw noise value into a shaped output
     * (terrain height, amplitude, whatever) via hand-tunable break points
     * instead of a raw linear or hard-coded formula. Values outside the
     * defined range clamp to the nearest endpoint rather than extrapolating.
     */

    private final float[] xPoints;
    private final float[] yPoints;

    public LinearSpline(float[] xPoints, float[] yPoints) {

        if (xPoints == null || yPoints == null || xPoints.length != yPoints.length || xPoints.length < 2)
            throwException("LinearSpline requires matching, non-null x/y arrays with at least 2 points");

        for (int i = 1; i < xPoints.length; i++)
            if (xPoints[i] <= xPoints[i - 1])
                throwException("LinearSpline x-points must be strictly ascending");

        this.xPoints = xPoints;
        this.yPoints = yPoints;
    }

    public float evaluate(float x) {

        if (x <= xPoints[0])
            return yPoints[0];

        int last = xPoints.length - 1;

        if (x >= xPoints[last])
            return yPoints[last];

        int index = findSegment(x);
        float segmentX0 = xPoints[index];
        float segmentX1 = xPoints[index + 1];
        float t = (x - segmentX0) / (segmentX1 - segmentX0);

        return yPoints[index] + (yPoints[index + 1] - yPoints[index]) * t;
    }

    private int findSegment(float x) {

        int low = 0;
        int high = xPoints.length - 2;

        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (xPoints[mid] <= x)
                low = mid;
            else
                high = mid - 1;
        }

        return low;
    }
}