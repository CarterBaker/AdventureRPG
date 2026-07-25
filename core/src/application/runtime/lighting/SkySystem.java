package application.runtime.lighting;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.runtime.RuntimeSetting;
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;

public class SkySystem extends SystemPackage {

    /*
     * Submits the sky pass render call each frame and queues the sky FBO
     * for compositing into the final scene. Binds this window's own grid's
     * Time/Sky/Sun/Moon UBO instances onto the sky pass material each frame
     * so the sky color and distant weather preview read the correct
     * location instead of whichever window updated last.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;
    private WorldSystem worldSystem;

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
        this.worldSystem = get(WorldSystem.class);
    }

    @Override
    protected void awake() {
        this.skyPass = passManager.getPassHandleFromPassName(RuntimeSetting.PASS_SKY);
        this.skyFbo = fboManager.cloneFbo(RuntimeSetting.FBO_SKY, context.getWindow());
    }

    @Override
    protected void update() {

        bindGridLightingData();

        renderManager.pushRenderCall(skyPass.getModelInstance(), skyFbo, 0, context.getWindow());
        fboRenderSystem.pushFbo(skyFbo, RuntimeSetting.LAYER_SKY, context.getWindow());
    }

    // Grid Lighting \\

    private void bindGridLightingData() {

        GridInstance grid = worldSystem.getGridInstance();

        if (grid == null)
            return;

        MaterialInstance mat = skyPass.getModelInstance().getMaterial();

        mat.setUBO(grid.getTimeDataUBO());
        mat.setUBO(grid.getSkyColorUBO());
        mat.setUBO(grid.getSunLightUBO());
        mat.setUBO(grid.getMoonLightUBO());
    }

    // Accessible \\

    public FboInstance getSkyFbo() {
        return skyFbo;
    }
}