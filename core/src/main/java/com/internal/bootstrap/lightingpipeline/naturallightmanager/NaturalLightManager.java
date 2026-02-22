package com.internal.bootstrap.lightingpipeline.naturallightmanager;

import com.internal.bootstrap.calendarpipeline.clockmanager.ClockManager;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.ManagerPackage;

public class NaturalLightManager extends ManagerPackage {

    // Internal
    private ClockManager clockManager;
    private UBOManager uboManager;

    private SunLightSystem sunLightSystem;
    private MoonLightSystem moonLightSystem;

    private UBOHandle naturalLightUBO;

    // Internal \\

    @Override
    protected void create() {
        this.sunLightSystem = create(SunLightSystem.class);
        this.moonLightSystem = create(MoonLightSystem.class);
    }

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        this.naturalLightUBO = uboManager.getUBOHandleFromUBOName("NaturalLightData");
        sunLightSystem.constructor(naturalLightUBO);
        moonLightSystem.constructor(naturalLightUBO);
    }

    @Override
    protected void update() {
        float visualTimeOfDay = (float) clockManager.getClockHandle().getVisualTimeOfDay();
        sunLightSystem.update(visualTimeOfDay);
        moonLightSystem.update(visualTimeOfDay);
        naturalLightUBO.push();
    }

    // Accessible \\

    public SunLightSystem getSunLightSystem() {
        return sunLightSystem;
    }

    public MoonLightSystem getMoonLightSystem() {
        return moonLightSystem;
    }

    public UBOHandle getNaturalLightUBO() {
        return naturalLightUBO;
    }
}