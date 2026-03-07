package com.internal.bootstrap.calendarpipeline.clockmanager;

import java.time.Instant;

import com.internal.bootstrap.calendarpipeline.calendar.CalendarHandle;
import com.internal.bootstrap.calendarpipeline.calendarmanager.CalendarManager;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.bootstrap.worldpipeline.worldmanager.WorldManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;

public class ClockManager extends ManagerPackage {

    // Internal
    private CalendarManager calendarManager;
    private WorldManager worldManager;

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
        this.currentTracker = create(CurrentTrackerSystem.class);
        this.dayTracker = create(DayTrackerSystem.class);
        this.monthTracker = create(MonthTrackerSystem.class);
        this.yearTracker = create(YearTrackerSystem.class);
        this.internalBufferSystem = create(InternalBufferSystem.class);
        this.clockHandle = create(ClockHandle.class);
    }

    @Override
    protected void get() {
        this.calendarManager = get(CalendarManager.class);
        this.worldManager = get(WorldManager.class);
    }

    @Override
    protected void awake() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        // Resolve calendar from active world
        this.calendarHandle = calendarManager.getCalendar(activeWorld.getCalendarName());

        // Stamp epoch if this world has never been played
        if (activeWorld.getWorldEpochStart() == -1L) {
            activeWorld.setWorldEpochStart(Instant.now().toEpochMilli());
            // TODO: persist to save file here
        }

        // Wire systems
        currentTracker.assignTimeData(clockHandle, activeWorld.getDaysPerDay(),
                activeWorld.getWorldEpochStart());

        dayTracker.assignTimeData(calendarHandle, clockHandle);
        monthTracker.assignTimeData(calendarHandle, clockHandle);
        yearTracker.assignTimeData(calendarHandle, clockHandle);

        internalBufferSystem.assignTimeData(clockHandle);

        verifyClockHandle(activeWorld);
    }

    @Override
    protected void update() {
        advanceGameClock();
    }

    // Clock \\

    private void verifyClockHandle(WorldHandle activeWorld) {

        // Epoch comes from the world (either restored from save or just stamped above)
        long gameEpochStart = activeWorld.getWorldEpochStart();

        int STARTING_DAY_OF_MONTH = EngineSetting.STARTING_DAY_OF_MONTH;
        int STARTING_MONTH = EngineSetting.STARTING_MONTH;
        int STARTING_YEAR = EngineSetting.STARTING_YEAR;
        int STARTING_AGE = EngineSetting.STARTING_AGE;
        int STARTING_HOUR = EngineSetting.STARTING_HOUR;
        int STARTING_MINUTE = EngineSetting.STARTING_MINUTE;

        if (STARTING_MONTH < 0 || STARTING_MONTH >= calendarHandle.getMonthCount())
            throwException("Invalid STARTING_MONTH: " + STARTING_MONTH +
                    ". Must be between 0 and " + (calendarHandle.getMonthCount() - 1));

        int daysInStartingMonth = calendarHandle.getMonthDays(STARTING_MONTH);
        if (STARTING_DAY_OF_MONTH < 1 || STARTING_DAY_OF_MONTH > daysInStartingMonth)
            throwException("Invalid STARTING_DAY_OF_MONTH: " + STARTING_DAY_OF_MONTH +
                    ". Must be between 1 and " + daysInStartingMonth +
                    " for month " + calendarHandle.getMonthName(STARTING_MONTH));

        long totalDaysElapsed = 0;
        double dayProgress = currentTracker.calculateDayProgress(STARTING_HOUR, STARTING_MINUTE);
        double rawTimeOfDay = currentTracker.calculateRawTimeOfDay(dayProgress);
        long totalDaysWithOffset = dayTracker.calculateTotalDaysWithOffset(totalDaysElapsed);
        double yearProgress = dayTracker.calculateYearProgress(totalDaysWithOffset);
        double visualYearProgress = dayTracker.calculateVisualYearProgress(yearProgress);
        int dayOfWeek = dayTracker.calculateDayOfWeek(totalDaysWithOffset);
        float randomNoise = dayTracker.calculateRandomNoise(totalDaysWithOffset);
        double visualTimeOfDay = currentTracker.calculateVisualTimeOfDay(rawTimeOfDay, yearProgress);

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
        if (currentTracker.advanceTime())
            if (dayTracker.advanceTime())
                if (monthTracker.advanceTime())
                    yearTracker.advanceTime();
    }

    // World Switch \\

    /**
     * Call when the player travels to a different world.
     * Swaps calendar and time rate. Epoch is world-specific and already in
     * WorldHandle.
     */
    public void switchWorld(WorldHandle newWorld) {

        this.calendarHandle = calendarManager.getCalendar(newWorld.getCalendarName());

        if (newWorld.getWorldEpochStart() == -1L) {
            newWorld.setWorldEpochStart(Instant.now().toEpochMilli());
            // TODO: persist to save file here
        }

        currentTracker.setDaysPerDay(newWorld.getDaysPerDay());
        currentTracker.setGameEpochStart(newWorld.getWorldEpochStart());

        dayTracker.assignTimeData(calendarHandle, clockHandle);
        monthTracker.assignTimeData(calendarHandle, clockHandle);
        yearTracker.assignTimeData(calendarHandle, clockHandle);
    }

    // Accessible \\

    public ClockHandle getClockHandle() {
        return clockHandle;
    }
}