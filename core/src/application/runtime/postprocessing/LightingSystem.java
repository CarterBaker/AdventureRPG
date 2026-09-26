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
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;

public class LightingSystem extends SystemPackage {

    /*
     * Deferred lighting pass. Reads the G-buffer and SSAO result, integrates
     * clouds between camera and fragment against the grid's weather map, and
     * writes LitScene. Binds this window's grid UBOs — sun, moon, sky color and
     * weather map — onto the pass each frame.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private WorldSystem worldSystem;
    private SSAOSystem ssaoSystem;

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
        mat.setUBO(grid.getWeatherMapUBO());
    }
}