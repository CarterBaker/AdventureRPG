package application.bootstrap.weatherpipeline.skymanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.calendar.SeasonKeyframeStruct;
import application.bootstrap.calendarpipeline.calendar.SeasonRangeStruct;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SeasonBlendSystem extends SystemPackage {

    /*
     * Resolves the calendar's own season date keyframes into a smoothly
     * blended sky tint and sunrise color for the current point in the
     * year. Keyframe centers and blend resolution come from
     * CalendarHandle.getSeasonKeyframes(); the color values themselves
     * come from each season's own SeasonHandle.
     */

    private SeasonManager seasonManager;

    private SeasonKeyframeStruct keyframes;
    private Vector3[] seasonTintColors;
    private Vector3[] seasonSunriseColors;

    private double lastYearProgress;
    private final Vector3 lastTintColor = new Vector3();
    private final Vector3 lastSunriseColor = new Vector3();

    private final int[] blendIndexScratch = new int[2];

    // Internal \\

    @Override
    protected void create() {
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
        this.keyframes = calendarHandle.getSeasonKeyframes();
        buildSeasonColors(calendarHandle);
        this.lastYearProgress = -1.0;
    }

    private void buildSeasonColors(CalendarHandle calendarHandle) {

        ObjectArrayList<SeasonRangeStruct> seasons = calendarHandle.getSeasons();
        int[] order = keyframes.getOrder();
        int count = keyframes.getCount();

        this.seasonTintColors = new Vector3[count];
        this.seasonSunriseColors = new Vector3[count];

        for (int i = 0; i < count; i++) {
            SeasonHandle seasonHandle = seasonManager.getSeasonHandleFromSeasonName(seasons.get(order[i]).getName());
            seasonTintColors[i] = seasonHandle.getTintColor();
            seasonSunriseColors[i] = seasonHandle.getSunriseColor();
        }
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

        int count = keyframes.getCount();

        if (count == 0)
            return;

        if (count == 1) {
            lastTintColor.set(seasonTintColors[0].x, seasonTintColors[0].y, seasonTintColors[0].z);
            lastSunriseColor.set(seasonSunriseColors[0].x, seasonSunriseColors[0].y, seasonSunriseColors[0].z);
            return;
        }

        double t = wrapFraction(yearProgress);

        if (lastYearProgress >= 0.0
                && Math.abs(wrappedDiff(t, lastYearProgress)) < EngineSetting.SEASON_BLEND_RECOMPUTE_EPSILON)
            return;

        double eased = keyframes.resolveEasedT(yearProgress, blendIndexScratch);

        int prevIndex = blendIndexScratch[0];
        int nextIndex = blendIndexScratch[1];

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

        lastYearProgress = t;
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

    private float lerp(float a, float b, double t) {
        return (float) (a + (b - a) * t);
    }
}