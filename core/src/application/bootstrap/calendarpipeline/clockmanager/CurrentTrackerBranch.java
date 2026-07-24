package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class CurrentTrackerBranch extends BranchPackage {

    /*
     * Advances the sub-day clock every frame from the real system clock,
     * applying the calendar's middayOffset so real-world noon lines up with
     * in-game noon. visualTimeOfDay additionally folds in a location phase
     * (see WorldWrapUtility.wrappedPlanetaryOffset, supplied by ClockManager)
     * before the seasonal sunrise/sunset bend, so different points along the
     * world's Y axis experience day and night at different moments, wrapping
     * seamlessly at the world edges.
     */

    // Internal
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

    // Calendar
    private CalendarHandle calendarHandle;

    // Per-world
    private float daysPerDay;
    private ClockHandle clockHandle;

    // Tracking
    private long lastDay;

    // Internal \\

    @Override
    protected void create() {

        // Internal
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

    void assignData(CalendarHandle calendarHandle, ClockHandle clockHandle, float daysPerDay) {
        this.calendarHandle = calendarHandle;
        this.clockHandle = clockHandle;
        this.daysPerDay = daysPerDay;
    }

    void setCalendarHandle(CalendarHandle calendarHandle) {
        this.calendarHandle = calendarHandle;
    }

    void setDaysPerDay(float daysPerDay) {
        this.daysPerDay = daysPerDay;
    }

    // Current Tracker \\

    boolean advanceTime(double locationOffset) {

        long now = internal.getTime();
        long millisPerGameDay = (long) (MILLIS_PER_REAL_DAY / daysPerDay);

        long totalDaysElapsed = (now - clockHandle.getWorldEpochStart()) / millisPerGameDay;
        double dayProgress = ((double) (now % MILLIS_PER_REAL_DAY) / MILLIS_PER_REAL_DAY * daysPerDay) % 1.0;

        double rawTimeOfDay = calculateRawTimeOfDay(dayProgress);
        int currentMinute = calculateMinute(rawTimeOfDay);
        int currentHour = calculateHour(rawTimeOfDay);
        double yearProgress = clockHandle.getYearProgress();

        double localRawTimeOfDay = wrapFraction(rawTimeOfDay + locationOffset);
        double visualTimeOfDay = calculateVisualTimeOfDay(localRawTimeOfDay, yearProgress);

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

        double raw = (dayProgress + calendarHandle.getMiddayOffset()) % 1.0;

        if (raw < 0)
            raw += 1.0;

        return raw;
    }

    int calculateMinute(double rawTimeOfDay) {

        int hoursPerDay = calendarHandle.getHoursPerDay();
        int minutesPerHour = calendarHandle.getMinutesPerHour();

        return (int) ((rawTimeOfDay * hoursPerDay * minutesPerHour) % minutesPerHour);
    }

    int calculateHour(double rawTimeOfDay) {
        return (int) (rawTimeOfDay * calendarHandle.getHoursPerDay());
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

    double wrapFraction(double value) {

        double wrapped = value % 1.0;

        if (wrapped < 0)
            wrapped += 1.0;

        return wrapped;
    }
}