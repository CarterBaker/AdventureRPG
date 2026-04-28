package application.runtime.lighting;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class SkySystem extends SystemPackage {

    private PassManager passManager;
    private RenderManager renderManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;

    private PassHandle skyPass;
    private FboInstance skyFbo;

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FboManager.class);
        this.fboRenderSystem = get(FboRenderSystem.class);

    }

    @Override
    protected void awake() {
        this.skyPass = passManager.getPassHandleFromPassName(EngineSetting.PASS_SKY);
        this.skyFbo = fboManager.getFbo(EngineSetting.FBO_SKY);
    }

    @Override
    protected void update() {
        renderManager.pushRenderCall(skyPass.getModelInstance(), skyFbo, 0, context.getWindow());
        fboRenderSystem.pushFbo(skyFbo);
    }
}
