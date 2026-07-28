// ClockManager.java
package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.calendarmanager.CalendarManager;
import application.bootstrap.calendarpipeline.clock.ClockData;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.calendarpipeline.clock.LocationTimeStruct;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ClockManager extends ManagerPackage {

    /*
     * Drives the in-game clock for the active world. Owns the ClockHandle
     * and all tracker branches, wired to the active world's calendar and
     * epoch. Starting point, day/year shape, and years-per-age all come
     * from the active calendar. The calendar tick itself (day/month/year/
     * season progression) is global and location-independent — there is
     * one shared timeline per world. Visual time of day is not: each
     * active grid tracks its own position along the world's Y axis, so
     * updateLocationTimes() resolves a LocationTimeStruct per grid every
     * frame, letting every player experience day and night independently
     * at the same real-world instant.
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
    private ClockBufferSystem internalBuffer;
    private SeasonBlendBranch seasonBlendBranch;

    // Clock
    private CalendarHandle calendarHandle;
    private ClockHandle clockHandle;
    private final LocationTimeStruct fallbackLocationTime = new LocationTimeStruct();

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.currentTracker = create(CurrentTrackerBranch.class);
        this.dayTracker = create(DayTrackerBranch.class);
        this.monthTracker = create(MonthTrackerBranch.class);
        this.yearTracker = create(YearTrackerBranch.class);
        this.internalBuffer = create(ClockBufferSystem.class);
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
    }

    private void advanceGameClock() {

        boolean dayChanged = currentTracker.advanceGlobalTime();

        if (dayChanged)
            if (dayTracker.advanceTime())
                if (monthTracker.advanceTime())
                    yearTracker.advanceTime();

        updateLocationTimes();
    }

    /*
     * Recomputes visual time of day for every active grid independently.
     * Each grid tracks a different player's position along the world's Y
     * axis, so each can sit at a different, correctly season-and-latitude
     * bent point of the day/night cycle at the same real-world instant.
     */
    private void updateLocationTimes() {

        WorldHandle locationWorld = resolveLocationWorld();

        if (locationWorld == null)
            return;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {

            GridInstance grid = (GridInstance) elements[i];
            long chunkCoordinate = grid.getActiveChunkCoordinate();

            double locationOffset = WorldWrapUtility.wrappedPlanetaryOffset(locationWorld, chunkCoordinate);
            double latitudeFactor = WorldWrapUtility.wrappedLatitudeFactor(locationWorld, chunkCoordinate);
            double visualTimeOfDay = currentTracker.computeVisualTimeOfDay(locationOffset, latitudeFactor);

            grid.getLocationTimeStruct().update(visualTimeOfDay, locationOffset, latitudeFactor);
        }
    }

    // Location \\

    private WorldHandle resolveLocationWorld() {

        if (!worldStreamManager.hasGrids())
            return null;

        return worldStreamManager.getActiveWorldHandle();
    }

    // World Switch \\

    /*
     * Call when the player travels to a different world. Swaps calendar,
     * time rate, axial tilt, and epoch anchor immediately.
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

    public CalendarHandle getCalendarHandle() {
        return calendarHandle;
    }

    /*
     * Shared reference location used only by the weather system's diurnal
     * wind/temperature curves, which remain a single simulation for the
     * whole world rather than per-window. Lighting, sky color, and the
     * time UBO are resolved per-window directly through each GridInstance
     * now — see GridInstance's own UBO instances.
     */
    public LocationTimeStruct getPrimaryLocationTime() {

        if (!worldStreamManager.hasGrids())
            return fallbackLocationTime;

        return worldStreamManager.getGrids().get(0).getLocationTimeStruct();
    }
}