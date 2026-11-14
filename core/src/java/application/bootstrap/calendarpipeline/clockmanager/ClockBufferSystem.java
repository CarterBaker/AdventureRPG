package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ClockBufferSystem extends SystemPackage {

    /*
     * Pushes clock state to each active grid's own TimeData UBOInstance
     * every frame. u_timeOfDay is location-dependent, read from that
     * grid's own ClockInstance; everything else (calendar date, year
     * progress, elapsed real seconds) is shared and identical across every
     * window.
     */

    // Internal
    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;
    private ClockHandle clockHandle;

    // Frame Tracking
    private float elapsedTime;

    // Internal \\

    @Override
    protected void create() {
        this.elapsedTime = 0;
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void update() {

        float deltaTime = internal.getDeltaTime();
        elapsedTime += deltaTime;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            pushData((GridInstance) elements[i], deltaTime);
    }

    // Assignment \\

    void assignData(ClockHandle clockHandle) {
        this.clockHandle = clockHandle;
    }

    // Buffer \\

    private void pushData(GridInstance grid, float deltaTime) {

        UBOInstance timeData = grid.getTimeDataUBO();

        timeData.updateUniform(EngineSetting.UNIFORM_TIME_OF_DAY, (float) grid.getClockInstance().getVisualTimeOfDay());
        timeData.updateUniform(EngineSetting.UNIFORM_TIME_OF_YEAR, (float) clockHandle.getVisualYearProgress());
        timeData.updateUniform(EngineSetting.UNIFORM_RAW_TIME_OF_DAY, (float) clockHandle.getDayProgress());
        timeData.updateUniform(EngineSetting.UNIFORM_TIME, elapsedTime);
        timeData.updateUniform(EngineSetting.UNIFORM_RANDOM_NOISE_FROM_DAY, clockHandle.getRandomNoiseFromDay());
        timeData.updateUniform(EngineSetting.UNIFORM_DELTA_TIME, deltaTime);
        timeData.updateUniform(EngineSetting.UNIFORM_CURRENT_HOUR, clockHandle.getCurrentHour());
        timeData.updateUniform(EngineSetting.UNIFORM_CURRENT_MINUTE, clockHandle.getCurrentMinute());
        timeData.updateUniform(EngineSetting.UNIFORM_CURRENT_DAY, clockHandle.getCurrentDayOfMonth());
        uboManager.push(timeData);
    }
}