package application.bootstrap.weatherpipeline.skymanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.calendar.SeasonRangeStruct;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SeasonColorBlendBranch extends BranchPackage {

    /*
     * Resolves the calendar's own season date keyframes into a smoothly
     * blended sky tint and sunrise color for the current point in the
     * year. Each season's colors are exact at that season's own calendar
     * center date and blend toward its neighbor as the year progresses,
     * wrapping across the year boundary with no discontinuity. Keyframe
     * dates come from the active CalendarHandle; the color values
     * themselves come from each season's own SeasonHandle.
     */

    private SeasonManager seasonManager;

    private double[] seasonCenters;
    private Vector3[] seasonTintColors;
    private Vector3[] seasonSunriseColors;
    private int seasonCount;

    private double lastYearProgress;
    private final Vector3 lastTintColor = new Vector3();
    private final Vector3 lastSunriseColor = new Vector3();
    private float RECOMPUTE_EPSILON;

    // Internal \\

    @Override
    protected void create() {
        this.RECOMPUTE_EPSILON = EngineSetting.SEASON_BLEND_RECOMPUTE_EPSILON;
        this.lastYearProgress = -1.0;
        this.lastTintColor.set(1f, 1f, 1f);
        this.lastSunriseColor.set(1f, 1f, 1f);
    }

    @Override
    protected void get() {
        this.seasonManager = get(SeasonManager.class);
    }

    // Assignment \\

    void assignData(CalendarHandle calendarHandle) {
        buildSeasonKeyframes(calendarHandle);
        this.lastYearProgress = -1.0;
    }

    // Keyframe Construction \\

    private void buildSeasonKeyframes(CalendarHandle calendarHandle) {

        ObjectArrayList<SeasonRangeStruct> seasons = calendarHandle.getSeasons();
        int count = seasons.size();
        int totalDaysInYear = calendarHandle.getTotalDaysInYear();

        double[] startFractions = new double[count];

        for (int i = 0; i < count; i++) {
            SeasonRangeStruct season = seasons.get(i);
            int dayOfYear = dayOfYearZeroIndexed(calendarHandle, season.getStartMonth(), season.getStartDayOfMonth());
            startFractions[i] = dayOfYear / (double) totalDaysInYear;
        }

        double[] rawCenters = new double[count];
        Vector3[] rawTints = new Vector3[count];
        Vector3[] rawSunrises = new Vector3[count];

        for (int i = 0; i < count; i++) {
            double start = startFractions[i];
            double end = (i + 1 < count) ? startFractions[i + 1] : startFractions[0] + 1.0;
            rawCenters[i] = wrapFraction((start + end) * 0.5);

            SeasonHandle seasonHandle = seasonManager.getSeasonHandleFromSeasonName(seasons.get(i).getName());
            rawTints[i] = seasonHandle.getTintColor();
            rawSunrises[i] = seasonHandle.getSunriseColor();
        }

        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++)
            order[i] = i;

        java.util.Arrays.sort(order, (a, b) -> Double.compare(rawCenters[a], rawCenters[b]));

        this.seasonCount = count;
        this.seasonCenters = new double[count];
        this.seasonTintColors = new Vector3[count];
        this.seasonSunriseColors = new Vector3[count];

        for (int i = 0; i < count; i++) {
            seasonCenters[i] = rawCenters[order[i]];
            seasonTintColors[i] = rawTints[order[i]];
            seasonSunriseColors[i] = rawSunrises[order[i]];
        }
    }

    private int dayOfYearZeroIndexed(CalendarHandle calendarHandle, int month, int dayOfMonth) {

        int dayOfYear = 0;

        for (int i = 0; i < month; i++)
            dayOfYear += calendarHandle.getMonthDays(i);

        dayOfYear += dayOfMonth - 1;

        return dayOfYear;
    }

    // Blend \\

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