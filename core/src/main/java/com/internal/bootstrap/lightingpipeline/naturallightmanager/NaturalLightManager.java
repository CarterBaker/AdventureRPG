package com.internal.bootstrap.lightingpipeline.naturallightmanager;

import com.internal.bootstrap.calendarpipeline.clockmanager.ClockManager;
import com.internal.bootstrap.lightingpipeline.directionallight.DirectionalLightHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.ManagerPackage;

public class NaturalLightManager extends ManagerPackage {

    // Internal
    private ClockManager clockManager;
    private UBOManager uboManager;

    private SunLightSystem sunLightSystem;
    private MoonLightSystem moonLightSystem;

    private DirectionalLightHandle directionalLight;

    // Internal \\

    @Override
    protected void create() {
        this.sunLightSystem = create(SunLightSystem.class);
        this.moonLightSystem = create(MoonLightSystem.class);
        this.directionalLight = create(DirectionalLightHandle.class);
    }

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        UBOHandle naturalLightUBO = uboManager.getUBOHandleFromUBOName("DirectionalLightData");
        directionalLight.constructor(naturalLightUBO);
    }

    @Override
    protected void update() {

        float visualTimeOfDay = (float) clockManager.getClockHandle().getVisualTimeOfDay();

        sunLightSystem.update(visualTimeOfDay);
        moonLightSystem.update(visualTimeOfDay);

        float sunIntensity = sunLightSystem.getIntensity();
        float moonIntensity = moonLightSystem.getIntensity();

        float sunBlend = Math.min(sunIntensity / 0.15f, 1.0f);

        directionalLight.setDirection(
                lerp(moonLightSystem.getDirection().x, sunLightSystem.getDirection().x, sunBlend),
                lerp(moonLightSystem.getDirection().y, sunLightSystem.getDirection().y, sunBlend),
                lerp(moonLightSystem.getDirection().z, sunLightSystem.getDirection().z, sunBlend));

        directionalLight.setColor(
                lerp(moonLightSystem.getColor().x, sunLightSystem.getColor().x, sunBlend),
                lerp(moonLightSystem.getColor().y, sunLightSystem.getColor().y, sunBlend),
                lerp(moonLightSystem.getColor().z, sunLightSystem.getColor().z, sunBlend));

        directionalLight.setIntensity(lerp(moonIntensity, sunIntensity, sunBlend));

        directionalLight.push();
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // Accessible \\

    public SunLightSystem getSunLightSystem() {
        return sunLightSystem;
    }

    public MoonLightSystem getMoonLightSystem() {
        return moonLightSystem;
    }

    public DirectionalLightHandle getDirectionalLight() {
        return directionalLight;
    }
}