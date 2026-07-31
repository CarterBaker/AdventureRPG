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
import engine.util.mathematics.vectors.Vector3;

public class OverheadCloudSystem extends SystemPackage {

    /*
     * Renders the overhead volumetric cloud box each frame — a single
     * static box, built once at awake to cover the weather system's near
     * range, pushed as one ordinary (non-instanced) render call exactly
     * like world chunk geometry. No geometry is ever created per cloud or
     * per weather pattern — the box's own fragment shader loops over the
     * shared WeatherMapData entries and raymarches every overlapping
     * pattern in a single pass. Composites into the world scene beneath
     * the sky dome via FboRenderSystem, at LAYER_OVERHEAD, filling the
     * gap the skybox's own distant cloud sampling leaves directly above
     * the player.
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
    private FboInstance overheadFbo;
    private ModelInstance cloudBoxModel;

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

        this.overheadFbo = fboManager.cloneFbo(RuntimeSetting.FBO_OVERHEAD, context.getWindow());

        var meshData = meshManager.getMeshHandleFromMeshName(EngineSetting.CLOUD_BOX_MESH_NAME).getMeshData();
        MaterialInstance material = materialManager.cloneMaterial(EngineSetting.CLOUD_VOLUME_DEFAULT_MATERIAL);

        this.cloudBoxModel = create(ModelInstance.class);
        cloudBoxModel.constructor(meshData, material);

        assignBoxBounds(material);
    }

    // Box Bounds \\

    /*
     * Sized once and never touched again — the box only ever needs to
     * comfortably contain every near-range weather pattern's footprint,
     * since the grid system already keeps the player-relative coordinate
     * space centered near the origin every frame.
     */
    private void assignBoxBounds(MaterialInstance material) {

        float radiusBlocks = (weatherManager.getEffectiveNearRangeChunks()
                + EngineSetting.WEATHER_PATTERN_SKY_FOOTPRINT_CHUNKS) * EngineSetting.CHUNK_SIZE;

        float minAltitude = EngineSetting.OVERHEAD_CLOUD_BOX_MIN_ALTITUDE;
        float maxAltitude = EngineSetting.OVERHEAD_CLOUD_BOX_MAX_ALTITUDE;

        material.setUniform("u_boxCenter", new Vector3(0f, (minAltitude + maxAltitude) * 0.5f, 0f));
        material.setUniform("u_boxHalfExtent",
                new Vector3(radiusBlocks, (maxAltitude - minAltitude) * 0.5f, radiusBlocks));
    }

    @Override
    protected void update() {

        GridInstance grid = worldSystem.getGridInstance();

        bindGridLightingData(grid);

        renderManager.pushRenderCall(cloudBoxModel, overheadFbo, 0, context.getWindow());
        fboRenderSystem.pushFbo(overheadFbo, RuntimeSetting.LAYER_OVERHEAD, context.getWindow());
    }

    // Grid Lighting \\

    private void bindGridLightingData(GridInstance grid) {

        if (grid == null)
            return;

        MaterialInstance mat = cloudBoxModel.getMaterial();

        mat.setUBO(grid.getTimeDataUBO());
        mat.setUBO(grid.getSkyColorUBO());
        mat.setUBO(grid.getSunLightUBO());
        mat.setUBO(grid.getMoonLightUBO());
        mat.setUBO(grid.getWeatherMapUBO());
        mat.setUBO(grid.getWindDataUBO());
    }

    // Accessible \\

    public FboInstance getOverheadFbo() {
        return overheadFbo;
    }
}