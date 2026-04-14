package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class CurrentTrackerBranch extends BranchPackage {

    /*
     * Advances the sub-day clock each frame. Derives day progress from the
     * real system clock modulo the game day length so time of day is always
     * consistent regardless of session start time. Derives total days elapsed
     * from the world epoch so the in-game date accumulates correctly across
     * sessions. Returns true when the day rolls over.
     */

    // Internal
    private int MINUTES_PER_HOUR;
    private int HOURS_PER_DAY;
    private float MIDDAY_OFFSET;
    private long MILLIS_PER_REAL_DAY;

    // Seasonal Bending
    private double MAX_SEASON_SHIFT;
    private double SUNRISE_MIN;
    private double SUNRISE_MAX;
    private double SUNSET_MIN;
    private double SUNSET_MAX;
    private double MIDNIGHT;
    private double NOON;
    private double QUARTER;
    private double THREE_QUARTERS;

    // Per-world
    private float daysPerDay;
    private ClockHandle clockHandle;

    // Tracking
    private long lastDay;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.MINUTES_PER_HOUR = EngineSetting.MINUTES_PER_HOUR;
        this.HOURS_PER_DAY = EngineSetting.HOURS_PER_DAY;
        this.MIDDAY_OFFSET = EngineSetting.MIDDAY_OFFSET;
        this.MILLIS_PER_REAL_DAY = EngineSetting.MILLIS_PER_REAL_DAY;

        // Seasonal Bending
        this.MAX_SEASON_SHIFT = EngineSetting.CLOCK_MAX_SEASON_SHIFT;
        this.SUNRISE_MIN = EngineSetting.CLOCK_SUNRISE_MIN;
        this.SUNRISE_MAX = EngineSetting.CLOCK_SUNRISE_MAX;
        this.SUNSET_MIN = EngineSetting.CLOCK_SUNSET_MIN;
        this.SUNSET_MAX = EngineSetting.CLOCK_SUNSET_MAX;
        this.MIDNIGHT = EngineSetting.CLOCK_MIDNIGHT;
        this.NOON = EngineSetting.CLOCK_NOON;
        this.QUARTER = EngineSetting.CLOCK_QUARTER;
        this.THREE_QUARTERS = EngineSetting.CLOCK_THREE_QUARTERS;

        // Tracking
        this.lastDay = -1;
    }

    // Assignment \\

    void assignData(ClockHandle clockHandle, float daysPerDay) {
        this.clockHandle = clockHandle;
        this.daysPerDay = daysPerDay;
    }

    void setDaysPerDay(float daysPerDay) {
        this.daysPerDay = daysPerDay;
    }

    // Current Tracker \\

    /*
     * Day progress is derived from the current system clock mod the game day
     * length — not from the epoch. This ensures time of day always reflects
     * real time divided by daysPerDay regardless of when the world was created.
     *
     * Total days elapsed uses the epoch so the in-game date accumulates
     * correctly from world creation and survives across sessions.
     */
    boolean advanceTime() {

        long now = System.currentTimeMillis();
        long millisPerGameDay = (long) (MILLIS_PER_REAL_DAY / daysPerDay);

        long totalDaysElapsed = (now - clockHandle.getWorldEpochStart()) / millisPerGameDay;
        double dayProgress = (double) (now % millisPerGameDay) / millisPerGameDay;
        double rawTimeOfDay = calculateRawTimeOfDay(dayProgress);
        int currentMinute = calculateMinute(rawTimeOfDay);
        int currentHour = calculateHour(rawTimeOfDay);
        double yearProgress = clockHandle.getYearProgress();
        double visualTimeOfDay = calculateVisualTimeOfDay(rawTimeOfDay, yearProgress);

        clockHandle.setTotalDaysElapsed(totalDaysElapsed);
        clockHandle.setDayProgress(dayProgress);
        clockHandle.setVisualTimeOfDay(visualTimeOfDay);
        clockHandle.setCurrentMinute(currentMinute);
        clockHandle.setCurrentHour(currentHour);

        boolean dayChanged = lastDay != totalDaysElapsed;
        lastDay = totalDaysElapsed;

        return dayChanged;
    }

    // Calculations \\

    double calculateRawTimeOfDay(double dayProgress) {

        double raw = (dayProgress + MIDDAY_OFFSET) % 1.0;

        if (raw < 0)
            raw += 1.0;

        return raw;
    }

    int calculateMinute(double rawTimeOfDay) {
        return (int) ((rawTimeOfDay * HOURS_PER_DAY * MINUTES_PER_HOUR) % MINUTES_PER_HOUR);
    }

    int calculateHour(double rawTimeOfDay) {
        return (int) (rawTimeOfDay * HOURS_PER_DAY);
    }

    /*
     * Bends raw time of day using seasonal sunrise/sunset offsets so that
     * summers have longer days and winters have longer nights.
     * 0.0 and 1.0 are always midnight. 0.5 is always visual noon.
     * The bend only affects the rate at which time moves between those anchors.
     */
    double calculateVisualTimeOfDay(double rawTimeOfDay, double yearProgress) {

        double seasonEffect = Math.sin(yearProgress * 2 * Math.PI);
        double shift = seasonEffect * MAX_SEASON_SHIFT;
        double actualSunrise = Math.max(SUNRISE_MIN, Math.min(SUNRISE_MAX, QUARTER - shift));
        double actualSunset = Math.max(SUNSET_MIN, Math.min(SUNSET_MAX, THREE_QUARTERS + shift));

        if (rawTimeOfDay < actualSunrise)
            return (rawTimeOfDay / actualSunrise) * QUARTER;

        if (rawTimeOfDay < NOON)
            return QUARTER + ((rawTimeOfDay - actualSunrise) / (NOON - actualSunrise)) * QUARTER;

        if (rawTimeOfDay < actualSunset)
            return NOON + ((rawTimeOfDay - NOON) / (actualSunset - NOON)) * QUARTER;

        return THREE_QUARTERS + ((rawTimeOfDay - actualSunset) / (1.0 - actualSunset)) * QUARTER;
    }
}