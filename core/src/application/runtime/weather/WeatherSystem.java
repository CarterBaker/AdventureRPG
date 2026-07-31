package application.runtime.weather;

import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.runtime.RuntimeSetting;
import application.runtime.world.WorldSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class WeatherSystem extends SystemPackage {

    /*
     * Renders all weather/cloud visuals in a single fullscreen raymarched
     * pass — a full-screen quad reconstructing the camera's world-space
     * view ray, raymarched against every near-range weather pattern's own
     * cloud entries straight from WeatherMapData. Replaces the old split
     * skybox-ring + overhead-box systems entirely; the sky pass now only
     * ever draws atmosphere color. Composites into the scene at
     * LAYER_WEATHER, beneath world geometry, exactly like the box system
     * did before it.
     */

    // Internal
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;
    private WorldSystem worldSystem;
    private WeatherManager weatherManager;

    // Render Target
    private FboInstance weatherFbo;
    private ModelInstance weatherModel;

    // Internal \\

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.fboManager = get(FboManager.class);
        this.fboRenderSystem = get(FboRenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
        this.weatherManager = get(WeatherManager.class);
    }

    @Override
    protected void awake() {

        this.weatherFbo = fboManager.cloneFbo(RuntimeSetting.FBO_WEATHER, context.getWindow());

        var meshData = meshManager.getMeshHandleFromMeshName(EngineSetting.DEFAULT_BLIT_MESH).getMeshData();
        MaterialInstance material = materialManager.cloneMaterial(EngineSetting.WEATHER_DEFAULT_MATERIAL);

        this.weatherModel = create(ModelInstance.class);
        weatherModel.constructor(meshData, material);

        assignRaymarchBounds(material);
    }

    // Raymarch Bounds \\

    /*
     * Sized once and never touched again — comfortably contains every
     * near-range weather pattern's footprint, the same radius the old
     * overhead box used.
     */
    private void assignRaymarchBounds(MaterialInstance material) {

        float maxDistanceBlocks = (weatherManager.getEffectiveNearRangeChunks()
                + EngineSetting.WEATHER_PATTERN_SKY_FOOTPRINT_CHUNKS) * EngineSetting.CHUNK_SIZE;

        material.setUniform("u_cloudAltitudeMin", EngineSetting.WEATHER_CLOUD_ALTITUDE_MIN);
        material.setUniform("u_cloudAltitudeMax", EngineSetting.WEATHER_CLOUD_ALTITUDE_MAX);
        material.setUniform("u_cloudMaxDistance", maxDistanceBlocks);
    }

    @Override
    protected void update() {

        GridInstance grid = worldSystem.getGridInstance();

        bindGridLightingData(grid);

        renderManager.pushRenderCall(weatherModel, weatherFbo, 0, context.getWindow());
        fboRenderSystem.pushFbo(weatherFbo, RuntimeSetting.LAYER_WEATHER, context.getWindow());
    }

    // Grid Lighting \\

    private void bindGridLightingData(GridInstance grid) {

        if (grid == null)
            return;

        MaterialInstance mat = weatherModel.getMaterial();

        mat.setUBO(grid.getTimeDataUBO());
        mat.setUBO(grid.getSkyColorUBO());
        mat.setUBO(grid.getSunLightUBO());
        mat.setUBO(grid.getMoonLightUBO());
        mat.setUBO(grid.getWeatherMapUBO());
        mat.setUBO(grid.getWindDataUBO());
    }

    // Accessible \\

    public FboInstance getWeatherFbo() {
        return weatherFbo;
    }
}