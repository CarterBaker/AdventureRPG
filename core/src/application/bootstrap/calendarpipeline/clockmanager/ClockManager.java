package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.calendarmanager.CalendarManager;
import application.bootstrap.calendarpipeline.clock.ClockData;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.ManagerPackage;
import engine.settings.EngineSetting;

public class ClockManager extends ManagerPackage {

    /*
     * Drives the in-game clock for the active world. Owns the ClockHandle and
     * all tracker branches. Validates calendar settings on awake, wires branches
     * to the active world's epoch, and advances time each update frame.
     * Supports world switching by rewiring branches to the new world's data.
     */

    // Internal
    private CalendarManager calendarManager;
    private WorldManager worldManager;

    // Branches
    private CurrentTrackerBranch currentTracker;
    private DayTrackerBranch dayTracker;
    private MonthTrackerBranch monthTracker;
    private YearTrackerBranch yearTracker;
    private InternalBufferBranch internalBuffer;

    // Clock
    private CalendarHandle calendarHandle;
    private ClockHandle clockHandle;

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.currentTracker = create(CurrentTrackerBranch.class);
        this.dayTracker = create(DayTrackerBranch.class);
        this.monthTracker = create(MonthTrackerBranch.class);
        this.yearTracker = create(YearTrackerBranch.class);
        this.internalBuffer = create(InternalBufferBranch.class);

        // Clock
        this.clockHandle = create(ClockHandle.class);
    }

    @Override
    protected void get() {

        // Internal
        this.calendarManager = get(CalendarManager.class);
        this.worldManager = get(WorldManager.class);
    }

    @Override
    protected void awake() {

        WorldHandle activeWorld = worldManager.getActiveWorld();
        this.calendarHandle = calendarManager.getCalendarHandleFromCalendarName(activeWorld.getCalendarName());

        if (activeWorld.getWorldEpochStart() == -1L)
            activeWorld.setWorldEpochStart(System.currentTimeMillis());

        ClockData clockData = new ClockData(activeWorld.getWorldEpochStart());
        clockHandle.constructor(clockData);

        validateSettings();
        wireData(activeWorld);
    }

    @Override
    protected void update() {
        advanceGameClock();
    }

    // Clock \\

    private void validateSettings() {

        int startingMonth = EngineSetting.STARTING_MONTH;
        int startingDayOfMonth = EngineSetting.STARTING_DAY_OF_MONTH;
        int monthCount = calendarHandle.getMonthCount();

        if (startingMonth < 0 || startingMonth >= monthCount)
            throwException("Invalid STARTING_MONTH: " + startingMonth +
                    ". Must be between 0 and " + (monthCount - 1));

        int daysInStartingMonth = calendarHandle.getMonthDays(startingMonth);

        if (startingDayOfMonth < 1 || startingDayOfMonth > daysInStartingMonth)
            throwException("Invalid STARTING_DAY_OF_MONTH: " + startingDayOfMonth +
                    ". Must be between 1 and " + daysInStartingMonth +
                    " for month " + calendarHandle.getMonthName(startingMonth));
    }

    private void wireData(WorldHandle activeWorld) {
        currentTracker.assignData(clockHandle, activeWorld.getDaysPerDay());
        dayTracker.assignData(calendarHandle, clockHandle);
        monthTracker.assignData(clockHandle);
        yearTracker.assignData(calendarHandle, clockHandle);
        internalBuffer.assignData(clockHandle);
    }

    private void advanceGameClock() {
        if (currentTracker.advanceTime())
            if (dayTracker.advanceTime())
                if (monthTracker.advanceTime())
                    yearTracker.advanceTime();
    }

    // World Switch \\

    /*
     * Call when the player travels to a different world.
     * Swaps calendar, time rate, and epoch anchor.
     * Time of day will immediately reflect the new world's game day cycle.
     */
    public void switchWorld(WorldHandle newWorld) {

        this.calendarHandle = calendarManager.getCalendarHandleFromCalendarName(newWorld.getCalendarName());

        if (newWorld.getWorldEpochStart() == -1L)
            newWorld.setWorldEpochStart(System.currentTimeMillis());

        clockHandle.setWorldEpochStart(newWorld.getWorldEpochStart());
        currentTracker.setDaysPerDay(newWorld.getDaysPerDay());

        dayTracker.assignData(calendarHandle, clockHandle);
        monthTracker.assignData(clockHandle);
        yearTracker.assignData(calendarHandle, clockHandle);
    }

    // Accessible \\

    public ClockHandle getClockHandle() {
        return clockHandle;
    }
}