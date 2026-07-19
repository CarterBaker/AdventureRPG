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
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class CloudRenderSystem extends SystemPackage {

    /*
     * Owns one instanced GPU buffer per cloud archetype — every active,
     * cloud-bearing overhead cell sharing a CloudHandle draws in a single
     * instanced call against a single shared MaterialInstance for that
     * archetype. Rebuilt fully every frame from OverheadManager's active
     * cells, farthest-first, so overlapping translucent boxes blend
     * back-to-front with no per-pixel sorting needed — the same technique
     * SkyWeatherPatternBranch uses for the sky dome's own array. Archetype
     * draw order is likewise sorted by each archetype's own average
     * distance this frame. Owned by WeatherRenderSystem.
     */

    private static final int[] INSTANCE_ATTR_SIZES = { 4, 4, 1 };
    private static final int MAX_TRACKED_CELLS = EngineSetting.WEATHER_PATTERN_OVERHEAD_LOBE_BUDGET;
    private static final int MAX_ARCHETYPES = 32;

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

    // Distance Tracking — drives both within-archetype and cross-archetype
    // back-to-front draw order.
    private Object2FloatOpenHashMap<CloudHandle> cloud2DistanceSqSum;
    private Object2IntOpenHashMap<CloudHandle> cloud2SampleCount;
    private Object2FloatOpenHashMap<CloudHandle> cloud2AverageDistanceSq;

    // Sort Scratch
    private final OverheadCellStruct[] sortScratchCells = new OverheadCellStruct[MAX_TRACKED_CELLS];
    private final float[] sortScratchDistanceSq = new float[MAX_TRACKED_CELLS];
    private final CloudHandle[] archetypeOrderScratch = new CloudHandle[MAX_ARCHETYPES];
    private final float[] archetypeDistanceScratch = new float[MAX_ARCHETYPES];

    private final float[] instanceScratch = new float[9];

    @Override
    protected void create() {

        this.cloud2Buffer = new Object2ObjectOpenHashMap<>();
        this.cloud2Material = new Object2ObjectOpenHashMap<>();

        this.cloud2DistanceSqSum = new Object2FloatOpenHashMap<>();
        this.cloud2DistanceSqSum.defaultReturnValue(0f);
        this.cloud2SampleCount = new Object2IntOpenHashMap<>();
        this.cloud2SampleCount.defaultReturnValue(0);
        this.cloud2AverageDistanceSq = new Object2FloatOpenHashMap<>();
        this.cloud2AverageDistanceSq.defaultReturnValue(0f);
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
     * Pushed once at bootstrap. Real cloud objects are capped to exactly
     * WeatherManager.getEffectiveNearRangeChunks() — the same
     * min(Settings.maxRenderDistance / 2, WEATHER_NEAR_RANGE_CHUNKS) formula
     * RegionSampleBranch and SkyWeatherPatternBranch's own horizon ring both
     * key off — read through that one shared method rather than recomputed
     * here, so the overhead system's streaming boundary and the sky dome's
     * ring can never drift apart from each other.
     */
    private void pushCloudSettings() {

        UBOHandle cloudSettingsData = uboManager.getUBOHandleFromUBOName(EngineSetting.CLOUD_SETTINGS_DATA_UBO);

        float cloudObjectRangeChunks = weatherManager.getEffectiveNearRangeChunks();

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

    void updateInstances() {

        for (CompositeBufferInstance buffer : cloud2Buffer.values())
            buffer.clear();

        cloud2DistanceSqSum.clear();
        cloud2SampleCount.clear();

        long referenceCoordinate = weatherManager.getReferenceCoordinate();
        int referenceChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int referenceChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        WorldHandle activeWorld = worldManager.getActiveWorld();

        int count = gatherSortedCells(referenceChunkX, referenceChunkZ, activeWorld);

        for (int i = 0; i < count; i++)
            accumulateInstance(sortScratchCells[i], referenceChunkX, referenceChunkZ, activeWorld);

        finalizeArchetypeDistances();
    }

    /*
     * Collects every cloud-bearing active cell and sorts far-to-near by
     * distance from the reference coordinate — insertion sort, allocation
     * free, trivial at the 64-cell budget cap.
     */
    private int gatherSortedCells(int referenceChunkX, int referenceChunkZ, WorldHandle activeWorld) {

        int count = 0;
        float nearRangeChunks = weatherManager.getEffectiveNearRangeChunks();
        float nearRangeChunksSq = nearRangeChunks * nearRangeChunks;

        for (OverheadCellStruct cell : overheadManager.getActiveCells().values()) {

            if (!cell.hasCloud() || count >= sortScratchCells.length)
                continue;

            double relativeChunkX = WorldWrapUtility.wrappedDeltaX(activeWorld, cell.getCurrentChunkX(),
                    referenceChunkX);
            double relativeChunkZ = WorldWrapUtility.wrappedDeltaZ(activeWorld, cell.getCurrentChunkZ(),
                    referenceChunkZ);

            float distanceSq = (float) (relativeChunkX * relativeChunkX + relativeChunkZ * relativeChunkZ);

            if (distanceSq > nearRangeChunksSq)
                continue;

            sortScratchCells[count] = cell;
            sortScratchDistanceSq[count] = distanceSq;
            count++;
        }

        for (int i = 1; i < count; i++) {

            OverheadCellStruct cell = sortScratchCells[i];
            float distanceSq = sortScratchDistanceSq[i];

            int j = i - 1;
            while (j >= 0 && sortScratchDistanceSq[j] < distanceSq) {
                sortScratchCells[j + 1] = sortScratchCells[j];
                sortScratchDistanceSq[j + 1] = sortScratchDistanceSq[j];
                j--;
            }

            sortScratchCells[j + 1] = cell;
            sortScratchDistanceSq[j + 1] = distanceSq;
        }

        return count;
    }

    private void accumulateInstance(
            OverheadCellStruct cell,
            int referenceChunkX,
            int referenceChunkZ,
            WorldHandle activeWorld) {

        CloudHandle cloudHandle = cell.getCloudHandle();
        CompositeBufferInstance buffer = getOrCreateArchetype(cloudHandle);

        double relativeChunkX = WorldWrapUtility.wrappedDeltaX(activeWorld, cell.getCurrentChunkX(), referenceChunkX);
        double relativeChunkZ = WorldWrapUtility.wrappedDeltaZ(activeWorld, cell.getCurrentChunkZ(), referenceChunkZ);

        float distanceSq = (float) (relativeChunkX * relativeChunkX + relativeChunkZ * relativeChunkZ);
        cloud2DistanceSqSum.addTo(cloudHandle, distanceSq);
        cloud2SampleCount.addTo(cloudHandle, 1);

        instanceScratch[0] = (float) (relativeChunkX * EngineSetting.CHUNK_SIZE);
        instanceScratch[1] = cell.getEffectiveAltitude();
        instanceScratch[2] = (float) (relativeChunkZ * EngineSetting.CHUNK_SIZE);
        instanceScratch[3] = cell.getRandomSeed();
        instanceScratch[4] = cell.getDomainRotation();
        instanceScratch[5] = cell.getFadeAlpha();
        instanceScratch[6] = cell.getIntensity();
        instanceScratch[7] = cell.getSizeVariance();
        instanceScratch[8] = cell.getElongation();

        buffer.addInstance(instanceScratch);
    }

    private void finalizeArchetypeDistances() {

        cloud2AverageDistanceSq.clear();

        for (var entry : cloud2DistanceSqSum.object2FloatEntrySet()) {
            int sampleCount = cloud2SampleCount.getInt(entry.getKey());
            if (sampleCount > 0)
                cloud2AverageDistanceSq.put(entry.getKey(), entry.getFloatValue() / sampleCount);
        }
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
     * Bakes every archetype-level SHAPE value off CloudData into this
     * archetype's own shared MaterialInstance, once, the first time it is
     * ever used. Every one of these maps directly to a CloudData field —
     * there is no lighting here; lighting is computed identically for
     * every archetype in the shader (real sun/moon response via the
     * shared deferred pass) rather than being an adjustable material knob.
     * Per-instance size variance and elongation are applied later, in the
     * vertex shader, against this shared base.
     */
    private void bakeArchetypeUniforms(MaterialInstance material, CloudHandle cloudHandle) {

        material.setUniform(EngineSetting.UNIFORM_CLOUD_COLOR, cloudHandle.getCloudColor());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_SCALE, cloudHandle.getScale());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_VERTICAL_THICKNESS, cloudHandle.getVerticalThickness());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_DENSITY, cloudHandle.getDensity());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_DENSITY_NOISE_SCALE, cloudHandle.getDensityNoiseScale());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_NOISE_WARP_STRENGTH, cloudHandle.getNoiseWarpStrength());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_COVERAGE_BIAS, cloudHandle.getCoverageBias());
        material.setUniform(EngineSetting.UNIFORM_CLOUD_SILHOUETTE_SOFTNESS, cloudHandle.getSilhouetteSoftness());
    }

    // Submit \\

    /*
     * Pushes every non-empty archetype buffer into this window's render
     * queue, ordered farthest-average-distance first so different cloud
     * types layered over each other this frame also composite back-to-front.
     */
    void submit(FboInstance fbo, WindowInstance window) {

        int archetypeCount = 0;

        for (var entry : cloud2Buffer.object2ObjectEntrySet()) {

            if (entry.getValue().isEmpty())
                continue;

            if (archetypeCount >= archetypeOrderScratch.length)
                break;

            archetypeOrderScratch[archetypeCount] = entry.getKey();
            archetypeDistanceScratch[archetypeCount] = cloud2AverageDistanceSq.getFloat(entry.getKey());
            archetypeCount++;
        }

        for (int i = 1; i < archetypeCount; i++) {

            CloudHandle handle = archetypeOrderScratch[i];
            float distanceSq = archetypeDistanceScratch[i];

            int j = i - 1;
            while (j >= 0 && archetypeDistanceScratch[j] < distanceSq) {
                archetypeOrderScratch[j + 1] = archetypeOrderScratch[j];
                archetypeDistanceScratch[j + 1] = archetypeDistanceScratch[j];
                j--;
            }

            archetypeOrderScratch[j + 1] = handle;
            archetypeDistanceScratch[j + 1] = distanceSq;
        }

        for (int i = 0; i < archetypeCount; i++) {
            CloudHandle handle = archetypeOrderScratch[i];
            CompositeBufferInstance buffer = cloud2Buffer.get(handle);
            MaterialInstance material = cloud2Material.get(handle);
            renderManager.pushInstancedCompositeCall(buffer, material, fbo, window);
        }
    }
}