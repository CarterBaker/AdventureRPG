package application.runtime.postprocessing;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.runtime.RuntimeSetting;
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;

public class LightingSystem extends SystemPackage {

    /*
     * Deferred lighting pass. Reads the full G-buffer and SSAO result,
     * computes final lit color, and writes into LitScene. LitScene composites
     * at LAYER_WORLD — replacing the old direct MainScene composite.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;
    private WorldSystem worldSystem;
    private SSAOSystem ssaoSystem;

    // Render Target
    private PassHandle lightingPass;
    private FboInstance litFbo;

    // Internal \\

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FboManager.class);
        this.fboRenderSystem = get(FboRenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
        this.ssaoSystem = get(SSAOSystem.class);
    }

    @Override
    protected void awake() {
        this.lightingPass = passManager.getPassHandleFromPassName(RuntimeSetting.PASS_LIGHTING);
        this.litFbo = fboManager.cloneFbo(RuntimeSetting.FBO_LIT, context.getWindow());

        FboInstance worldFbo = worldSystem.getWorldFbo();
        FboInstance ssaoFbo = ssaoSystem.getSsaoFbo();
        MaterialInstance mat = lightingPass.getModelInstance().getMaterial();

        mat.setUniform("u_gAlbedo", worldFbo.getColorTexture("albedo"));
        mat.setUniform("u_gNormal", worldFbo.getColorTexture("normal"));
        mat.setUniform("u_gMaterial", worldFbo.getColorTexture("material"));
        mat.setUniform("u_gDepth", worldFbo.getDepthTexture());
        mat.setUniform("u_ssaoTex", ssaoFbo.getColorTexture("ao"));
    }

    @Override
    protected void update() {
        renderManager.pushRenderCall(lightingPass.getModelInstance(), litFbo, 0, context.getWindow());
        fboRenderSystem.pushFbo(litFbo, RuntimeSetting.LAYER_WORLD, context.getWindow());
    }
}