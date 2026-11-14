package application.runtime.weather;

import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.pass.PassInstance;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.bootstrap.weatherpipeline.cloudmanager.CloudManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.runtime.RuntimeSetting;
import application.runtime.postprocessing.PostEffectsSystem;
import application.runtime.water.WaterSystem;
import application.runtime.world.WorldSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class PrecipitationSystem extends SystemPackage {

    /*
     * Renders rain and snow in a single fullscreen pass composited over the
     * lit world. The pass reads the scene depth so nothing falls in front of
     * terrain it should be behind, and this grid's PrecipitationData column
     * map so nothing falls beneath a block. This system only clones the
     * per-window FBO target and binds the grid's own UBOs — what falls, how
     * hard, and where it is sheltered are all resolved per grid by
     * PrecipitationManager. Drops stop at the water surface too, read from the
     * water target's depth, and nothing falls while the camera is under the
     * sea. Drops fall only beneath the rain-bearing clouds of the grid's
     * weather map, read through the cloud noise, and the rain shafts the
     * weather pass writes into its distance target are laid beneath them, so
     * that target is rendered first.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private PostEffectsSystem postEffectsSystem;
    private WorldSystem worldSystem;
    private WaterSystem waterSystem;
    private WeatherSystem weatherSystem;
    private CloudManager cloudManager;

    // Render Target
    private PassInstance precipitationPass;
    private FBOInstance precipitationFbo;

    // Internal \\

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.postEffectsSystem = get(PostEffectsSystem.class);
        this.worldSystem = get(WorldSystem.class);
        this.waterSystem = get(WaterSystem.class);
        this.weatherSystem = get(WeatherSystem.class);
        this.cloudManager = get(CloudManager.class);
    }

    @Override
    protected void awake() {

        this.precipitationPass = passManager.clonePass(RuntimeSetting.PASS_PRECIPITATION);
        this.precipitationFbo = fboManager.cloneFbo(RuntimeSetting.FBO_PRECIPITATION, context.getWindow());

        MaterialInstance mat = precipitationPass.getModelInstance().getMaterial();
        mat.setUniform(RuntimeSetting.UNIFORM_SCENE_DEPTH, worldSystem.getWorldFbo().getDepthTexture());
        mat.setUniform(RuntimeSetting.UNIFORM_WATER_DEPTH, waterSystem.getWaterFbo().getDepthTexture());
        mat.setUniform(
                RuntimeSetting.UNIFORM_CLOUD_DISTANCE,
                weatherSystem.getWeatherFbo().getColorTexture(RuntimeSetting.ATTACHMENT_DISTANCE));
        mat.setUniform(EngineSetting.UNIFORM_CLOUD_NOISE, cloudManager.getCloudNoiseTexture());
    }

    @Override
    protected void update() {
        renderManager.ensureFboRendered(weatherSystem.getWeatherFbo(), context.getWindow());
    }

    @Override
    protected void lateUpdate() {

        if (waterSystem.isCameraSubmerged())
            return;

        bindGridPrecipitationData(worldSystem.getGridInstance());

        renderManager.pushRenderCall(
                precipitationPass.getModelInstance(),
                precipitationFbo,
                RuntimeSetting.PASS_DRAW_DEPTH,
                context.getWindow());
        postEffectsSystem.pushSceneLayer(precipitationFbo, RuntimeSetting.LAYER_PRECIPITATION);
    }

    // Grid Precipitation \\

    private void bindGridPrecipitationData(GridInstance grid) {

        if (grid == null)
            return;

        MaterialInstance mat = precipitationPass.getModelInstance().getMaterial();

        mat.setUBO(grid.getTimeDataUBO());
        mat.setUBO(grid.getSkyColorUBO());
        mat.setUBO(grid.getPrecipitationDataUBO());
        mat.setUBO(grid.getWeatherMapUBO());
    }

    // Accessible \\

    public FBOInstance getPrecipitationFbo() {
        return precipitationFbo;
    }
}
