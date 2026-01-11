package com.AdventureRPG.bootstrap.calendarpipeline.clockmanager;

import java.time.Instant;

import com.AdventureRPG.bootstrap.calendarpipeline.calendarmanager.CalendarHandle;
import com.AdventureRPG.bootstrap.calendarpipeline.calendarmanager.CalendarManager;
import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;

public class ClockManager extends ManagerPackage {

    // Internal
    private CalendarManager calendarManager;

    private CurrentTrackerSystem currentTracker;
    private DayTrackerSystem dayTracker;
    private MonthTrackerSystem monthTracker;
    private YearTrackerSystem yearTracker;

    private InternalBufferSystem internalBufferSystem;

    private CalendarHandle calendarHandle;
    private ClockHandle clockHandle;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.currentTracker = create(CurrentTrackerSystem.class);
        this.dayTracker = create(DayTrackerSystem.class);
        this.monthTracker = create(MonthTrackerSystem.class);
        this.yearTracker = create(YearTrackerSystem.class);

        this.internalBufferSystem = create(InternalBufferSystem.class);

        this.clockHandle = create(ClockHandle.class);
    }

    @Override
    protected void get() {

        // Internal
        this.calendarManager = get(CalendarManager.class);
    }

    @Override
    protected void awake() {

        // Internal
        this.calendarHandle = calendarManager.getCalendarHandle();

        currentTracker.assignTimeData(clockHandle);
        dayTracker.assignTimeData(calendarHandle, clockHandle);
        monthTracker.assignTimeData(calendarHandle, clockHandle);
        yearTracker.assignTimeData(calendarHandle, clockHandle);

        internalBufferSystem.assignTimeData(clockHandle);

        verifyClockHandle();
    }

    @Override
    protected void update() {
        advanceGameClock();
    }

    // Clock \\

    private void verifyClockHandle() {

        long gameEpochStart = Instant.now().toEpochMilli();

        // Get starting values from EngineSetting
        int STARTING_DAY_OF_MONTH = EngineSetting.STARTING_DAY_OF_MONTH;
        int STARTING_MONTH = EngineSetting.STARTING_MONTH;
        int STARTING_YEAR = EngineSetting.STARTING_YEAR;
        int STARTING_AGE = EngineSetting.STARTING_AGE;
        int STARTING_HOUR = EngineSetting.STARTING_HOUR;
        int STARTING_MINUTE = EngineSetting.STARTING_MINUTE;

        // Verify month is valid
        if (STARTING_MONTH < 0 || STARTING_MONTH >= calendarHandle.getMonthCount())
            throwException("Invalid STARTING_MONTH: " + STARTING_MONTH +
                    ". Must be between 0 and " + (calendarHandle.getMonthCount() - 1));

        // Verify day of month is valid for the starting month
        int daysInStartingMonth = calendarHandle.getMonthDays(STARTING_MONTH);
        if (STARTING_DAY_OF_MONTH < 1 || STARTING_DAY_OF_MONTH > daysInStartingMonth)
            throwException("Invalid STARTING_DAY_OF_MONTH: " + STARTING_DAY_OF_MONTH +
                    ". Must be between 1 and " + daysInStartingMonth +
                    " for month " + calendarHandle.getMonthName(STARTING_MONTH));

        // Calculate initial values using pure calculation methods
        long totalDaysElapsed = 0;

        // CurrentTracker calculations
        double dayProgress = currentTracker.calculateDayProgress(STARTING_HOUR, STARTING_MINUTE);
        double rawTimeOfDay = currentTracker.calculateRawTimeOfDay(dayProgress);

        // DayTracker calculations
        long totalDaysWithOffset = dayTracker.calculateTotalDaysWithOffset(totalDaysElapsed);
        double yearProgress = dayTracker.calculateYearProgress(totalDaysWithOffset);
        double visualYearProgress = dayTracker.calculateVisualYearProgress(yearProgress);
        int dayOfWeek = dayTracker.calculateDayOfWeek(totalDaysWithOffset);
        float randomNoise = dayTracker.calculateRandomNoise(totalDaysWithOffset);

        // Calculate visual time of day with year progress
        double visualTimeOfDay = currentTracker.calculateVisualTimeOfDay(rawTimeOfDay, yearProgress);

        // Construct ClockHandle
        clockHandle.constructor(
                gameEpochStart,
                totalDaysElapsed,
                totalDaysWithOffset,
                dayProgress,
                visualTimeOfDay,
                yearProgress,
                visualYearProgress,
                STARTING_MINUTE,
                STARTING_HOUR,
                dayOfWeek,
                STARTING_DAY_OF_MONTH,
                STARTING_MONTH,
                STARTING_YEAR,
                STARTING_AGE,
                randomNoise);
    }

    private void advanceGameClock() {

        // Cascade through time systems
        if (currentTracker.advanceTime())

            // Day rolled over
            if (dayTracker.advanceTime())

                // Month rolled over
                if (monthTracker.advanceTime())

                    // Year rolled over
                    yearTracker.advanceTime();

    }

    // Utility \\

    ClockHandle getClockHandle() {
        return clockHandle;
    }
}