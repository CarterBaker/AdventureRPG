package application.runtime.lighting;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbo.FboManager;
import application.bootstrap.renderpipeline.fborendermanager.FboRenderManager;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import engine.root.SystemPackage;

public class SkySystem extends SystemPackage {

    private static final String SKY_FBO = "SkyScene";

    private PassManager passManager;
    private FboManager fboManager;
    private FboRenderManager fboRenderManager;

    private PassHandle skyPass;
    private FboInstance skyFbo;

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.fboManager = get(FboManager.class);
        this.fboRenderManager = get(FboRenderManager.class);

        this.skyPass = passManager.getPassHandleFromPassName("Sky");
    }

    @Override
    protected void awake() {
        this.skyFbo = fboManager.getFbo(SKY_FBO);
    }

    @Override
    protected void update() {
        passManager.pushPass(skyPass, SKY_FBO, context.getWindow());
        fboRenderManager.pushFbo(skyFbo);
    }
}
