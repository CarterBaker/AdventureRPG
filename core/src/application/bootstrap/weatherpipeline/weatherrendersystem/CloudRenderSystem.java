package application.bootstrap.weatherpipeline.weatherrendersystem;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.overheadmanager.OverheadCellStruct;
import application.bootstrap.weatherpipeline.overheadmanager.OverheadManager;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class CloudRenderSystem extends SystemPackage {

    /*
     * Owns one instanced GPU buffer per cloud archetype — every active,
     * cloud-bearing overhead cell sharing a CloudHandle draws in a single
     * instanced call against a single shared MaterialInstance for that
     * archetype, baked once the first time that archetype is ever used.
     * Rebuilt fully every frame from OverheadManager's active cells, since
     * position, fade, and intensity all change continuously as patterns
     * drift, stream, and fade. Owned by WeatherRenderSystem, which supplies
     * the per-window fbo/window pairs to submit().
     */

    private static final int[] INSTANCE_ATTR_SIZES = { 4, 4 };

    private OverheadManager overheadManager;
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private UBOManager uboManager;
    private RenderManager renderManager;
    private WeatherManager weatherManager;
    private WorldManager worldManager;

    private MeshHandle cloudMeshHandle;

    private Object2ObjectOpenHashMap<CloudHandle, CompositeBufferInstance> cloud2Buffer;
    private Object2ObjectOpenHashMap<CloudHandle, MaterialInstance> cloud2Material;

    private final float[] instanceScratch = new float[8];

    @Override
    protected void create() {
        this.cloud2Buffer = new Object2ObjectOpenHashMap<>();
        this.cloud2Material = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.overheadManager = get(OverheadManager.class);
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.uboManager = get(UBOManager.class);
        this.renderManager = get(RenderManager.class);
        this.weatherManager = get(WeatherManager.class);
        this.worldManager = get(WorldManager.class);
    }

    @Override
    protected void awake() {
        this.cloudMeshHandle = meshManager.getMeshHandleFromMeshName(EngineSetting.CLOUD_VOLUME_MESH_NAME);
        pushCloudSettings();
    }

    // Settings \\

    /*
     * Pushed once at bootstrap, never per frame. Real cloud OBJECTS are
     * capped to whichever is smaller: the actual visible chunk radius
     * (Settings.maxRenderDistance / 2, matching GridBuildSystem's own grid
     * radius), or the weather simulation's own design-intent near range
     * (EngineSetting.WEATHER_NEAR_RANGE_CHUNKS) — this is what guarantees a
     * real cloud object is never rendered floating over ground that was
     * never drawn. WeatherPatternManager derives its own streaming radius
     * from this identical expression, so a cloud dissolving into the
     * sky-dome preview and a cloud streaming out of the registry always
     * happen at the same boundary.
     */
    private void pushCloudSettings() {

        UBOHandle cloudSettingsData = uboManager.getUBOHandleFromUBOName(EngineSetting.CLOUD_SETTINGS_DATA_UBO);

        float cloudObjectRangeChunks = Math.min(
                settings.maxRenderDistance / 2f,
                (float) EngineSetting.WEATHER_NEAR_RANGE_CHUNKS);

        float horizonDistanceBlocks = cloudObjectRangeChunks * EngineSetting.CHUNK_SIZE
                * EngineSetting.CLOUD_HORIZON_RENDER_DISTANCE_SCALE;

        float maxSafeHorizonBlocks = EngineSetting.CAMERA_FAR_PLANE
                * EngineSetting.CLOUD_HORIZON_FAR_PLANE_SAFETY_MARGIN;
        horizonDistanceBlocks = Math.min(horizonDistanceBlocks, maxSafeHorizonBlocks);

        float skyViewDistanceBlocks = EngineSetting.WEATHER_FAR_RANGE_CHUNKS * EngineSetting.CHUNK_SIZE;
        float transitionStartBlocks = horizonDistanceBlocks * EngineSetting.CLOUD_VOLUME_FADE_START_RATIO;

        cloudSettingsData.updateUniform(EngineSetting.UNIFORM_CLOUD_HORIZON_DISTANCE, horizonDistanceBlocks);
        cloudSettingsData.updateUniform(EngineSetting.UNIFORM_CLOUD_MIN_SCALE, EngineSetting.CLOUD_HORIZON_MIN_SCALE);
        cloudSettingsData.updateUniform(EngineSetting.UNIFORM_CLOUD_MAX_SCALE, EngineSetting.CLOUD_HORIZON_MAX_SCALE);
        cloudSettingsData.updateUniform(EngineSetting.UNIFORM_CLOUD_SKY_VIEW_DISTANCE, skyViewDistanceBlocks);
        cloudSettingsData.updateUniform(EngineSetting.UNIFORM_CLOUD_TRANSITION_START, transitionStartBlocks);

        uboManager.push(cloudSettingsData);
    }

    // Update \\

    /*
     * Rebuilds every archetype's instance buffer from scratch each frame —
     * cheap, since the whole active-cell set is at most a few hundred
     * entries, and correct, since a cell's render position, fade, and
     * intensity are never stable frame-to-frame while its pattern drifts,
     * streams, or fades.
     */
    void updateInstances() {

        for (CompositeBufferInstance buffer : cloud2Buffer.values())
            buffer.clear();

        long referenceCoordinate = weatherManager.getReferenceCoordinate();
        int referenceChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int referenceChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        WorldHandle activeWorld = worldManager.getActiveWorld();

        for (OverheadCellStruct cell : overheadManager.getActiveCells().values())
            accumulateInstance(cell, referenceChunkX, referenceChunkZ, activeWorld);
    }

    /*
     * Resolves this cell's render-space position exactly as the old
     * per-cell system did — see WorldWrapUtility's own toroidal delta
     * helpers — then appends one row of instance data to its archetype's
     * buffer. instanceScratch is reused every call; addInstance() copies
     * it into the buffer's own backing array immediately, so reuse is safe.
     */
    private void accumulateInstance(
            OverheadCellStruct cell,
            int referenceChunkX,
            int referenceChunkZ,
            WorldHandle activeWorld) {

        if (!cell.hasCloud())
            return;

        CloudHandle cloudHandle = cell.getCloudHandle();
        CompositeBufferInstance buffer = getOrCreateArchetype(cloudHandle);

        double relativeChunkX = WorldWrapUtility.wrappedDeltaX(activeWorld, cell.getCurrentChunkX(), referenceChunkX);
        double relativeChunkZ = WorldWrapUtility.wrappedDeltaZ(activeWorld, cell.getCurrentChunkZ(), referenceChunkZ);

        instanceScratch[0] = (float) (relativeChunkX * EngineSetting.CHUNK_SIZE);
        instanceScratch[1] = cell.getEffectiveAltitude();
        instanceScratch[2] = (float) (relativeChunkZ * EngineSetting.CHUNK_SIZE);
        instanceScratch[3] = cell.getRandomSeed();
        instanceScratch[4] = cell.getDomainRotation();
        instanceScratch[5] = cell.getFadeAlpha();
        instanceScratch[6] = cell.getIntensity();
        instanceScratch[7] = cell.getSizeVariance();

        buffer.addInstance(instanceScratch);
    }

    private CompositeBufferInstance getOrCreateArchetype(CloudHandle cloudHandle) {

        CompositeBufferInstance buffer = cloud2Buffer.get(cloudHandle);

        if (buffer != null)
            return buffer;

        buffer = renderManager.createInstancedCompositeBuffer(cloudMeshHandle, INSTANCE_ATTR_SIZES);

        MaterialInstance material = materialManager.cloneMaterial(EngineSetting.CLOUD_VOLUME_MATERIAL_NAME);
        bakeArchetypeUniforms(material, cloudHandle);

        cloud2Buffer.put(cloudHandle, buffer);
        cloud2Material.put(cloudHandle, material);

        return buffer;
    }

    /*
     * Bakes every archetype-level value off CloudData into this
     * archetype's own shared MaterialInstance, once, the first time it is
     * ever used. Scale and vertical thickness stay the archetype's own
     * base values here — per-instance size variance (see
     * OverheadCellStruct/WeatherPatternLobeStruct) is applied later, in
     * the vertex shader, against this shared base.
     */
    private void bakeArchetypeUniforms(MaterialInstance material, CloudHandle cloudHandle) {

        material.setUniform(EngineSetting.UNIFORM_CLOUD_COLOR, cloudHandle.getCloudColor());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_SCALE, cloudHandle.getScale());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_VERTICAL_THICKNESS, cloudHandle.getVerticalThickness());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_DENSITY, cloudHandle.getDensity());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_TOP_COLOR, cloudHandle.getTopColor());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_TOON_BANDS, cloudHandle.getToonBands());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_DENSITY_NOISE_SCALE, cloudHandle.getDensityNoiseScale());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_NOISE_WARP_STRENGTH, cloudHandle.getNoiseWarpStrength());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_COVERAGE_BIAS, cloudHandle.getCoverageBias());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_SILHOUETTE_SOFTNESS, cloudHandle.getSilhouetteSoftness());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_SHADOW_COLOR, cloudHandle.getShadowColor());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_SHADE_STRENGTH, cloudHandle.getShadeStrength());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_RIM_LIGHT_STRENGTH, cloudHandle.getRimLightStrength());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_AMBIENT_OCCLUSION_STRENGTH,
                cloudHandle.getAmbientOcclusionStrength());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_BRIGHTNESS_MULTIPLIER,
                cloudHandle.getBrightnessMultiplier());
    }

    // Submit \\

    /*
     * Pushes every non-empty archetype buffer into this window's render
     * queue for the given target fbo. Safe to call once per active grid
     * window each frame — an archetype with no active cell this frame is
     * skipped entirely.
     */
    void submit(FboInstance fbo, WindowInstance window) {

        for (var entry : cloud2Buffer.object2ObjectEntrySet()) {

            CompositeBufferInstance buffer = entry.getValue();

            if (buffer.isEmpty())
                continue;

            MaterialInstance material = cloud2Material.get(entry.getKey());
            renderManager.pushInstancedCompositeCall(buffer, material, fbo, window);
        }
    }
}