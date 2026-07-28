package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.calendar.SeasonKeyframeStruct;
import application.bootstrap.calendarpipeline.calendar.SeasonRangeStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.SeasonBlendResultStruct;
import engine.util.mathematics.extras.SeasonBlendUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SeasonBlendBranch extends BranchPackage {

    /*
     * Resolves the active calendar's named seasons into a smoothly blended
     * day length for the current point in the year, used to bend the
     * calendar's own visual time-of-day curve. Keyframe centers come from
     * CalendarHandle.getSeasonKeyframes(); this branch only owns the
     * day-length values themselves and the recompute caching.
     */

    private SeasonKeyframeStruct keyframes;
    private float[] seasonDayLengths;

    private double lastYearProgress;
    private float lastDayLength;
    private float RECOMPUTE_EPSILON;

    private final SeasonBlendResultStruct blendResult = new SeasonBlendResultStruct();

    // Internal \\

    @Override
    protected void create() {
        this.RECOMPUTE_EPSILON = EngineSetting.SEASON_BLEND_RECOMPUTE_EPSILON;
        this.lastYearProgress = -1.0;
        this.lastDayLength = 0.5f;
    }

    // Assignment \\

    void assignData(CalendarHandle calendarHandle) {
        this.keyframes = calendarHandle.getSeasonKeyframes();
        buildDayLengths(calendarHandle);
        this.lastYearProgress = -1.0;
    }

    private void buildDayLengths(CalendarHandle calendarHandle) {

        ObjectArrayList<SeasonRangeStruct> seasons = calendarHandle.getSeasons();
        int[] order = keyframes.getOrder();
        int count = keyframes.getCount();

        this.seasonDayLengths = new float[count];

        for (int i = 0; i < count; i++)
            seasonDayLengths[i] = seasons.get(order[i]).getDayLength();
    }

    // Blend \\

    float getDayLengthForYearProgress(double yearProgress) {
        resolveBlend(yearProgress);
        return lastDayLength;
    }

    private void resolveBlend(double yearProgress) {

        int count = keyframes.getCount();

        if (count == 0)
            return;

        if (count == 1) {
            lastDayLength = seasonDayLengths[0];
            return;
        }

        double t = SeasonBlendUtility.wrapFraction(yearProgress);

        if (lastYearProgress >= 0.0
                && Math.abs(SeasonBlendUtility.wrappedDiff(t, lastYearProgress)) < RECOMPUTE_EPSILON)
            return;

        SeasonBlendUtility.resolve(keyframes.getCenters(), count, t, blendResult);

        lastDayLength = SeasonBlendUtility.lerp(
                seasonDayLengths[blendResult.getPrevIndex()],
                seasonDayLengths[blendResult.getNextIndex()],
                blendResult.getEasedT());

        lastYearProgress = t;
    }
}