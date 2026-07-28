package application.runtime.weather;

import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.runtime.RuntimeSetting;
import application.runtime.world.WorldSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class OverheadCloudSystem extends SystemPackage {

    /*
     * Renders the overhead volumetric cloud box each frame — a single
     * shared box mesh, drawn once per near-range WeatherMapData entry via
     * instancing. gl_InstanceID indexes directly into the same UBO slots
     * WeatherMapBufferSystem already wrote (nearest-first, so the leading
     * N slots are always exactly the near-range entries) — no separate
     * per-instance CPU buffer exists or is needed. Composites into the
     * world scene beneath the sky dome via FboRenderSystem, at
     * LAYER_OVERHEAD, filling the gap the skybox's own distant cloud
     * sampling leaves directly above the player. ensureFboRendered() keeps
     * the FBO clearing correctly on a clear-sky frame even when zero
     * instances are drawn.
     */

    // Internal
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private FboManager fboManager;
    private FboRenderSystem fboRenderSystem;
    private WeatherPatternManager weatherPatternManager;
    private WorldSystem worldSystem;

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
        this.weatherPatternManager = get(WeatherPatternManager.class);
        this.worldSystem = get(WorldSystem.class);
    }

    @Override
    protected void awake() {

        this.overheadFbo = fboManager.cloneFbo(RuntimeSetting.FBO_OVERHEAD, context.getWindow());

        var meshData = meshManager.getMeshHandleFromMeshName(EngineSetting.CLOUD_BOX_MESH_NAME).getMeshData();
        MaterialInstance material = materialManager.cloneMaterial(EngineSetting.CLOUD_VOLUME_DEFAULT_MATERIAL);

        this.cloudBoxModel = create(ModelInstance.class);
        cloudBoxModel.constructor(meshData, material);
    }

    @Override
    protected void update() {

        bindGridLightingData();

        // Registers the FBO for this frame's clear/bind even if zero
        // instances end up drawn — see class doc comment.
        renderManager.ensureFboRendered(overheadFbo, context.getWindow());

        int instanceCount = weatherPatternManager.getNearRangeWeatherMapEntryCount();

        if (instanceCount > 0)
            renderManager.pushRenderCall(cloudBoxModel, overheadFbo, 0, instanceCount, context.getWindow());

        fboRenderSystem.pushFbo(overheadFbo, RuntimeSetting.LAYER_OVERHEAD, context.getWindow());
    }

    // Grid Lighting \\

    private void bindGridLightingData() {

        GridInstance grid = worldSystem.getGridInstance();

        if (grid == null)
            return;

        MaterialInstance mat = cloudBoxModel.getMaterial();

        mat.setUBO(grid.getTimeDataUBO());
        mat.setUBO(grid.getSkyColorUBO());
        mat.setUBO(grid.getSunLightUBO());
        mat.setUBO(grid.getMoonLightUBO());
    }

    // Accessible \\

    public FboInstance getOverheadFbo() {
        return overheadFbo;
    }
}