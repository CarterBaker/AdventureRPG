package application.runtime.weather;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.runtime.RuntimeSetting;
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;

public class WeatherSystem extends SystemPackage {

    /*
     * Renders the sky's clouds in a single fullscreen raymarched pass, driven
     * by the shared processing-pass pipeline. This system only clones the
     * per-window FBO target and binds the grid's own UBOs — the weather map,
     * its cloud layers, and where the flow has carried them are all written
     * per grid by WeatherMapBufferSystem, so nothing about the clouds is a
     * material setting here. The target is composited through its own resolve
     * material, which smooths the march's per-pixel dither while upscaling.
     */

    // Internal
    private PassManager passManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;
    private WorldSystem worldSystem;

    // Render Target
    private PassHandle weatherPass;
    private FboInstance weatherFbo;

    // Internal \\

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FboManager.class);
        this.fboRenderSystem = get(FboRenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
    }

    @Override
    protected void awake() {
        this.weatherPass = passManager.getPassHandleFromPassName(RuntimeSetting.PASS_WEATHER);
        this.weatherFbo = fboManager.cloneFbo(RuntimeSetting.FBO_WEATHER, context.getWindow());

        fboRenderSystem.setBlitOverride(
                weatherFbo,
                null,
                materialManager.cloneMaterial(RuntimeSetting.MATERIAL_WEATHER_RESOLVE));
    }

    @Override
    protected void lateUpdate() {

        GridInstance grid = worldSystem.getGridInstance();

        bindGridLightingData(grid);

        renderManager.pushRenderCall(weatherPass.getModelInstance(), weatherFbo, 0, context.getWindow());
        fboRenderSystem.pushFbo(weatherFbo, RuntimeSetting.LAYER_WEATHER, context.getWindow());
    }

    // Grid Lighting \\

    private void bindGridLightingData(GridInstance grid) {

        if (grid == null)
            return;

        MaterialInstance mat = weatherPass.getModelInstance().getMaterial();

        mat.setUBO(grid.getSkyColorUBO());
        mat.setUBO(grid.getSunLightUBO());
        mat.setUBO(grid.getMoonLightUBO());
        mat.setUBO(grid.getWeatherMapUBO());
    }

    // Accessible \\

    public FboInstance getWeatherFbo() {
        return weatherFbo;
    }
}
