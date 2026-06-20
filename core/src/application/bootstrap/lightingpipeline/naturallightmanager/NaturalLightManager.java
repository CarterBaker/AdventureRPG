package application.bootstrap.lightingpipeline.naturallightmanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.lightingpipeline.directionallight.DirectionalLightStruct;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;

public class NaturalLightManager extends ManagerPackage {

    /*
     * Drives the natural lighting pipeline each frame. Sun and moon are
     * independent directional lights, both always active — no blending.
     * Owns both light systems and pushes each light's state to its own UBO.
     */

    // Internal
    private ClockManager clockManager;
    private UBOManager uboManager;

    // Systems
    private SunLightSystem sunLightSystem;
    private MoonLightSystem moonLightSystem;

    // Light State
    private DirectionalLightStruct sunLight;
    private DirectionalLightStruct moonLight;

    // UBOs
    private UBOHandle sunLightUBO;
    private UBOHandle moonLightUBO;

    // Internal \\

    @Override
    protected void create() {

        // Systems
        this.sunLightSystem = create(SunLightSystem.class);
        this.moonLightSystem = create(MoonLightSystem.class);

        // Light State
        this.sunLight = new DirectionalLightStruct();
        this.moonLight = new DirectionalLightStruct();
    }

    @Override
    protected void get() {

        // Internal
        this.clockManager = get(ClockManager.class);
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        this.sunLightUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.SUN_LIGHT_UBO);
        this.moonLightUBO = uboManager.getUBOHandleFromUBOName(EngineSetting.MOON_LIGHT_UBO);
    }

    @Override
    protected void update() {

        float visualTimeOfDay = (float) clockManager.getClockHandle().getVisualTimeOfDay();

        sunLightSystem.update(visualTimeOfDay);
        moonLightSystem.update(visualTimeOfDay);

        sunLight.setDirection(
                sunLightSystem.getDirection().x,
                sunLightSystem.getDirection().y,
                sunLightSystem.getDirection().z);
        sunLight.setColor(
                sunLightSystem.getColor().x,
                sunLightSystem.getColor().y,
                sunLightSystem.getColor().z);
        sunLight.setIntensity(sunLightSystem.getIntensity());

        moonLight.setDirection(
                moonLightSystem.getDirection().x,
                moonLightSystem.getDirection().y,
                moonLightSystem.getDirection().z);
        moonLight.setColor(
                moonLightSystem.getColor().x,
                moonLightSystem.getColor().y,
                moonLightSystem.getColor().z);
        moonLight.setIntensity(moonLightSystem.getIntensity());

        pushLight(sunLight, sunLightUBO,
                EngineSetting.UNIFORM_SUN_DIRECTION,
                EngineSetting.UNIFORM_SUN_INTENSITY,
                EngineSetting.UNIFORM_SUN_COLOR);

        pushLight(moonLight, moonLightUBO,
                EngineSetting.UNIFORM_MOON_DIRECTION,
                EngineSetting.UNIFORM_MOON_INTENSITY,
                EngineSetting.UNIFORM_MOON_COLOR);
    }

    // Push \\

    private void pushLight(
            DirectionalLightStruct light,
            UBOHandle ubo,
            String directionName,
            String intensityName,
            String colorName) {

        ubo.updateUniform(directionName, light.getDirection());
        ubo.updateUniform(intensityName, light.getIntensity());
        ubo.updateUniform(colorName, light.getColor());
        uboManager.push(ubo);
    }

    // Accessible \\

    public SunLightSystem getSunLightSystem() {
        return sunLightSystem;
    }

    public MoonLightSystem getMoonLightSystem() {
        return moonLightSystem;
    }

    public DirectionalLightStruct getSunLight() {
        return sunLight;
    }

    public DirectionalLightStruct getMoonLight() {
        return moonLight;
    }
}