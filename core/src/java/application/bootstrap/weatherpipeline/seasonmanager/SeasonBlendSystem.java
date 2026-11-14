package application.bootstrap.weatherpipeline.seasonmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.calendar.SeasonKeyframeStruct;
import application.bootstrap.calendarpipeline.calendar.SeasonRangeStruct;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SeasonBlendSystem extends SystemPackage {

    /*
     * Resolves where the year sits between the active calendar's season
     * keyframes once per frame, from the clock's continuous visual year
     * progress, and exposes the two surrounding seasons and the eased blend
     * between them. Every season-driven value — temperature, sky palette,
     * anything else — blends through this one result, so nothing steps at a
     * season boundary. Rebuilds itself whenever the clock's calendar
     * changes, so a world switch needs no extra call.
     */

    // Internal
    private ClockManager clockManager;
    private SeasonManager seasonManager;

    // Calendar
    private CalendarHandle calendarHandle;
    private SeasonKeyframeStruct keyframes;
    private SeasonHandle[] keyframeSeasons;

    // Blend
    private final int[] blendIndexScratch = new int[2];
    private SeasonHandle previousSeason;
    private SeasonHandle nextSeason;
    private float blendFactor;

    // Internal \\

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.seasonManager = get(SeasonManager.class);
    }

    @Override
    protected void awake() {
        resolveBlend();
    }

    @Override
    protected void update() {
        resolveBlend();
    }

    // Calendar \\

    private void assignCalendar(CalendarHandle calendarHandle) {

        this.calendarHandle = calendarHandle;
        this.keyframes = calendarHandle.getSeasonKeyframes();

        ObjectArrayList<SeasonRangeStruct> seasons = calendarHandle.getSeasons();
        int[] order = keyframes.getOrder();
        int count = keyframes.getCount();

        this.keyframeSeasons = new SeasonHandle[count];

        for (int i = 0; i < count; i++)
            keyframeSeasons[i] = seasonManager.getSeasonHandleFromSeasonName(seasons.get(order[i]).getName());
    }

    // Blend \\

    private void resolveBlend() {

        CalendarHandle activeCalendar = clockManager.getCalendarHandle();

        if (activeCalendar != calendarHandle)
            assignCalendar(activeCalendar);

        double yearProgress = clockManager.getClockHandle().getVisualYearProgress();
        double eased = keyframes.resolveEasedT(yearProgress, blendIndexScratch);

        this.previousSeason = keyframeSeasons[blendIndexScratch[0]];
        this.nextSeason = keyframeSeasons[blendIndexScratch[1]];
        this.blendFactor = (float) eased;
    }

    // Accessible \\

    SeasonHandle getPreviousSeason() {
        return previousSeason;
    }

    SeasonHandle getNextSeason() {
        return nextSeason;
    }

    float getBlendFactor() {
        return blendFactor;
    }

    float getBaseTemperature() {
        return lerp(previousSeason.getBaseTemperature(), nextSeason.getBaseTemperature());
    }

    float getTemperatureVariance() {
        return lerp(previousSeason.getTemperatureVariance(), nextSeason.getTemperatureVariance());
    }

    // Utility \\

    private float lerp(float from, float to) {
        return from + (to - from) * blendFactor;
    }
}
