package application.runtime.weather;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.passmanager.PassManager;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.runtime.RuntimeSetting;
import application.runtime.world.WorldSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;

public class WeatherSystem extends SystemPackage {

    /*
     * Renders all weather/cloud visuals in a single fullscreen raymarched
     * pass, driven by the shared processing-pass pipeline (see PassManager)
     * — the same pattern SkySystem/SSAOSystem/LightingSystem use — instead
     * of constructing its own mesh/material/model at runtime. The pass's
     * own descriptor (processingpasses/Weather.json) owns the mesh and
     * material/shader wiring; this system only clones the per-window FBO
     * target and pushes the handful of uniforms that can't be known until
     * runtime (raymarch bounds and drift direction/speed, both derived
     * from world/weather data resolved after bootstrap).
     *
     * Composites into the scene at LAYER_WEATHER, beneath world geometry.
     * Cloud internal motion and silhouette orientation are driven by the
     * same world-rotation drift that moves weather patterns themselves —
     * never the independent local wind system, which stays reserved for
     * future systems (grass, trees, water).
     */

    // Internal
    private PassManager passManager;
    private RenderManager renderManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;
    private WorldSystem worldSystem;
    private WeatherManager weatherManager;

    // Render Target
    private PassHandle weatherPass;
    private FboInstance weatherFbo;

    // Internal \\

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FboManager.class);
        this.fboRenderSystem = get(FboRenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
        this.weatherManager = get(WeatherManager.class);
    }

    @Override
    protected void awake() {

        this.weatherPass = passManager.getPassHandleFromPassName(RuntimeSetting.PASS_WEATHER);
        this.weatherFbo = fboManager.cloneFbo(RuntimeSetting.FBO_WEATHER, context.getWindow());

        MaterialInstance material = weatherPass.getModelInstance().getMaterial();

        assignRaymarchBounds(material);
        assignDriftUniforms(material);
    }

    // Raymarch Bounds \\

    /*
     * Sized once and never touched again — comfortably contains every
     * in-range weather pattern's footprint, the same radius the old
     * overhead box used.
     */
    private void assignRaymarchBounds(MaterialInstance material) {

        float maxDistanceBlocks = (weatherManager.getEffectiveRangeChunks()
                + EngineSetting.WEATHER_PATTERN_SKY_FOOTPRINT_CHUNKS) * EngineSetting.CHUNK_SIZE;

        material.setUniform("u_cloudAltitudeMin", EngineSetting.WEATHER_CLOUD_ALTITUDE_MIN);
        material.setUniform("u_cloudAltitudeMax", EngineSetting.WEATHER_CLOUD_ALTITUDE_MAX);
        material.setUniform("u_cloudMaxDistance", maxDistanceBlocks);
    }

    // Drift Uniforms \\

    /*
     * WeatherPatternManager advects every pattern at the negated world-
     * rotation drift (see its assignVelocity()) so a pattern's own motion
     * cancels the noise field's rotation phase instead of doubling it.
     * The direction here matches that same negated axis so the cloud
     * puffs' internal scroll reads as moving with the pattern instead of
     * against it. Constant for the session, so this is set once here
     * instead of re-pushed every frame.
     */
    private void assignDriftUniforms(MaterialInstance material) {

        float driftSpeedBlocksPerSecond = weatherManager.getWorldDriftChunksPerSecondX() * EngineSetting.CHUNK_SIZE;

        material.setUniform("u_weatherDriftDirection", new Vector2(-1f, 0f));
        material.setUniform("u_weatherDriftSpeed", driftSpeedBlocksPerSecond);
    }

    @Override
    protected void update() {

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

        mat.setUBO(grid.getTimeDataUBO());
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