package application.bootstrap.weatherpipeline.weatherrendersystem;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.cloudbuffer.CloudBufferInstance;
import application.bootstrap.weatherpipeline.cloudbuffermanager.CloudBufferManager;
import application.bootstrap.weatherpipeline.overheadmanager.OverheadCellStruct;
import application.bootstrap.weatherpipeline.overheadmanager.OverheadManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class CloudRenderSystem extends SystemPackage {

    /*
     * Rebuilds every cloud archetype's shared instanced buffer once per
     * frame from OverheadManager's live cell grid, then registers each
     * non-empty buffer into every active grid window's render queue via
     * RenderManager.pushWeatherCall(). This is exactly the class
     * CloudBufferManager/CloudBufferInstance's own doc comments have been
     * describing all along ("Every buffer here is fully rebuilt every
     * frame by CloudRenderSystem from OverheadManager's live cell grid" —
     * see CloudBufferManager; "driven by CloudRenderSystem.render()" — see
     * CloudBufferInstance) — this is that missing driver.
     *
     * Materials are cloned once per distinct CloudHandle archetype and
     * cached forever — every instance sharing that archetype draws
     * through the exact same MaterialInstance, so the whole archetype is
     * one instanced draw call regardless of how many OverheadCellStructs
     * reference it. Baked-once uniforms (colour, scale, density, edge
     * softness, puff jitter) come straight off CloudData — see the "Baked
     * once per material clone in CloudRenderSystem.resolveMaterial()" doc
     * comments already sitting on CloudVolumeShader.vsh/.fsh.
     *
     * Owned by WeatherRenderSystem, which supplies the per-window
     * fbo/window pairs to submit() — this class has no window/grid
     * awareness of its own, mirroring how FrustumCullingSystem never
     * touches WorldStreamManager directly and is instead handed a
     * GridInstance by WorldRenderManager each call.
     */

    // Fixed instance layout: world position (3), random seed (1), fade alpha (1)
    private static final int[] CLOUD_INSTANCE_ATTR_SIZES = { 3, 1, 1 };

    // Internal
    private OverheadManager overheadManager;
    private CloudBufferManager cloudBufferManager;
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private UBOManager uboManager;
    private RenderManager renderManager;

    // Mesh
    private MeshHandle cloudMeshHandle;

    // Material Cache — one clone per distinct cloud archetype, never rebuilt
    private Object2ObjectOpenHashMap<CloudHandle, MaterialInstance> cloudHandle2Material;

    // Scratch — reused every rebuild pass, never reallocated
    private final float[] instanceScratch = new float[5];

    // Internal \\

    @Override
    protected void create() {
        this.cloudHandle2Material = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.overheadManager = get(OverheadManager.class);
        this.cloudBufferManager = get(CloudBufferManager.class);
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.uboManager = get(UBOManager.class);
        this.renderManager = get(RenderManager.class);
    }

    @Override
    protected void awake() {

        this.cloudMeshHandle = meshManager.getMeshHandleFromMeshName(EngineSetting.CLOUD_VOLUME_MESH_NAME);

        pushCloudSettings();
    }

    // Settings \\

    /*
     * Pushed once at bootstrap, never per frame — see CloudSettingsData.glsl's
     * own doc comment. Horizon distance converts HORIZON_DISTANCE from chunk
     * units to world/block units, the same convention OverheadManager and
     * RegionSampleBranch already use, since CloudVolumeShader.vsh compares it
     * directly against world-space camera distance.
     */
    private void pushCloudSettings() {

        UBOHandle cloudSettingsData = uboManager.getUBOHandleFromUBOName(EngineSetting.CLOUD_SETTINGS_DATA_UBO);

        float horizonDistanceBlocks = EngineSetting.HORIZON_DISTANCE * EngineSetting.CHUNK_SIZE;

        cloudSettingsData.updateUniform(EngineSetting.UNIFORM_CLOUD_HORIZON_DISTANCE, horizonDistanceBlocks);
        cloudSettingsData.updateUniform(EngineSetting.UNIFORM_CLOUD_MIN_SCALE, EngineSetting.CLOUD_HORIZON_MIN_SCALE);
        cloudSettingsData.updateUniform(EngineSetting.UNIFORM_CLOUD_MAX_SCALE, EngineSetting.CLOUD_HORIZON_MAX_SCALE);

        uboManager.push(cloudSettingsData);
    }

    // Rebuild \\

    /*
     * Clears every archetype buffer, then walks every currently active
     * overhead cell and re-adds its instance data fresh. Called once per
     * frame, before any window submits — never per-window, since the
     * underlying cell grid is itself global (see OverheadManager's own
     * doc comment on why a cell's identity must never re-roll per frame).
     */
    void rebuildInstances() {

        cloudBufferManager.clearAll();

        for (OverheadCellStruct cell : overheadManager.getActiveCells().values())
            addInstance(cell);
    }

    private void addInstance(OverheadCellStruct cell) {

        CloudHandle cloudHandle = cell.getCloudHandle();

        CloudBufferInstance buffer = cloudBufferManager.getOrCreateCloudBuffer(
                cloudHandle, cloudMeshHandle, CLOUD_INSTANCE_ATTR_SIZES);

        instanceScratch[0] = (float) (cell.getCurrentChunkX() * EngineSetting.CHUNK_SIZE);
        instanceScratch[1] = cell.getEffectiveAltitude();
        instanceScratch[2] = (float) (cell.getCurrentChunkZ() * EngineSetting.CHUNK_SIZE);
        instanceScratch[3] = cell.getRandomSeed();
        instanceScratch[4] = cell.getFadeAlpha();

        buffer.addInstance(instanceScratch);
    }

    // Submit \\

    /*
     * Registers every non-empty cloud archetype buffer into this window's
     * render queue for the given target fbo. Safe to call once per active
     * grid window each frame — RenderManager.pushWeatherCall() dedupes
     * against a buffer already queued for this fbo this frame.
     */
    void submit(FboInstance fbo, WindowInstance window) {

        for (Object2ObjectMap.Entry<CloudHandle, CloudBufferInstance> entry : cloudBufferManager.getBufferMap()
                .object2ObjectEntrySet()) {

            CloudBufferInstance buffer = entry.getValue();

            if (buffer.isEmpty())
                continue;

            MaterialInstance material = resolveMaterial(entry.getKey());
            renderManager.pushWeatherCall(buffer, material, fbo, window);
        }
    }

    // Material Resolution \\

    private MaterialInstance resolveMaterial(CloudHandle cloudHandle) {

        MaterialInstance existing = cloudHandle2Material.get(cloudHandle);

        if (existing != null)
            return existing;

        MaterialInstance material = materialManager.cloneMaterial(EngineSetting.CLOUD_VOLUME_MATERIAL_NAME);

        material.setUniform(EngineSetting.UNIFORM_CLOUD_COLOR, cloudHandle.getCloudColor());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_SCALE, cloudHandle.getScale());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_VERTICAL_THICKNESS, cloudHandle.getVerticalThickness());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_DENSITY, cloudHandle.getDensity());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_EDGE_SOFTNESS, cloudHandle.getEdgeSoftness());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_PUFF_JITTER, cloudHandle.getPuffJitter());

        cloudHandle2Material.put(cloudHandle, material);

        return material;
    }
}