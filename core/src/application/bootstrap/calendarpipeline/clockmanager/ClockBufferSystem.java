package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

class ClockBufferSystem extends SystemPackage {

    /*
     * Pushes clock state to the GPU time UBO each frame. Accumulates elapsed
     * real time for the shader u_time uniform. Wired to the active
     * ClockHandle via assignData() after awake. u_timeOfDay is read from
     * ClockManager's primary grid location — see
     * ClockManager.getPrimaryLocationTime() — pending per-window time UBOs.
     */

    // Internal
    private UBOManager uboManager;
    private ClockHandle clockHandle;
    private ClockManager clockManager;

    // UBO
    private UBOHandle timeData;

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
    }

    @Override
    protected void awake() {
        this.timeData = uboManager.getUBOHandleFromUBOName(EngineSetting.UBO_TIME_DATA_NAME);
    }

    @Override
    protected void update() {
        float deltaTime = internal.getDeltaTime();
        elapsedTime += deltaTime;
        pushData(deltaTime);
    }

    // Assignment \\

    void assignData(ClockHandle clockHandle, ClockManager clockManager) {
        this.clockHandle = clockHandle;
        this.clockManager = clockManager;
    }

    // Buffer \\

    private void pushData(float deltaTime) {
        timeData.updateUniform("u_timeOfDay", (float) clockManager.getPrimaryLocationTime().getVisualTimeOfDay());
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