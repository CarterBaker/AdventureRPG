package com.internal.bootstrap.calendarpipeline.clockmanager;

import com.internal.bootstrap.calendarpipeline.clock.ClockHandle;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;

class CurrentTrackerBranch extends BranchPackage {

    // Internal
    private int MINUTES_PER_HOUR;
    private int HOURS_PER_DAY;
    private float MIDDAY_OFFSET;

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
        long millisPerGameDay = (long) (86400000.0 / daysPerDay);

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

        boolean dayChanged = (lastDay != totalDaysElapsed);
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
        double maxShift = 0.08;
        double shift = seasonEffect * maxShift;
        double actualSunrise = Math.max(0.05, Math.min(0.40, 0.25 - shift));
        double actualSunset = Math.max(0.60, Math.min(0.95, 0.75 + shift));

        if (rawTimeOfDay < actualSunrise)
            return (rawTimeOfDay / actualSunrise) * 0.25;

        if (rawTimeOfDay < 0.5)
            return 0.25 + ((rawTimeOfDay - actualSunrise) / (0.5 - actualSunrise)) * 0.25;

        if (rawTimeOfDay < actualSunset)
            return 0.5 + ((rawTimeOfDay - 0.5) / (actualSunset - 0.5)) * 0.25;

        return 0.75 + ((rawTimeOfDay - actualSunset) / (1.0 - actualSunset)) * 0.25;
    }
}