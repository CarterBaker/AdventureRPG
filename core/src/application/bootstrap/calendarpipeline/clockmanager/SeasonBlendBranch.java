package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.calendar.SeasonRangeStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SeasonBlendBranch extends BranchPackage {

    /*
     * Resolves the current daylight fraction (0-1 of a full day/night cycle)
     * from the active calendar's named seasons. Each season's dayLength is
     * exact at that season's own center date — the midpoint between its
     * start and the next season's start — and the value smoothly blends
     * toward the neighboring season's dayLength as the year progresses away
     * from one center and toward the next, wrapping across the year boundary
     * with no discontinuity. Results are cached and only recomputed once
     * yearProgress has moved enough to matter, since this is evaluated every
     * frame but the underlying value changes at most once a game-day.
     */

    // Internal
    private CalendarHandle calendarHandle;

    // Season Keyframes — sorted ascending by center
    private double[] seasonCenters;
    private float[] seasonDayLengths;
    private int seasonCount;

    // Cache
    private double lastYearProgress;
    private float lastDayLength;
    private float RECOMPUTE_EPSILON;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.RECOMPUTE_EPSILON = EngineSetting.SEASON_BLEND_RECOMPUTE_EPSILON;

        // Cache
        this.lastYearProgress = -1.0;
        this.lastDayLength = 0.5f;
    }

    // Assignment \\

    void assignData(CalendarHandle calendarHandle) {
        this.calendarHandle = calendarHandle;
        buildSeasonKeyframes();
        this.lastYearProgress = -1.0;
    }

    // Keyframe Construction \\

    private void buildSeasonKeyframes() {

        ObjectArrayList<SeasonRangeStruct> seasons = calendarHandle.getSeasons();
        int count = seasons.size();
        int totalDaysInYear = calendarHandle.getTotalDaysInYear();

        double[] startFractions = new double[count];

        for (int i = 0; i < count; i++) {
            SeasonRangeStruct season = seasons.get(i);
            int dayOfYear = dayOfYearZeroIndexed(season.getStartMonth(), season.getStartDayOfMonth());
            startFractions[i] = dayOfYear / (double) totalDaysInYear;
        }

        double[] rawCenters = new double[count];
        float[] rawDayLengths = new float[count];

        for (int i = 0; i < count; i++) {
            double start = startFractions[i];
            double end = (i + 1 < count) ? startFractions[i + 1] : startFractions[0] + 1.0;
            rawCenters[i] = wrapFraction((start + end) * 0.5);
            rawDayLengths[i] = seasons.get(i).getDayLength();
        }

        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++)
            order[i] = i;

        java.util.Arrays.sort(order, (a, b) -> Double.compare(rawCenters[a], rawCenters[b]));

        this.seasonCount = count;
        this.seasonCenters = new double[count];
        this.seasonDayLengths = new float[count];

        for (int i = 0; i < count; i++) {
            seasonCenters[i] = rawCenters[order[i]];
            seasonDayLengths[i] = rawDayLengths[order[i]];
        }
    }

    private int dayOfYearZeroIndexed(int month, int dayOfMonth) {

        int dayOfYear = 0;

        for (int i = 0; i < month; i++)
            dayOfYear += calendarHandle.getMonthDays(i);

        dayOfYear += dayOfMonth - 1;

        return dayOfYear;
    }

    // Blend \\

    float getDayLengthForYearProgress(double yearProgress) {

        if (seasonCount == 0)
            return 0.5f;

        if (seasonCount == 1)
            return seasonDayLengths[0];

        double t = wrapFraction(yearProgress);

        if (lastYearProgress >= 0.0 && Math.abs(wrappedDiff(t, lastYearProgress)) < RECOMPUTE_EPSILON)
            return lastDayLength;

        float result = computeBlend(t);

        lastYearProgress = t;
        lastDayLength = result;

        return result;
    }

    private float computeBlend(double t) {

        int nextIndex = -1;

        for (int i = 0; i < seasonCount; i++) {
            if (seasonCenters[i] > t) {
                nextIndex = i;
                break;
            }
        }

        int prevIndex;
        double prevCenter;
        double nextCenter;

        if (nextIndex == -1) {
            prevIndex = seasonCount - 1;
            nextIndex = 0;
            prevCenter = seasonCenters[prevIndex];
            nextCenter = seasonCenters[nextIndex] + 1.0;
        } else if (nextIndex == 0) {
            prevIndex = seasonCount - 1;
            prevCenter = seasonCenters[prevIndex] - 1.0;
            nextCenter = seasonCenters[nextIndex];
        } else {
            prevIndex = nextIndex - 1;
            prevCenter = seasonCenters[prevIndex];
            nextCenter = seasonCenters[nextIndex];
        }

        double span = nextCenter - prevCenter;
        double localT = (span <= 0.0) ? 0.0 : (t - prevCenter) / span;
        double eased = smoothstep(localT);

        float prevLength = seasonDayLengths[prevIndex];
        float nextLength = seasonDayLengths[nextIndex];

        return (float) (prevLength + (nextLength - prevLength) * eased);
    }

    // Math Helpers \\

    private double smoothstep(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    private double wrapFraction(double value) {
        double wrapped = value % 1.0;
        if (wrapped < 0)
            wrapped += 1.0;
        return wrapped;
    }

    private double wrappedDiff(double a, double b) {
        double d = a - b;
        d = ((d + 0.5) % 1.0 + 1.0) % 1.0 - 0.5;
        return d;
    }
}