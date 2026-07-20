package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weathermanager.WeatherBandStruct;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector4;

class SkyWeatherPatternBranch extends BranchPackage {

    /*
     * Populates the SkyWeatherPatternData UBO from the weather noise field
     * sampled around a near and far horizon ring. Entries are depth-sorted
     * so the sky dome's front-to-back compositing always draws the closer
     * lobe on top, and every cloud's height in the sky is derived from its
     * altitude against a fixed reference distance rather than from whichever
     * ring supplied it, so a high thin layer always reads above a low thick
     * one regardless of streaming distance.
     */

    private static final int MAX_CLOUDS = EngineSetting.WEATHER_PATTERN_OVERHEAD_LOBE_BUDGET;
    private static final int SLICES_PER_RING = MAX_CLOUDS / 2;
    private static final float ELEVATION_LIMIT_MARGIN_RADIANS = (float) Math.toRadians(8.0);
    private static final long RING_SEED_MIX = 0xA24BAED4963EE407L;

    // Every cloud's altitude is converted to an elevation angle against this
    // fixed distance, then re-projected onto whatever distance its own ring
    // actually renders at — see resolveCenterY().
    private static final float ELEVATION_REFERENCE_DISTANCE_BLOCKS = EngineSetting.WEATHER_FAR_RANGE_CHUNKS
            * EngineSetting.CHUNK_SIZE;

    private WeatherManager weatherManager;
    private UBOManager uboManager;
    private PlayerManager playerManager;
    private WindowManager windowManager;

    private UBOHandle skyWeatherPatternData;

    private final WeatherBandStruct bandScratch = new WeatherBandStruct();

    // UBO Arrays
    private Vector3[] center;
    private Vector3[] halfExtent;
    private Float[] domainRotation;
    private Vector4[] bounds;
    private Float[] distanceFromCenter;
    private Float[] fadeAlpha;
    private Float[] intensity;
    private Vector3[] color;
    private Float[] density;
    private Float[] densityNoiseScale;
    private Float[] noiseWarpStrength;
    private Float[] coverageBias;
    private Float[] silhouetteSoftness;
    private Float[] seed;

    // Sort Scratch
    private final int[] sortRing = new int[MAX_CLOUDS];
    private final int[] sortSlice = new int[MAX_CLOUDS];
    private final CloudChanceStruct[] sortCloudEntry = new CloudChanceStruct[MAX_CLOUDS];
    private final float[] sortDistanceChunks = new float[MAX_CLOUDS];
    private final float[] sortDepthBlocks = new float[MAX_CLOUDS];
    private final float[] sortIntensity = new float[MAX_CLOUDS];
    private final float[] sortDensityMultiplier = new float[MAX_CLOUDS];

    @Override
    protected void create() {

        this.center = newVector3Array();
        this.halfExtent = newVector3Array();
        this.domainRotation = newFloatArray();
        this.bounds = newVector4Array();
        this.distanceFromCenter = newFloatArray();
        this.fadeAlpha = newFloatArray();
        this.intensity = newFloatArray();
        this.color = newVector3Array();
        this.density = newFloatArray();
        this.densityNoiseScale = newFloatArray();
        this.noiseWarpStrength = newFloatArray();
        this.coverageBias = newFloatArray();
        this.silhouetteSoftness = newFloatArray();
        this.seed = newFloatArray();
    }

