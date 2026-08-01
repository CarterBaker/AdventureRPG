// WeatherMapBufferSystem.java
package application.bootstrap.weatherpipeline.weatherpatternmanager;

import java.util.Arrays;

import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WeatherMapBufferSystem extends SystemPackage {

    /*
     * Flattens the shared active weather-instance pool into each active
     * grid's own WeatherMapData UBO every frame, culled to that grid's own
     * near range and sorted nearest-first. Culling and the range fade are
     * both keyed off each pattern's footprint edge rather than its center,
     * so a pattern only drops out once its whole visible footprint has
     * actually left near range instead of popping the moment its center
     * crosses the line. Also derives the tight world-space Y band actually
     * occupied by this frame's entries so the fullscreen cloud pass can
     * bound its raymarch to where cloud volume can physically exist instead
     * of the full atmosphere column.
     */

    private static final long RENDER_SEED_MIX = 0x94D049BB133111EBL;
    private static final float RANGE_FADE_CHUNKS = 24f;
    private static final float LAYER_BOUND_MARGIN_BLOCKS = 16f;

    private WeatherPatternManager weatherPatternManager;
    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;
    private WorldManager worldManager;

    private Vector4[] bounds;
    private Vector4[] patternState;
    private Vector4[] cloudColorScale;
    private Vector4[] cloudMaterial;
    private Vector4[] cloudShape;
    private Vector4[] cloudNoise;
    private Vector4[] cloudVariance0;
    private Vector4[] cloudVariance1;

    private long[] sortScratch;

    @Override
    protected void create() {

        int capacity = EngineSetting.WEATHER_MAP_UBO_MAX_ENTRIES;

        this.bounds = allocate(capacity);
        this.patternState = allocate(capacity);
        this.cloudColorScale = allocate(capacity);
        this.cloudMaterial = allocate(capacity);
        this.cloudShape = allocate(capacity);
        this.cloudNoise = allocate(capacity);
        this.cloudVariance0 = allocate(capacity);
        this.cloudVariance1 = allocate(capacity);

        this.sortScratch = new long[EngineSetting.WEATHER_PATTERN_MAX_ACTIVE_COUNT];
    }

    @Override
    protected void get() {
        this.weatherPatternManager = get(WeatherPatternManager.class);
        this.uboManager = get(UBOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.worldManager = get(WorldManager.class);
    }

    @Override
    protected void update() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            writeEntriesForGrid((GridInstance) elements[i]);
    }

    private void writeEntriesForGrid(GridInstance grid) {

        long referenceCoordinate = grid.getActiveChunkCoordinate();
        int refChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int refChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        float nearRangeChunks = weatherPatternManager.getNearRangeChunks();
        float chunkSizeBlocks = EngineSetting.CHUNK_SIZE;

        WeatherInstance[] pool = weatherPatternManager.getPatternPool();
        int patternCount = 0;

        for (int slot = 0; slot < pool.length; slot++) {

            if (!weatherPatternManager.isPatternActive(slot))
                continue;

            WeatherInstance pattern = pool[slot];

            double dx = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkX(), refChunkX, worldWidthChunks);
            double dz = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkZ(), refChunkZ, worldHeightChunks);
            float distanceChunks = (float) Math.sqrt(dx * dx + dz * dz);
            float edgeDistanceChunks = distanceChunks - pattern.getFootprintRadiusChunks();

            if (edgeDistanceChunks > nearRangeChunks)
                continue;

            int distanceBits = Float.floatToRawIntBits(distanceChunks);
            sortScratch[patternCount] = ((long) distanceBits << 32) | (slot & 0xFFFFFFFFL);
            patternCount++;
        }

        Arrays.sort(sortScratch, 0, patternCount);

        int capacity = EngineSetting.WEATHER_MAP_UBO_MAX_ENTRIES;
        int entryCount = 0;

        for (int i = 0; i < patternCount && entryCount < capacity; i++) {

            long packed = sortScratch[i];
            int slot = (int) (packed & 0xFFFFFFFFL);
            float distanceChunks = Float.intBitsToFloat((int) (packed >>> 32));

            WeatherInstance pattern = pool[slot];

            double dx = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkX(), refChunkX, worldWidthChunks);
            double dz = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkZ(), refChunkZ, worldHeightChunks);
            float centerXBlocks = (float) (dx * chunkSizeBlocks);
            float centerZBlocks = (float) (dz * chunkSizeBlocks);
            float footprintRadiusChunks = pattern.getFootprintRadiusChunks();
            float radiusBlocks = footprintRadiusChunks * chunkSizeBlocks;
            float distanceBlocks = distanceChunks * chunkSizeBlocks;
            float rangeFade = computeRangeFade(distanceChunks - footprintRadiusChunks, nearRangeChunks);

            WeatherHandle weatherHandle = pattern.getWeatherHandle();
            int cloudCount = weatherHandle.getCloudCount();

            for (int c = 0; c < cloudCount && entryCount < capacity; c++) {
                writeEntry(entryCount, pattern, centerXBlocks, centerZBlocks, radiusBlocks, distanceBlocks,
                        rangeFade, weatherHandle, c);
                entryCount++;
            }
        }

        UBOInstance weatherMapUBO = grid.getWeatherMapUBO();

        weatherMapUBO.updateUniform("u_weatherBounds", bounds);
        weatherMapUBO.updateUniform("u_weatherPatternState", patternState);
        weatherMapUBO.updateUniform("u_weatherCloudColorScale", cloudColorScale);
        weatherMapUBO.updateUniform("u_weatherCloudMaterial", cloudMaterial);
        weatherMapUBO.updateUniform("u_weatherCloudShape", cloudShape);
        weatherMapUBO.updateUniform("u_weatherCloudNoise", cloudNoise);
        weatherMapUBO.updateUniform("u_weatherCloudVariance0", cloudVariance0);
        weatherMapUBO.updateUniform("u_weatherCloudVariance1", cloudVariance1);
        weatherMapUBO.updateUniform("u_weatherEntryCount", entryCount);

        float layerMinY = 0f;
        float layerMaxY = 0f;

        if (entryCount > 0) {

            layerMinY = Float.MAX_VALUE;
            layerMaxY = -Float.MAX_VALUE;

            for (int i = 0; i < entryCount; i++) {

                float altitude = cloudShape[i].y;
                float halfThickness = cloudShape[i].x * 0.5f;

                layerMinY = Math.min(layerMinY, altitude - halfThickness);
                layerMaxY = Math.max(layerMaxY, altitude + halfThickness);
            }

            layerMinY -= LAYER_BOUND_MARGIN_BLOCKS;
            layerMaxY += LAYER_BOUND_MARGIN_BLOCKS;
        }

        weatherMapUBO.updateUniform("u_weatherCloudLayerMinY", layerMinY);
        weatherMapUBO.updateUniform("u_weatherCloudLayerMaxY", layerMaxY);

        uboManager.push(weatherMapUBO);
    }

    private float computeRangeFade(float distanceChunks, float nearRangeChunks) {

        if (RANGE_FADE_CHUNKS <= 0f)
            return 1f;

        float fadeStartChunks = Math.max(0f, nearRangeChunks - RANGE_FADE_CHUNKS);
        float t = 1f - (distanceChunks - fadeStartChunks) / RANGE_FADE_CHUNKS;
        t = Math.max(0f, Math.min(1f, t));

        return t * t * (3f - 2f * t);
    }

    private void writeEntry(
            int index,
            WeatherInstance pattern,
            float centerXBlocks,
            float centerZBlocks,
            float radiusBlocks,
            float distanceBlocks,
            float rangeFade,
            WeatherHandle weatherHandle,
            int cloudIndex) {

        bounds[index].set(
                centerXBlocks - radiusBlocks,
                centerZBlocks - radiusBlocks,
                centerXBlocks + radiusBlocks,
                centerZBlocks + radiusBlocks);

        patternState[index].set(
                pattern.getIntensity(),
                pattern.getFadeAlpha(),
                distanceBlocks,
                rangeFade);

        CloudHandle cloudHandle = weatherHandle.getCloudHandle(cloudIndex);
        var color = cloudHandle.getCloudColor();

        cloudColorScale[index].set(color.x, color.y, color.z, cloudHandle.getScale());

        cloudMaterial[index].set(cloudHandle.getSaturation(), cloudHandle.getFullness(), 0f, 0f);

        float resolvedDensity = cloudHandle.getDensity()
                * weatherHandle.getCloudDensityMultiplier()
                * weatherHandle.getCloudDensityMultiplier(cloudIndex);

        cloudShape[index].set(
                cloudHandle.getVerticalThickness(),
                weatherHandle.getCloudEffectiveAltitude(cloudIndex),
                resolvedDensity,
                pattern.getDriftSpeedScale() * cloudHandle.getDriftSpeedScale());

        cloudNoise[index].set(
                cloudHandle.getDensityNoiseScale(),
                cloudHandle.getNoiseWarpStrength(),
                cloudHandle.getCoverageBias(),
                cloudHandle.getSilhouetteSoftness());

        cloudVariance0[index].set(
                cloudHandle.getSpreadRatio(),
                cloudHandle.getSizeVarianceMin(),
                cloudHandle.getSizeVarianceMax(),
                cloudHandle.getElongationMin());

        cloudVariance1[index].set(
                cloudHandle.getElongationMax(),
                (float) cloudHandle.getCloudTypeIndex(),
                WeatherPatternManager.hash01(pattern.getPatternKey() ^ RENDER_SEED_MIX),
                0f);
    }

    private static Vector4[] allocate(int size) {
        Vector4[] array = new Vector4[size];
        for (int i = 0; i < size; i++)
            array[i] = new Vector4();
        return array;
    }
}