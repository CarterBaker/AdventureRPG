package com.internal.bootstrap.calendarpipeline.clockmanager;

import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;

public class CurrentTrackerSystem extends SystemPackage {

    // Internal
    private int MINUTES_PER_HOUR;
    private int HOURS_PER_DAY;
    private float MIDDAY_OFFSET;

    // Per-world — set on awake and on world switch
    private float daysPerDay;
    private long gameEpochStart;

    private ClockHandle clockHandle;

    // Tracking
    private long lastDay;

    // Internal \\

    @Override
    protected void create() {
        this.MINUTES_PER_HOUR = EngineSetting.MINUTES_PER_HOUR;
        this.HOURS_PER_DAY = EngineSetting.HOURS_PER_DAY;
        this.MIDDAY_OFFSET = EngineSetting.MIDDAY_OFFSET;
        this.lastDay = -1;
    }

    // Assignment \\

    void assignTimeData(ClockHandle clockHandle, float daysPerDay, long gameEpochStart) {
        this.clockHandle = clockHandle;
        this.daysPerDay = daysPerDay;
        this.gameEpochStart = gameEpochStart;
    }

    /** Called by ClockManager.switchWorld() */
    void setDaysPerDay(float daysPerDay) {
        this.daysPerDay = daysPerDay;
    }

    void setGameEpochStart(long gameEpochStart) {
        this.gameEpochStart = gameEpochStart;
    }

    // Current Tracker \\

    boolean advanceTime() {

        long now = internal.getTime();
        long millisSinceEpoch = now - gameEpochStart;
        double realDaysElapsed = millisSinceEpoch / 86400000.0;
        double totalGameDays = realDaysElapsed * daysPerDay;

        long totalDaysElapsed = (long) totalGameDays;
        double dayProgress = totalGameDays - totalDaysElapsed;

        double rawTimeOfDay = calculateRawTimeOfDay(dayProgress);
        int currentMinute = calculateMinute(rawTimeOfDay);
        int currentHour = calculateHour(rawTimeOfDay);
        double yearProgress = clockHandle.getYearProgress();
        double visualTimeOfDay = calculateVisualTimeOfDay(rawTimeOfDay, yearProgress);

        clockHandle.setDayProgress(dayProgress);
        clockHandle.setVisualTimeOfDay(visualTimeOfDay);
        clockHandle.setCurrentMinute(currentMinute);
        clockHandle.setCurrentHour(currentHour);
        clockHandle.setTotalDaysElapsed(totalDaysElapsed);

        boolean dayChanged = (lastDay != totalDaysElapsed);
        lastDay = totalDaysElapsed;

        return dayChanged;
    }

    // Calculations \\

    double calculateDayProgress(int hour, int minute) {
        return ((double) hour / HOURS_PER_DAY)
                + ((double) minute / (HOURS_PER_DAY * MINUTES_PER_HOUR));
    }

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

    double calculateVisualTimeOfDay(double rawTimeOfDay, double yearProgress) {

        double seasonEffect = Math.sin(yearProgress * 2 * Math.PI);
        double maxShift = 0.08; // was 0.15 — halved, winters noticeably shorter but not brutal
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