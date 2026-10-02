package application.runtime.postprocessing;

import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.renderpipeline.rendermanager.FBORenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.runtime.RuntimeSetting;
import application.runtime.weather.WeatherSystem;
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;

public class LightingSystem extends SystemPackage {

    /*
     * Deferred lighting pass. Reads the G-buffer and SSAO result, lays the
     * weather pass's clouds standing in front of each fragment and the fog of
     * a cloud the camera stands in over it, and writes LitScene. Binds this
     * window's grid UBOs — sun, moon and sky color — and the weather targets
     * onto the pass each frame. The world, SSAO and weather targets are queued
     * ahead of this pass, so the lit world is always drawn with this frame's
     * camera, the same one the sky and clouds are drawn with.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private WorldSystem worldSystem;
    private SSAOSystem ssaoSystem;
    private WeatherSystem weatherSystem;

    // Render Target
    private PassHandle lightingPass;
    private FBOInstance litFbo;

    // Base \\

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
        this.ssaoSystem = get(SSAOSystem.class);
        this.weatherSystem = get(WeatherSystem.class);
    }

    @Override
    protected void awake() {
        this.lightingPass = passManager.getPassHandleFromPassName(RuntimeSetting.PASS_LIGHTING);
        this.litFbo = fboManager.cloneFbo(RuntimeSetting.FBO_LIT, context.getWindow());

        FBOInstance worldFbo = worldSystem.getWorldFbo();
        FBOInstance ssaoFbo = ssaoSystem.getSsaoFbo();
        MaterialInstance mat = lightingPass.getModelInstance().getMaterial();

        mat.setUniform(RuntimeSetting.UNIFORM_G_ALBEDO, worldFbo.getColorTexture(RuntimeSetting.ATTACHMENT_ALBEDO));
        mat.setUniform(RuntimeSetting.UNIFORM_G_NORMAL, worldFbo.getColorTexture(RuntimeSetting.ATTACHMENT_NORMAL));
        mat.setUniform(RuntimeSetting.UNIFORM_G_MATERIAL, worldFbo.getColorTexture(RuntimeSetting.ATTACHMENT_MATERIAL));
        mat.setUniform(RuntimeSetting.UNIFORM_G_DEPTH, worldFbo.getDepthTexture());
        mat.setUniform(RuntimeSetting.UNIFORM_SSAO_TEXTURE, ssaoFbo.getColorTexture(RuntimeSetting.ATTACHMENT_AO));
    }

    @Override
    protected void update() {

        bindGridLightingData();
        bindCloudTargets();

        renderManager.ensureFboRendered(worldSystem.getWorldFbo(), context.getWindow());
        renderManager.ensureFboRendered(ssaoSystem.getSsaoFbo(), context.getWindow());
        renderManager.ensureFboRendered(weatherSystem.getWeatherFbo(), context.getWindow());
        renderManager.pushRenderCall(
                lightingPass.getModelInstance(),
                litFbo,
                RuntimeSetting.PASS_DRAW_DEPTH,
                context.getWindow());
        fboRenderSystem.pushFbo(litFbo, RuntimeSetting.LAYER_WORLD, context.getWindow());
    }

    // Grid Lighting \\

    private void bindGridLightingData() {

        GridInstance grid = worldSystem.getGridInstance();

        if (grid == null)
            return;

        MaterialInstance mat = lightingPass.getModelInstance().getMaterial();
        mat.setUBO(grid.getSunLightUBO());
        mat.setUBO(grid.getMoonLightUBO());
        mat.setUBO(grid.getSkyColorUBO());
    }

    // Clouds \\

    private void bindCloudTargets() {

        FBOInstance weatherFbo = weatherSystem.getWeatherFbo();
        MaterialInstance mat = lightingPass.getModelInstance().getMaterial();

        mat.setUniform(RuntimeSetting.UNIFORM_CLOUD_COLOR, weatherFbo.getColorTexture(RuntimeSetting.ATTACHMENT_COLOR));
        mat.setUniform(
                RuntimeSetting.UNIFORM_CLOUD_DISTANCE,
                weatherFbo.getColorTexture(RuntimeSetting.ATTACHMENT_DISTANCE));
    }

    // Accessible \\

    public FBOInstance getLitFbo() {
        return litFbo;
    }
}