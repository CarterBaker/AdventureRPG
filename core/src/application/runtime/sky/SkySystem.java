package application.runtime.sky;

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

public class SkySystem extends SystemPackage {

    /*
     * Submits the sky pass render call each frame and queues the sky FBO
     * for compositing. Binds this window's own grid's Time, Sky, and Sun
     * UBO instances so sky color and the sun-side glow reflect this grid's
     * own location. The sky pass has no knowledge of weather or clouds —
     * that is handled entirely by the weather fullscreen pass.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private WorldSystem worldSystem;

    // Render Target
    private PassHandle skyPass;
    private FBOInstance skyFbo;

    @Override
    protected void get() {

        // Internal
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
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

        renderManager.pushRenderCall(
                skyPass.getModelInstance(),
                skyFbo,
                RuntimeSetting.PASS_DRAW_DEPTH,
                context.getWindow());
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
    }

    // Accessible \\

    public FBOInstance getSkyFbo() {
        return skyFbo;
    }
}