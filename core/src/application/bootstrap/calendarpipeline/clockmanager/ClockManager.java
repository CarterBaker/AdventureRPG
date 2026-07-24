// ClockManager.java
package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.calendarmanager.CalendarManager;
import application.bootstrap.calendarpipeline.clock.ClockData;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.ManagerPackage;

public class ClockManager extends ManagerPackage {

    /*
     * Drives the in-game clock for the active world. Owns the ClockHandle and
     * all tracker branches, and wires them to the active world's calendar and
     * epoch. Every fixed time constant — starting point, day/year shape,
     * years-per-age, daysPerDay — comes from the active calendar. Each frame
     * also resolves the current render viewpoint's position along the
     * world's Y axis into a location phase offset and a latitude factor (see
     * WorldWrapUtility's wrappedPlanetaryOffset and wrappedLatitudeFactor),
     * and SeasonBlendBranch resolves the calendar's own named seasons into a
     * current daylight fraction, so visualTimeOfDay reflects both where and
     * when on the world a viewer actually is.
     */

    // Internal
    private CalendarManager calendarManager;
    private WorldManager worldManager;
    private WorldStreamManager worldStreamManager;

    // Branches
    private CurrentTrackerBranch currentTracker;
    private DayTrackerBranch dayTracker;
    private MonthTrackerBranch monthTracker;
    private YearTrackerBranch yearTracker;
    private InternalBufferBranch internalBuffer;
    private SkyColorBranch skyColorBranch;
    private SeasonBlendBranch seasonBlendBranch;

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
        this.skyColorBranch = create(SkyColorBranch.class);
        this.seasonBlendBranch = create(SeasonBlendBranch.class);

        // Clock
        this.clockHandle = create(ClockHandle.class);
    }

    @Override
    protected void get() {

        // Internal
        this.calendarManager = get(CalendarManager.class);
        this.worldManager = get(WorldManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void awake() {

        WorldHandle activeWorld = worldManager.getActiveWorld();
        this.calendarHandle = calendarManager.getCalendarHandleFromCalendarName(activeWorld.getCalendarName());

        if (activeWorld.getWorldEpochStart() == -1L)
            activeWorld.setWorldEpochStart(System.currentTimeMillis());

        ClockData clockData = new ClockData(activeWorld.getWorldEpochStart());
        clockHandle.constructor(clockData);
        clockHandle.setCalendarHandle(calendarHandle);

        wireData(activeWorld);
    }

    @Override
    protected void update() {
        advanceGameClock();
    }

    // Clock \\

    private void wireData(WorldHandle activeWorld) {
        seasonBlendBranch.assignData(calendarHandle);
        currentTracker.assignData(
                calendarHandle,
                clockHandle,
                activeWorld.getAxialTilt(),
                seasonBlendBranch);
        dayTracker.assignData(calendarHandle, clockHandle);
        monthTracker.assignData(clockHandle);
        yearTracker.assignData(calendarHandle, clockHandle);
        internalBuffer.assignData(clockHandle);
        skyColorBranch.assignData(clockHandle);
    }

    private void advanceGameClock() {

        WorldHandle locationWorld = resolveLocationWorld();

        double locationOffset = 0.0;
        double latitudeFactor = 0.0;

        if (locationWorld != null) {
            long chunkCoordinate = worldStreamManager.getGrids().get(0).getActiveChunkCoordinate();
            locationOffset = WorldWrapUtility.wrappedPlanetaryOffset(locationWorld, chunkCoordinate);
            latitudeFactor = WorldWrapUtility.wrappedLatitudeFactor(locationWorld, chunkCoordinate);
        }

        if (currentTracker.advanceTime(locationOffset, latitudeFactor))
            if (dayTracker.advanceTime())
                if (monthTracker.advanceTime())
                    yearTracker.advanceTime();
    }

    // Location \\

    private WorldHandle resolveLocationWorld() {

        if (!worldStreamManager.hasGrids())
            return null;

        return worldStreamManager.getActiveWorldHandle();
    }

    // World Switch \\

    /*
     * Call when the player travels to a different world.
     * Swaps calendar, time rate, axial tilt, and epoch anchor.
     * Time of day will immediately reflect the new world's game day cycle.
     */
    public void switchWorld(WorldHandle newWorld) {

        this.calendarHandle = calendarManager.getCalendarHandleFromCalendarName(newWorld.getCalendarName());

        if (newWorld.getWorldEpochStart() == -1L)
            newWorld.setWorldEpochStart(System.currentTimeMillis());

        clockHandle.setWorldEpochStart(newWorld.getWorldEpochStart());
        clockHandle.setCalendarHandle(calendarHandle);
        currentTracker.setCalendarHandle(calendarHandle);
        currentTracker.setAxialTilt(newWorld.getAxialTilt());

        seasonBlendBranch.assignData(calendarHandle);
        dayTracker.assignData(calendarHandle, clockHandle);
        monthTracker.assignData(clockHandle);
        yearTracker.assignData(calendarHandle, clockHandle);
    }

    // Accessible \\

    public ClockHandle getClockHandle() {
        return clockHandle;
    }
}