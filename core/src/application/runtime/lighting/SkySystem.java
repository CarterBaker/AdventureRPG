package application.runtime.lighting;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.runtime.RuntimeSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class SkySystem extends SystemPackage {

    /*
     * Submits the sky pass render call each frame and queues the sky FBO
     * for compositing into the final scene.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;

    // Render Target
    private PassHandle skyPass;
    private FboInstance skyFbo;

    @Override
    protected void get() {

        // Internal
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FboManager.class);
        this.fboRenderSystem = get(FboRenderSystem.class);
    }

    @Override
    protected void awake() {
        this.skyPass = passManager.getPassHandleFromPassName(EngineSetting.PASS_SKY);
        this.skyFbo = fboManager.getFbo(RuntimeSetting.FBO_SKY);
    }

    @Override
    protected void update() {
        renderManager.pushRenderCall(skyPass.getModelInstance(), skyFbo, 0, context.getWindow());
        fboRenderSystem.pushFbo(skyFbo, RuntimeSetting.LAYER_SKY);
    }
}