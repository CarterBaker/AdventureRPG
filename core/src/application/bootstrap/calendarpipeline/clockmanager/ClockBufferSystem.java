package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ClockBufferSystem extends SystemPackage {

    /*
     * Pushes clock state to each active grid's own TimeData UBOInstance
     * every frame. u_timeOfDay is location-dependent, read from that
     * grid's own LocationTimeStruct; everything else (calendar date, year
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

        timeData.updateUniform("u_timeOfDay", (float) grid.getLocationTimeStruct().getVisualTimeOfDay());
        timeData.updateUniform("u_timeOfYear", (float) clockHandle.getVisualYearProgress());
        timeData.updateUniform("u_rawTimeOfDay", (float) clockHandle.getDayProgress());
        timeData.updateUniform("u_time", elapsedTime);
        timeData.updateUniform("u_randomNoiseFromDay", clockHandle.getRandomNoiseFromDay());
        timeData.updateUniform("u_deltaTime", deltaTime);
        timeData.updateUniform("u_currentHour", clockHandle.getCurrentHour());
        timeData.updateUniform("u_currentMinute", clockHandle.getCurrentMinute());
        timeData.updateUniform("u_currentDay", clockHandle.getCurrentDayOfMonth());
        uboManager.push(timeData);
    }
}