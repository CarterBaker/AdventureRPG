package application.bootstrap.lightingpipeline.naturallightmanager;

import application.bootstrap.lightingpipeline.directionallight.DirectionalLightStruct;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class NaturalLightManager extends ManagerPackage {

    /*
     * Drives natural lighting once per active grid each frame — sun and
     * moon are independent, always-active directional lights computed from
     * that grid's own LocationTimeStruct, so each window sees the sun/moon
     * position for wherever its own player currently sits on the world's Y
     * axis. Each grid owns its own Sun/Moon UBOInstance (built in
     * GridBuildSystem); LightingSystem binds them onto its own pass.
     */

    // Internal
    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;

    // Systems
    private SunLightSystem sunLightSystem;
    private MoonLightSystem moonLightSystem;

    // Scratch
    private final DirectionalLightStruct sunLight = new DirectionalLightStruct();
    private final DirectionalLightStruct moonLight = new DirectionalLightStruct();

    // Internal \\

    @Override
    protected void create() {
        this.sunLightSystem = create(SunLightSystem.class);
        this.moonLightSystem = create(MoonLightSystem.class);
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void update() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            updateGridLighting((GridInstance) elements[i]);
    }

    // Per-Grid Lighting \\

    private void updateGridLighting(GridInstance grid) {

        float visualTimeOfDay = (float) grid.getLocationTimeStruct().getVisualTimeOfDay();

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

        pushLight(sunLight, grid.getSunLightUBO(),
                EngineSetting.UNIFORM_SUN_DIRECTION,
                EngineSetting.UNIFORM_SUN_INTENSITY,
                EngineSetting.UNIFORM_SUN_COLOR);

        pushLight(moonLight, grid.getMoonLightUBO(),
                EngineSetting.UNIFORM_MOON_DIRECTION,
                EngineSetting.UNIFORM_MOON_INTENSITY,
                EngineSetting.UNIFORM_MOON_COLOR);
    }

    // Push \\

    private void pushLight(
            DirectionalLightStruct light,
            UBOInstance ubo,
            String directionName,
            String intensityName,
            String colorName) {

        ubo.updateUniform(directionName, light.getDirection());
        ubo.updateUniform(intensityName, light.getIntensity());
        ubo.updateUniform(colorName, light.getColor());
        uboManager.push(ubo);
    }
}