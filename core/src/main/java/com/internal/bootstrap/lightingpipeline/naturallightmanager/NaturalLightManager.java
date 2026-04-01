package com.internal.bootstrap.lightingpipeline.naturallightmanager;

import com.internal.bootstrap.calendarpipeline.clockmanager.ClockManager;
import com.internal.bootstrap.lightingpipeline.directionallight.DirectionalLightHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.settings.EngineSetting;

public class NaturalLightManager extends ManagerPackage {

    /*
     * Drives the natural lighting pipeline each frame. Blends sun and moon
     * contributions based on sun intensity and pushes the result to the
     * directional light UBO. Owns the sun and moon systems and the directional
     * light handle.
     */

    // Internal
    private ClockManager clockManager;
    private UBOManager uboManager;

    // Systems
    private SunLightSystem sunLightSystem;
    private MoonLightSystem moonLightSystem;

    // Light
    private DirectionalLightHandle directionalLight;

    // Settings
    private float SUN_BLEND_THRESHOLD;

    // Internal \\

    @Override
    protected void create() {

        // Systems
        this.sunLightSystem = create(SunLightSystem.class);
        this.moonLightSystem = create(MoonLightSystem.class);

        // Light
        this.directionalLight = create(DirectionalLightHandle.class);

        // Settings
        this.SUN_BLEND_THRESHOLD = EngineSetting.SUN_BLEND_THRESHOLD;
    }

    @Override
    protected void get() {

        // Internal
        this.clockManager = get(ClockManager.class);
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {

        UBOHandle naturalLightUBO = uboManager.getUBOHandleFromUBOName(
                EngineSetting.DIRECTIONAL_LIGHT_UBO);
        directionalLight.constructor(naturalLightUBO);
    }

    @Override
    protected void update() {

        float visualTimeOfDay = (float) clockManager.getClockHandle().getVisualTimeOfDay();

        sunLightSystem.update(visualTimeOfDay);
        moonLightSystem.update(visualTimeOfDay);

        float sunIntensity = sunLightSystem.getIntensity();
        float moonIntensity = moonLightSystem.getIntensity();
        float sunBlend = Math.min(sunIntensity / SUN_BLEND_THRESHOLD, 1.0f);

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