package com.AdventureRPG.bootstrap.calendarpipeline.clockmanager;

import com.AdventureRPG.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.AdventureRPG.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.AdventureRPG.core.engine.SystemPackage;

public class InternalBufferSystem extends SystemPackage {

    // Internal
    private UBOManager uboManager;
    private ClockHandle clockHandle;

    // UBO
    private UBOHandle timeData;

    // Frame tracking
    private float elapsedTime;

    // Internal \\

    @Override
    protected void create() {

        // Initialize frame tracking
        this.elapsedTime = 0;
    }

    @Override
    protected void get() {

        // Get UBOManager dependency
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {

        // Get the TimeData UBO handle
        this.timeData = uboManager.getUBOHandleFromUBOName("TimeData");
    }

    @Override
    protected void update() {

        // Update frame timing
        float deltaTime = internal.getDeltaTime();
        this.elapsedTime += deltaTime;

        // Push all data to UBO
        pushData(deltaTime);
    }

    // Buffer System \\

    void assignTimeData(ClockHandle clockHandle) {
        this.clockHandle = clockHandle;
    }

    private void pushData(float deltaTime) {

        // Update all uniforms from ClockHandle
        this.timeData.updateUniform("u_timeOfDay", (float) clockHandle.getVisualTimeOfDay());
        this.timeData.updateUniform("u_timeOfYear", (float) clockHandle.getVisualYearProgress());
        this.timeData.updateUniform("u_rawTimeOfDay", (float) clockHandle.getDayProgress());
        this.timeData.updateUniform("u_time", this.elapsedTime);
        this.timeData.updateUniform("u_randomNoiseFromDay", clockHandle.getRandomNoiseFromDay());
        this.timeData.updateUniform("u_deltaTime", deltaTime);
        this.timeData.updateUniform("u_currentHour", clockHandle.getCurrentHour());
        this.timeData.updateUniform("u_currentMinute", clockHandle.getCurrentMinute());
        this.timeData.updateUniform("u_currentDay", clockHandle.getCurrentDayOfMonth());

        // Push to GPU
        this.timeData.push();
    }
}