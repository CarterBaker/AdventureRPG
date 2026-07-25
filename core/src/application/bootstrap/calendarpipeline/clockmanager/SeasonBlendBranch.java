package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.calendar.SeasonRangeStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SeasonBlendBranch extends BranchPackage {

    /*
     * Resolves the active calendar's named seasons into whatever the
     * current point in the year needs: daylight fraction, sky tint color,
     * and sunrise/sunset color. Each season's values are exact at that
     * season's own center date and blend smoothly toward its neighbor as
     * the year progresses, wrapping across the year boundary with no
     * discontinuity. All three quantities share one cached blend per frame
     * since they're evaluated from the same year progress.
     */

    // Internal
    private CalendarHandle calendarHandle;

    // Season Keyframes — sorted ascending by center
    private double[] seasonCenters;
    private float[] seasonDayLengths;
    private Vector3[] seasonTintColors;
    private Vector3[] seasonSunriseColors;
    private int seasonCount;

    // Cache
    private double lastYearProgress;
    private float lastDayLength;
    private final Vector3 lastTintColor = new Vector3();
    private final Vector3 lastSunriseColor = new Vector3();
    private float RECOMPUTE_EPSILON;

    // Internal \\

    @Override
    protected void create() {
        this.RECOMPUTE_EPSILON = EngineSetting.SEASON_BLEND_RECOMPUTE_EPSILON;
        this.lastYearProgress = -1.0;
        this.lastDayLength = 0.5f;
        this.lastTintColor.set(1f, 1f, 1f);
        this.lastSunriseColor.set(1f, 1f, 1f);
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
        Vector3[] rawTints = new Vector3[count];
        Vector3[] rawSunrises = new Vector3[count];

        for (int i = 0; i < count; i++) {
            double start = startFractions[i];
            double end = (i + 1 < count) ? startFractions[i + 1] : startFractions[0] + 1.0;
            rawCenters[i] = wrapFraction((start + end) * 0.5);
            rawDayLengths[i] = seasons.get(i).getDayLength();
            rawTints[i] = seasons.get(i).getTintColor();
            rawSunrises[i] = seasons.get(i).getSunriseColor();
        }

        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++)
            order[i] = i;

        java.util.Arrays.sort(order, (a, b) -> Double.compare(rawCenters[a], rawCenters[b]));

        this.seasonCount = count;
        this.seasonCenters = new double[count];
        this.seasonDayLengths = new float[count];
        this.seasonTintColors = new Vector3[count];
        this.seasonSunriseColors = new Vector3[count];

        for (int i = 0; i < count; i++) {
            seasonCenters[i] = rawCenters[order[i]];
            seasonDayLengths[i] = rawDayLengths[order[i]];
            seasonTintColors[i] = rawTints[order[i]];
            seasonSunriseColors[i] = rawSunrises[order[i]];
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
        resolveBlend(yearProgress);
        return lastDayLength;
    }

    Vector3 getTintColorForYearProgress(double yearProgress) {
        resolveBlend(yearProgress);
        return lastTintColor;
    }

    Vector3 getSunriseColorForYearProgress(double yearProgress) {
        resolveBlend(yearProgress);
        return lastSunriseColor;
    }

    private void resolveBlend(double yearProgress) {

        if (seasonCount == 0)
            return;

        if (seasonCount == 1) {
            lastDayLength = seasonDayLengths[0];
            lastTintColor.set(seasonTintColors[0].x, seasonTintColors[0].y, seasonTintColors[0].z);
            lastSunriseColor.set(seasonSunriseColors[0].x, seasonSunriseColors[0].y, seasonSunriseColors[0].z);
            return;
        }

        double t = wrapFraction(yearProgress);

        if (lastYearProgress >= 0.0 && Math.abs(wrappedDiff(t, lastYearProgress)) < RECOMPUTE_EPSILON)
            return;

        computeBlend(t);
        lastYearProgress = t;
    }

    private void computeBlend(double t) {

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

        lastDayLength = lerp(seasonDayLengths[prevIndex], seasonDayLengths[nextIndex], eased);

        Vector3 prevTint = seasonTintColors[prevIndex];
        Vector3 nextTint = seasonTintColors[nextIndex];
        lastTintColor.set(
                lerp(prevTint.x, nextTint.x, eased),
                lerp(prevTint.y, nextTint.y, eased),
                lerp(prevTint.z, nextTint.z, eased));

        Vector3 prevSunrise = seasonSunriseColors[prevIndex];
        Vector3 nextSunrise = seasonSunriseColors[nextIndex];
        lastSunriseColor.set(
                lerp(prevSunrise.x, nextSunrise.x, eased),
                lerp(prevSunrise.y, nextSunrise.y, eased),
                lerp(prevSunrise.z, nextSunrise.z, eased));
    }

    // Math Helpers \\

    private float lerp(float a, float b, double t) {
        return (float) (a + (b - a) * t);
    }

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