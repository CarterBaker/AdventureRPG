package application.runtime.weather;

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

public class PrecipitationSystem extends SystemPackage {

    /*
     * Renders rain and snow in a single fullscreen pass composited over the
     * lit world. The pass reads the scene depth so nothing falls in front of
     * terrain it should be behind, and this grid's PrecipitationData column
     * map so nothing falls beneath a block. This system only clones the
     * per-window FBO target and binds the grid's own UBOs — what falls, how
     * hard, and where it is sheltered are all resolved per grid by
     * PrecipitationManager.
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FBOManager fboManager;
    private FBORenderSystem fboRenderSystem;
    private WorldSystem worldSystem;

    // Render Target
    private PassHandle precipitationPass;
    private FBOInstance precipitationFbo;

    // Internal \\

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FBOManager.class);
        this.fboRenderSystem = get(FBORenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
    }

    @Override
    protected void awake() {

        this.precipitationPass = passManager.getPassHandleFromPassName(RuntimeSetting.PASS_PRECIPITATION);
        this.precipitationFbo = fboManager.cloneFbo(RuntimeSetting.FBO_PRECIPITATION, context.getWindow());

        MaterialInstance mat = precipitationPass.getModelInstance().getMaterial();
        mat.setUniform(RuntimeSetting.UNIFORM_SCENE_DEPTH, worldSystem.getWorldFbo().getDepthTexture());
    }

    @Override
    protected void lateUpdate() {

        bindGridPrecipitationData(worldSystem.getGridInstance());

        renderManager.pushRenderCall(
                precipitationPass.getModelInstance(),
                precipitationFbo,
                RuntimeSetting.PASS_DRAW_DEPTH,
                context.getWindow());
        fboRenderSystem.pushFbo(precipitationFbo, RuntimeSetting.LAYER_PRECIPITATION, context.getWindow());
    }

    // Grid Precipitation \\

    private void bindGridPrecipitationData(GridInstance grid) {

        if (grid == null)
            return;

        MaterialInstance mat = precipitationPass.getModelInstance().getMaterial();

        mat.setUBO(grid.getTimeDataUBO());
        mat.setUBO(grid.getSkyColorUBO());
        mat.setUBO(grid.getPrecipitationDataUBO());
    }

    // Accessible \\

    public FBOInstance getPrecipitationFbo() {
        return precipitationFbo;
    }
}
