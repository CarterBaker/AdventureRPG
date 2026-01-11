package com.AdventureRPG.bootstrap.calendarpipeline.clockmanager;

import java.time.Instant;

import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;

public class CurrentTrackerSystem extends SystemPackage {

    // Internal
    private int MINUTES_PER_HOUR;
    private int HOURS_PER_DAY;
    private int DAYS_PER_DAY;
    private float MIDDAY_OFFSET;

    private ClockHandle clockHandle;

    private long gameEpochStart;

    // Tracking
    private long lastDay;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.MINUTES_PER_HOUR = EngineSetting.MINUTES_PER_HOUR;
        this.HOURS_PER_DAY = EngineSetting.HOURS_PER_DAY;
        this.DAYS_PER_DAY = EngineSetting.DAYS_PER_DAY;
        this.MIDDAY_OFFSET = EngineSetting.MIDDAY_OFFSET;

        // Time Tracking
        this.lastDay = -1;
    }

    // Current Tracker \\

    void assignTimeData(ClockHandle clockHandle) {
        this.clockHandle = clockHandle;
        this.gameEpochStart = clockHandle.getGameEpochStart();
    }

    boolean advanceTime() {

        // 1. Get current system time
        long now = Instant.now().toEpochMilli();
        long millisSinceEpoch = now - gameEpochStart;

        // 2. Real-world days elapsed since epoch
        double realDaysElapsed = millisSinceEpoch / 86400000.0;

        // 3. Scale into game time
        double totalGameDays = realDaysElapsed * DAYS_PER_DAY;

        // 4. Split into whole days and fractional progress
        long totalDaysElapsed = (long) totalGameDays;
        double dayProgress = totalGameDays - totalDaysElapsed;

        // 5. Calculate time values
        double rawTimeOfDay = calculateRawTimeOfDay(dayProgress);
        int currentMinute = calculateMinute(rawTimeOfDay);
        int currentHour = calculateHour(rawTimeOfDay);
        double yearProgress = clockHandle.getYearProgress();
        double visualTimeOfDay = calculateVisualTimeOfDay(rawTimeOfDay, yearProgress);

        // 6. Update ClockHandle
        clockHandle.setDayProgress(dayProgress);
        clockHandle.setVisualTimeOfDay(visualTimeOfDay);
        clockHandle.setCurrentMinute(currentMinute);
        clockHandle.setCurrentHour(currentHour);
        clockHandle.setTotalDaysElapsed(totalDaysElapsed);

        // 7. Check for changes and cascade
        boolean dayChanged = (lastDay != totalDaysElapsed);
        lastDay = totalDaysElapsed;

        // Return true if day rolled over (triggers DayTracker)
        return dayChanged;
    }

    // Calculations \\

    double calculateDayProgress(int hour, int minute) {
        return ((double) hour / HOURS_PER_DAY) + ((double) minute / (HOURS_PER_DAY * MINUTES_PER_HOUR));
    }

    double calculateRawTimeOfDay(double dayProgress) {
        double rawTimeOfDay = (dayProgress + MIDDAY_OFFSET) % 1.0;
        if (rawTimeOfDay < 0)
            rawTimeOfDay += 1.0;
        return rawTimeOfDay;
    }

    int calculateMinute(double rawTimeOfDay) {
        return (int) ((rawTimeOfDay * HOURS_PER_DAY * MINUTES_PER_HOUR) % MINUTES_PER_HOUR);
    }

    int calculateHour(double rawTimeOfDay) {
        return (int) (rawTimeOfDay * HOURS_PER_DAY);
    }

    double calculateVisualTimeOfDay(double rawTimeOfDay, double yearProgress) {

        // Seasonal effect: summer = +1, winter = -1
        double seasonEffect = Math.sin(yearProgress * 2 * Math.PI);

        // Seasonal shift: how much to shift sunrise/sunset times
        double maxShift = 0.15; // 15% shift max
        double shift = seasonEffect * maxShift;

        // Actual sunrise/sunset times in raw time
        double actualSunrise = 0.25 - shift;
        double actualSunset = 0.75 + shift;

        // Clamp to reasonable bounds
        actualSunrise = Math.max(0.05, Math.min(0.40, actualSunrise));
        actualSunset = Math.max(0.60, Math.min(0.95, actualSunset));

        double bent;

        if (rawTimeOfDay < actualSunrise) {
            // Pre-sunrise night: [0, actualSunrise] → [0, 0.25]
            bent = (rawTimeOfDay / actualSunrise) * 0.25;

        } else if (rawTimeOfDay < 0.5) {
            // Morning: [actualSunrise, 0.5] → [0.25, 0.5]
            bent = 0.25 + ((rawTimeOfDay - actualSunrise) / (0.5 - actualSunrise)) * 0.25;

        } else if (rawTimeOfDay < actualSunset) {
            // Afternoon: [0.5, actualSunset] → [0.5, 0.75]
            bent = 0.5 + ((rawTimeOfDay - 0.5) / (actualSunset - 0.5)) * 0.25;

        } else {
            // Post-sunset night: [actualSunset, 1.0] → [0.75, 1.0]
            bent = 0.75 + ((rawTimeOfDay - actualSunset) / (1.0 - actualSunset)) * 0.25;
        }

        return bent;
    }
}