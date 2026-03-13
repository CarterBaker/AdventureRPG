package com.internal.bootstrap.calendarpipeline.clockmanager;

import com.internal.bootstrap.calendarpipeline.clock.ClockHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.BranchPackage;

class InternalBufferBranch extends BranchPackage {

    // Internal
    private UBOManager uboManager;
    private ClockHandle clockHandle;

    // UBO
    private UBOHandle timeData;

    // Frame Tracking
    private float elapsedTime;

    // Internal \\

    @Override
    protected void create() {

        // Frame Tracking
        this.elapsedTime = 0;
    }

    @Override
    protected void get() {

        // Internal
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {

        // UBO
        this.timeData = uboManager.getUBOHandleFromUBOName("TimeData");
    }

    @Override
    protected void update() {

        float deltaTime = internal.getDeltaTime();
        this.elapsedTime += deltaTime;

        pushData(deltaTime);
    }

    // Assignment \\

    void assignData(ClockHandle clockHandle) {
        this.clockHandle = clockHandle;
    }

    // Buffer \\

    private void pushData(float deltaTime) {
        timeData.updateUniform("u_timeOfDay", (float) clockHandle.getVisualTimeOfDay());
        timeData.updateUniform("u_timeOfYear", (float) clockHandle.getVisualYearProgress());
        timeData.updateUniform("u_rawTimeOfDay", (float) clockHandle.getDayProgress());
        timeData.updateUniform("u_time", elapsedTime);
        timeData.updateUniform("u_randomNoiseFromDay", clockHandle.getRandomNoiseFromDay());
        timeData.updateUniform("u_deltaTime", deltaTime);
        timeData.updateUniform("u_currentHour", clockHandle.getCurrentHour());
        timeData.updateUniform("u_currentMinute", clockHandle.getCurrentMinute());
        timeData.updateUniform("u_currentDay", clockHandle.getCurrentDayOfMonth());
        timeData.push();
    }
}