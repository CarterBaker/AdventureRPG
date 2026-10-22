// ClockManager.java
package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.calendarmanager.CalendarManager;
import application.bootstrap.calendarpipeline.clock.ClockData;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.calendarpipeline.clock.ClockInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ClockManager extends ManagerPackage {

    /*
     * Drives the in-game clock for the active world. Owns the global
     * ClockHandle and calendar wiring; each grid holds its own
     * ClockInstance, handed out by this manager, so time of day resolves
     * independently per location. A world is fresh every session, so its
     * clock starts the moment it is wired, on its calendar's own start date
     * and time, giving every session the same starting point. The moon's
     * place in its orbit advances with the clock. Loading and switching
     * worlds share one wiring path, and the clock is solved immediately
     * afterward so every value is already correct before the first frame
     * reads it. Any place on the world can ask its visual time of day, so a
     * map can shade the whole world by the same shared clock every grid
     * follows. Moving the clock to another day and minute is one path too:
     * every tracker forgets what it last saw and the clock is solved again at
     * once, so a jump of any size lands on the right date in one step. A
     * clock can also be held at any moment, to the millisecond, by the time
     * elapsed since its epoch — the one number every date and time falls out
     * of — so a recording replays it smoothly.
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
    private LunarTrackerBranch lunarTracker;
    private ClockBufferSystem internalBuffer;

    // Clock
    private CalendarHandle calendarHandle;
    private ClockHandle clockHandle;
    private ClockInstance fallbackLocationTime;

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.currentTracker = create(CurrentTrackerBranch.class);
        this.dayTracker = create(DayTrackerBranch.class);
        this.monthTracker = create(MonthTrackerBranch.class);
        this.yearTracker = create(YearTrackerBranch.class);
        this.lunarTracker = create(LunarTrackerBranch.class);
        this.internalBuffer = create(ClockBufferSystem.class);

        // Clock
        this.clockHandle = create(ClockHandle.class);
        this.fallbackLocationTime = createClockInstance();
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

        clockHandle.constructor(new ClockData(internal.getTime()));
        internalBuffer.assignData(clockHandle);

        wireData(worldManager.getActiveWorld());
    }

    @Override
    protected void update() {
        advanceGameClock();
    }

    // Clock \\

    private void wireData(WorldHandle world) {

        this.calendarHandle = calendarManager.getCalendarHandleFromCalendarName(world.getCalendarName());

        clockHandle.setWorldEpochStart(internal.getTime());
        clockHandle.setCalendarHandle(calendarHandle);

        currentTracker.assignData(calendarHandle, clockHandle, world.getAxialTilt());
        dayTracker.assignData(calendarHandle, clockHandle, world.getSeed());
        monthTracker.assignData(clockHandle);
        yearTracker.assignData(calendarHandle, clockHandle);
        lunarTracker.assignData(calendarHandle, clockHandle);

        advanceGameClock();
    }

    private void advanceGameClock() {

        boolean dayChanged = currentTracker.advanceGlobalTime();

        if (dayChanged)
            if (dayTracker.advanceTime())
                if (monthTracker.advanceTime())
                    yearTracker.advanceTime();

        currentTracker.advanceVisualYear();
        dayTracker.advanceDayBlend();
        lunarTracker.advanceOrbit();

        updateLocationTimes();
    }

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

            grid.getClockInstance().update(
                    computeVisualTimeOfDay(locationWorld, chunkCoordinate),
                    WorldWrapUtility.wrappedPlanetaryOffset(locationWorld, chunkCoordinate),
                    WorldWrapUtility.wrappedLatitudeFactor(locationWorld, chunkCoordinate));
        }
    }

    // Location Time \\

    public double computeVisualTimeOfDay(WorldHandle world, long chunkCoordinate) {
        return currentTracker.computeVisualTimeOfDay(
                WorldWrapUtility.wrappedPlanetaryOffset(world, chunkCoordinate),
                WorldWrapUtility.wrappedLatitudeFactor(world, chunkCoordinate));
    }

    // Location \\

    private WorldHandle resolveLocationWorld() {

        if (!worldStreamManager.hasGrids())
            return null;

        return worldStreamManager.getActiveWorldHandle();
    }

    // Move \\

    // totalDaysWithOffset counts from the first day of year 0 of the calendar's starting age
    public void moveClock(long totalDaysWithOffset, int minuteOfDay) {

        if (totalDaysWithOffset < 0)
            throwException("Cannot move the clock to day " + totalDaysWithOffset
                    + ", before the first day of the calendar's starting age.");

        currentTracker.moveTo(totalDaysWithOffset - dayTracker.getStartDayOffset(), minuteOfDay);

        resetTracking();
        advanceGameClock();
    }

    // Reads the milliseconds given since the epoch right now; landing on an earlier day, or more than a day on,
    // every tracker forgets what it last saw as a move does
    public void holdClock(long elapsedMillis) {

        long previousDay = clockHandle.getTotalDaysElapsed();
        long day = currentTracker.resolveTotalDays(elapsedMillis);

        clockHandle.setWorldEpochStart(internal.getTime() - elapsedMillis);

        if (day < previousDay || day > previousDay + 1)
            resetTracking();

        advanceGameClock();
    }

    private void resetTracking() {
        currentTracker.resetTracking();
        dayTracker.resetTracking();
        monthTracker.resetTracking();
        yearTracker.resetTracking();
    }

    // World Switch \\

    public void switchWorld(WorldHandle newWorld) {
        wireData(newWorld);
    }

    // Location Clocks \\

    public ClockInstance createClockInstance() {
        return create(ClockInstance.class);
    }

    // Accessible \\

    public ClockHandle getClockHandle() {
        return clockHandle;
    }

    public long getElapsedMillis() {
        return internal.getTime() - clockHandle.getWorldEpochStart();
    }

    public CalendarHandle getCalendarHandle() {
        return calendarHandle;
    }

    public float getDailyRandom(long stream) {
        return dayTracker.resolveDailyRandom(stream);
    }

    public float getDayLengthForYearProgress(double yearProgress) {
        return calendarHandle.getDayLengthForYearProgress(yearProgress);
    }

    public ClockInstance getPrimaryLocationTime() {

        if (!worldStreamManager.hasGrids())
            return fallbackLocationTime;

        return worldStreamManager.getGrids().get(0).getClockInstance();
    }
}
