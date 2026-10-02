package application.runtime.water;

import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.grid.WaterTargetStruct;
import application.runtime.RuntimeSetting;
import application.runtime.postprocessing.LightingSystem;
import application.runtime.sky.SkySystem;
import application.runtime.weather.WeatherSystem;
import application.runtime.world.WorldSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class WaterSystem extends SystemPackage {

    /*
     * Forward water for this window. Clones the water target, binds it onto
     * the grid with everything water reads — the lit scene it refracts, the
     * scene depth it tests against, and the sky and clouds it reflects — and
     * composites it over the lit world. The target is queued behind the lit,
     * sky and cloud targets, so water always reads this frame's scene. While
     * the camera is under the sea, the underwater pass fogs the whole scene
     * into the target first, and the water surface draws over it seen from
     * below.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private WorldSystem worldSystem;
    private LightingSystem lightingSystem;
    private SkySystem skySystem;
    private WeatherSystem weatherSystem;

    // Render Target
    private PassHandle underwaterPass;
    private FBOInstance waterFbo;

    // Internal \\

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
        this.lightingSystem = get(LightingSystem.class);
        this.skySystem = get(SkySystem.class);
        this.weatherSystem = get(WeatherSystem.class);
    }

    @Override
    protected void awake() {

        this.underwaterPass = passManager.getPassHandleFromPassName(RuntimeSetting.PASS_UNDERWATER);
        this.waterFbo = fboManager.cloneFbo(RuntimeSetting.FBO_WATER, context.getWindow());

        worldSystem.getGridInstance().setWaterTarget(new WaterTargetStruct(
                waterFbo,
                worldSystem.getWorldFbo(),
                lightingSystem.getLitFbo(),
                skySystem.getSkyFbo(),
                weatherSystem.getWeatherFbo(),
                RuntimeSetting.ATTACHMENT_COLOR,
                RuntimeSetting.ATTACHMENT_DISTANCE));
    }

    @Override
    protected void update() {
        renderManager.ensureFboRendered(skySystem.getSkyFbo(), context.getWindow());
        renderManager.ensureFboRendered(weatherSystem.getWeatherFbo(), context.getWindow());
        renderManager.ensureFboRendered(waterFbo, context.getWindow());
    }

    @Override
    protected void lateUpdate() {

        GridInstance grid = worldSystem.getGridInstance();

        if (grid != null && grid.getWaveInstance().isCameraSubmerged())
            pushUnderwaterPass(grid);

        fboRenderSystem.pushFbo(waterFbo, RuntimeSetting.LAYER_WATER, context.getWindow());
    }

    // Underwater \\

    private void pushUnderwaterPass(GridInstance grid) {

        WaterTargetStruct waterTarget = grid.getWaterTarget();
        MaterialInstance mat = underwaterPass.getModelInstance().getMaterial();

        mat.setUBO(grid.getOceanDataUBO());
        mat.setUBO(grid.getSunLightUBO());
        mat.setUBO(grid.getMoonLightUBO());
        mat.setUBO(grid.getSkyColorUBO());
        mat.setUniform(EngineSetting.UNIFORM_WATER_SCENE_COLOR, waterTarget.getSceneColorTexture());
        mat.setUniform(EngineSetting.UNIFORM_WATER_SCENE_DEPTH, waterTarget.getSceneDepthTexture());

        renderManager.pushRenderCall(
                underwaterPass.getModelInstance(),
                waterFbo,
                RuntimeSetting.UNDERWATER_DRAW_DEPTH,
                context.getWindow());
    }

    // Accessible \\

    public FBOInstance getWaterFbo() {
        return waterFbo;
    }

    public boolean isCameraSubmerged() {

        GridInstance grid = worldSystem.getGridInstance();

        return grid != null && grid.getWaveInstance().isCameraSubmerged();
    }
}