    @Override
    protected void get() {
        this.weatherManager = get(WeatherManager.class);
        this.uboManager = get(UBOManager.class);
        this.playerManager = get(PlayerManager.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void awake() {
        this.skyWeatherPatternData = uboManager.getUBOHandleFromUBOName(EngineSetting.SKY_WEATHER_PATTERN_DATA_UBO);
    }

    @Override
    protected void update() {
        pushClouds();
    }

    // Push \\

    private void pushClouds() {

        if (!weatherManager.hasActiveWeatherPool()) {
            skyWeatherPatternData.updateUniform("u_cloudCount", 0);
            uboManager.push(skyWeatherPatternData);
            return;
        }

        long referenceCoordinate = weatherManager.getReferenceCoordinate();
        int referenceChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int referenceChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        float nearRangeChunks = weatherManager.getEffectiveNearRangeChunks();
        float farRangeChunks = EngineSetting.WEATHER_FAR_RANGE_CHUNKS;

        float cameraY = resolveCameraY();

        int count = gatherRing(0, 0, nearRangeChunks, referenceChunkX, referenceChunkZ, cameraY);
        count = gatherRing(count, 1, farRangeChunks, referenceChunkX, referenceChunkZ, cameraY);

        sortGatheredNearToFar(count);

        float highestTopElevation = -(float) Math.PI * 0.5f;

        for (int index = 0; index < count; index++) {
            float topElevation = writeCloudEntry(
                    index, sortCloudEntry[index], sortDistanceChunks[index], sortIntensity[index],
                    sortDensityMultiplier[index], sortRing[index], sortSlice[index], cameraY);
            highestTopElevation = Math.max(highestTopElevation, topElevation);
        }

        for (int i = count; i < MAX_CLOUDS; i++)
            fadeAlpha[i] = 0f;

        float fadeStartElevation = highestTopElevation;
        float elevationLimit = Math.min(fadeStartElevation + ELEVATION_LIMIT_MARGIN_RADIANS, (float) Math.PI * 0.5f);

        if (elevationLimit <= fadeStartElevation)
            fadeStartElevation = elevationLimit - 0.001f;

        skyWeatherPatternData.updateUniform("u_cloudCount", count);
        skyWeatherPatternData.updateUniform("u_cloudCenter", center);
        skyWeatherPatternData.updateUniform("u_cloudHalfExtent", halfExtent);
        skyWeatherPatternData.updateUniform("u_cloudDomainRotation", domainRotation);
        skyWeatherPatternData.updateUniform("u_cloudBounds", bounds);
        skyWeatherPatternData.updateUniform("u_cloudDistanceFromCenter", distanceFromCenter);
        skyWeatherPatternData.updateUniform("u_cloudFadeAlpha", fadeAlpha);
        skyWeatherPatternData.updateUniform("u_cloudIntensity", intensity);
        skyWeatherPatternData.updateUniform("u_cloudColor", color);
        skyWeatherPatternData.updateUniform("u_cloudDensity", density);
        skyWeatherPatternData.updateUniform("u_cloudDensityNoiseScale", densityNoiseScale);
        skyWeatherPatternData.updateUniform("u_cloudNoiseWarpStrength", noiseWarpStrength);
        skyWeatherPatternData.updateUniform("u_cloudCoverageBias", coverageBias);
        skyWeatherPatternData.updateUniform("u_cloudSilhouetteSoftness", silhouetteSoftness);
        skyWeatherPatternData.updateUniform("u_cloudSeed", seed);
        skyWeatherPatternData.updateUniform("u_skyElevationFadeStart", (float) Math.sin(fadeStartElevation));
        skyWeatherPatternData.updateUniform("u_skyElevationLimit", (float) Math.sin(elevationLimit));

        uboManager.push(skyWeatherPatternData);
    }

    // Ring Sampling \\

    private int gatherRing(
            int count,
            int ringIndex,
            float ringDistanceChunks,
            int referenceChunkX,
            int referenceChunkZ,
            float cameraY) {

        for (int slice = 0; slice < SLICES_PER_RING && count < MAX_CLOUDS; slice++) {

            long sliceSeed = sliceSeed(ringIndex, slice);
            float angle = sliceAngle(sliceSeed, slice);

            int homeChunkX = referenceChunkX + Math.round((float) Math.cos(angle) * ringDistanceChunks);
            int homeChunkZ = referenceChunkZ + Math.round((float) Math.sin(angle) * ringDistanceChunks);

            long homeCoordinate = Coordinate2Long.pack(homeChunkX, homeChunkZ);
            weatherManager.resolveWeatherBandTowardHorizon(bandScratch, homeCoordinate);

            WeatherHandle weatherHandle = bandScratch.getPrimary();

            if (!weatherHandle.hasClouds())
                continue;

            float spread = bandScratch.getIntensityFor(weatherHandle);
            float sliceIntensity = spread * weatherHandle.getCloudCoverage();

            if (sliceIntensity <= 0.02f)
                continue;

            float cloudPickNoise = hash01(sliceSeed ^ 0xBF58476D1CE4E5B9L);
            CloudChanceStruct cloudEntry = weatherHandle.pickCloud(cloudPickNoise);

            if (cloudEntry == null)
                continue;

            float sizeVariance = resolveSizeVariance(sliceSeed);
            float halfY = cloudEntry.getCloudHandle().getVerticalThickness() * sizeVariance * 0.5f;
            float centerY = resolveCenterY(cloudEntry.getEffectiveAltitude(), halfY, ringDistanceChunks, cameraY);

            float horizontalDistanceBlocks = ringDistanceChunks * EngineSetting.CHUNK_SIZE;
            float verticalDistanceBlocks = centerY - cameraY;
            float trueDistanceBlocks = (float) Math.sqrt(
                    horizontalDistanceBlocks * horizontalDistanceBlocks
                            + verticalDistanceBlocks * verticalDistanceBlocks);

            sortRing[count] = ringIndex;
            sortSlice[count] = slice;
            sortCloudEntry[count] = cloudEntry;
            sortDistanceChunks[count] = ringDistanceChunks;
            sortDepthBlocks[count] = trueDistanceBlocks;
            sortIntensity[count] = sliceIntensity;
            sortDensityMultiplier[count] = weatherHandle.getCloudDensityMultiplier()
                    * cloudEntry.getDensityMultiplier();
            count++;
        }

        return count;
    }

    private void sortGatheredNearToFar(int count) {

        for (int i = 1; i < count; i++) {

            int ring = sortRing[i];
            int slice = sortSlice[i];
            CloudChanceStruct cloudEntry = sortCloudEntry[i];
            float distance = sortDistanceChunks[i];
            float depth = sortDepthBlocks[i];
            float sliceIntensity = sortIntensity[i];
            float densityMultiplier = sortDensityMultiplier[i];

            int j = i - 1;
            while (j >= 0 && sortDepthBlocks[j] > depth) {
                sortRing[j + 1] = sortRing[j];
                sortSlice[j + 1] = sortSlice[j];
                sortCloudEntry[j + 1] = sortCloudEntry[j];
                sortDistanceChunks[j + 1] = sortDistanceChunks[j];
                sortDepthBlocks[j + 1] = sortDepthBlocks[j];
                sortIntensity[j + 1] = sortIntensity[j];
                sortDensityMultiplier[j + 1] = sortDensityMultiplier[j];
                j--;
            }

            sortRing[j + 1] = ring;
            sortSlice[j + 1] = slice;
            sortCloudEntry[j + 1] = cloudEntry;
            sortDistanceChunks[j + 1] = distance;
            sortDepthBlocks[j + 1] = depth;
            sortIntensity[j + 1] = sliceIntensity;
            sortDensityMultiplier[j + 1] = densityMultiplier;
        }
    }

    private float writeCloudEntry(
            int index,
            CloudChanceStruct cloudEntry,
            float distanceChunks,
            float sliceIntensity,
            float densityMultiplier,
            int ringIndex,
            int slice,
            float cameraY) {

        CloudHandle cloud = cloudEntry.getCloudHandle();

        long sliceSeed = sliceSeed(ringIndex, slice);
        float angle = sliceAngle(sliceSeed, slice);

        float centerXBlocks = (float) Math.cos(angle) * distanceChunks * EngineSetting.CHUNK_SIZE;
        float centerZBlocks = (float) Math.sin(angle) * distanceChunks * EngineSetting.CHUNK_SIZE;

        float angularSliceRadians = (float) (Math.PI * 2.0 / SLICES_PER_RING);
        float halfX = Math.max(distanceChunks * EngineSetting.CHUNK_SIZE * angularSliceRadians * 0.65f, 4f);
        float halfZ = Math.max(distanceChunks * EngineSetting.CHUNK_SIZE * 0.12f, 4f);

        float sizeVariance = resolveSizeVariance(sliceSeed);
        float verticalThickness = cloud.getVerticalThickness() * sizeVariance;
        float halfY = verticalThickness * 0.5f;

        float baseAltitude = cloudEntry.getEffectiveAltitude();
        float centerY = resolveCenterY(baseAltitude, halfY, distanceChunks, cameraY);

        center[index].set(centerXBlocks, centerY, centerZBlocks);
        halfExtent[index].set(halfX, halfY, halfZ);
        domainRotation[index] = angle + (float) (Math.PI * 0.5);

        bounds[index].set(
                centerXBlocks - halfX, centerZBlocks - halfZ,
                centerXBlocks + halfX, centerZBlocks + halfZ);

        float distanceBlocks = distanceChunks * EngineSetting.CHUNK_SIZE;
        distanceFromCenter[index] = distanceBlocks;

        fadeAlpha[index] = clamp01(sliceIntensity);
        intensity[index] = sliceIntensity;

        color[index].set(cloud.getCloudColor());
        density[index] = cloud.getDensity() * densityMultiplier;
        densityNoiseScale[index] = cloud.getDensityNoiseScale();
        noiseWarpStrength[index] = cloud.getNoiseWarpStrength();
        coverageBias[index] = cloud.getCoverageBias();
        silhouetteSoftness[index] = cloud.getSilhouetteSoftness();
        seed[index] = hash01(sliceSeed ^ 0xD1B54A32D192ED03L);

        float topWorldY = centerY + halfY;
        float distanceForElevation = Math.max(distanceBlocks, 1f);

        return (float) Math.atan2(topWorldY - cameraY, distanceForElevation);
    }

    /*
     * Resolves a cloud's altitude to an elevation angle against the fixed
     * reference distance, then re-projects that same angle onto the distance
     * this cloud is actually rendered at. Two clouds sharing an altitude end
     * up at the same apparent height in the sky this way, regardless of
     * whether one was sampled from the near ring and the other from the far
     * ring.
     */
    private float resolveCenterY(float baseAltitude, float halfY, float ringDistanceChunks, float cameraY) {

        float altitudeDiff = (baseAltitude + halfY) - cameraY;
        float elevationAngle = (float) Math.atan2(altitudeDiff, ELEVATION_REFERENCE_DISTANCE_BLOCKS);

        float distanceBlocks = Math.max(ringDistanceChunks * EngineSetting.CHUNK_SIZE, 1f);

        return cameraY + (float) Math.tan(elevationAngle) * distanceBlocks;
    }

    // Slice Math \\

    private long sliceSeed(int ringIndex, int slice) {
        return ((long) ringIndex << 32) ^ ((long) slice * 0x9E3779B97F4A7C15L) ^ RING_SEED_MIX;
    }

    private float sliceAngle(long sliceSeed, int slice) {
        return (slice + hash01(sliceSeed ^ 0x2545F4914F6CDD1DL) * 0.5f)
                * (float) (Math.PI * 2.0 / SLICES_PER_RING);
    }

    private float resolveSizeVariance(long sliceSeed) {
        return 0.85f + hash01(sliceSeed ^ 0x94D049BB133111EBL) * 0.4f;
    }

    private static float hash01(long seed) {

        long h = seed;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);

        return (float) ((h >>> 11) / (double) (1L << 53));
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private Float[] newFloatArray() {
        Float[] array = new Float[MAX_CLOUDS];
        java.util.Arrays.fill(array, 0f);
        return array;
    }

    private Vector3[] newVector3Array() {
        Vector3[] array = new Vector3[MAX_CLOUDS];
        for (int i = 0; i < MAX_CLOUDS; i++)
            array[i] = new Vector3();
        return array;
    }

    private Vector4[] newVector4Array() {
        Vector4[] array = new Vector4[MAX_CLOUDS];
        for (int i = 0; i < MAX_CLOUDS; i++)
            array[i] = new Vector4();
        return array;
    }

    private float resolveCameraY() {

        WindowInstance mainWindow = windowManager.getMainWindow();

        if (mainWindow == null)
            return 0f;

        int windowID = mainWindow.getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return 0f;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);

        return player.getWorldPositionStruct().getPosition().y;
    }
}