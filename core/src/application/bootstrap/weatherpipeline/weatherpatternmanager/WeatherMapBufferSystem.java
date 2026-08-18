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
     * Flattens each grid's own local weather instance, plus the shared
     * active weather-instance pool, into that grid's own WeatherMapData UBO
     * every frame. The local instance is always written first, centered
     * exactly on the reference chunk; pool entries follow nearest-first,
     * culled to range. Each cloud slot within a pattern is placed into its
     * own deterministically hashed sub-region and altitude jitter (see
     * writeEntry) so a multi-cloud weather reads as a genuine patchwork of
     * distinct cloud types rather than one uniform layer, and every slot
     * cross-fades between a pattern's previous and current WeatherHandle
     * across that pattern's own eased transitionT. All tuning values and
     * seeds come from EngineSetting — nothing here is a locally authored
     * duplicate.
     */

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

    // Runs in LATE_UPDATE, deliberately after both UPDATE and FIXED_UPDATE
    // have finished for this frame. Player movement is physics-driven and
    // resolved in FIXED_UPDATE, so a grid's own reference chunk coordinate
    // isn't final for the frame until then. Writing these entries any
    // earlier (i.e. from UPDATE, which runs before FIXED_UPDATE) reads a
    // stale reference chunk on exactly the frame the player's position
    // wraps across a chunk boundary — every pattern's centerXBlocks/Z is
    // computed relative to that reference chunk, so a one-frame-stale read
    // shows up as every cloud pattern popping sideways by one full chunk
    // width for that single frame. Nothing else this system depends on
    // (pattern positions, transitions) needs to be any fresher than
    // UPDATE, so only this write moves.
    @Override
    protected void lateUpdate() {

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

        float rangeChunks = weatherPatternManager.getRangeChunks();
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

            if (edgeDistanceChunks > rangeChunks)
                continue;

            int distanceBits = Float.floatToRawIntBits(distanceChunks);
            sortScratch[patternCount] = ((long) distanceBits << 32) | (slot & 0xFFFFFFFFL);
            patternCount++;
        }

        Arrays.sort(sortScratch, 0, patternCount);

        int capacity = EngineSetting.WEATHER_MAP_UBO_MAX_ENTRIES;
        int entryCount = 0;

        WeatherInstance localPattern = grid.getWeatherInstance();

        if (localPattern.isConfigured()) {
            float localRadiusBlocks = localPattern.getFootprintRadiusChunks() * chunkSizeBlocks;
            entryCount = writePatternEntries(
                    entryCount, capacity, localPattern, 0f, 0f, localRadiusBlocks, 0f, 1f);
        }

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
            float rangeFade = computeRangeFade(distanceChunks - footprintRadiusChunks, rangeChunks);

            entryCount = writePatternEntries(
                    entryCount, capacity, pattern, centerXBlocks, centerZBlocks, radiusBlocks, distanceBlocks,
                    rangeFade);
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

            layerMinY -= EngineSetting.WEATHER_MAP_LAYER_BOUND_MARGIN_BLOCKS;
            layerMaxY += EngineSetting.WEATHER_MAP_LAYER_BOUND_MARGIN_BLOCKS;
        }

        weatherMapUBO.updateUniform("u_weatherCloudLayerMinY", layerMinY);
        weatherMapUBO.updateUniform("u_weatherCloudLayerMaxY", layerMaxY);
        weatherMapUBO.updateUniform("u_weatherRangeBlocks", rangeChunks * chunkSizeBlocks);

        uboManager.push(weatherMapUBO);
    }

    private int writePatternEntries(
            int entryCount,
            int capacity,
            WeatherInstance pattern,
            float centerXBlocks,
            float centerZBlocks,
            float radiusBlocks,
            float distanceBlocks,
            float rangeFade) {

        WeatherHandle previousWeatherHandle = pattern.getPreviousWeatherHandle();
        WeatherHandle currentWeatherHandle = pattern.getWeatherHandle();
        float transitionT = pattern.getEasedTransitionT();

        int blendedCloudCount = transitionT >= 1f
                ? currentWeatherHandle.getCloudCount()
                : Math.max(currentWeatherHandle.getCloudCount(), previousWeatherHandle.getCloudCount());

        for (int c = 0; c < blendedCloudCount && entryCount < capacity; c++) {
            writeEntry(entryCount, pattern, centerXBlocks, centerZBlocks, radiusBlocks, distanceBlocks,
                    rangeFade, previousWeatherHandle, currentWeatherHandle, transitionT, c);
            entryCount++;
        }

        return entryCount;
    }

    private float computeRangeFade(float distanceChunks, float rangeChunks) {

        if (EngineSetting.WEATHER_MAP_RANGE_FADE_CHUNKS <= 0f)
            return 1f;

        float fadeStartChunks = Math.max(0f, rangeChunks - EngineSetting.WEATHER_MAP_RANGE_FADE_CHUNKS);
        float t = 1f - (distanceChunks - fadeStartChunks) / EngineSetting.WEATHER_MAP_RANGE_FADE_CHUNKS;
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
            WeatherHandle previousWeatherHandle,
            WeatherHandle currentWeatherHandle,
            float transitionT,
            int cloudIndex) {

        // Sub-region placement — every cloud slot in a multi-cloud weather
        // (see WeatherBuilder's "clouds" list, capped at
        // EngineSetting.MAX_CLOUDS_PER_WEATHER) gets its own patch of sky
        // offset within the pattern's overall footprint instead of painting
        // across the same full footprint as every other slot, plus its own
        // altitude jitter off the archetype's authored baseAltitude — both
        // hashed from this pattern's own key and the slot index, so the
        // same pattern always lays its clouds out identically for every
        // player while different patterns and different slots never match.
        long placementSeed = pattern.getPatternKey()
                ^ (EngineSetting.WEATHER_CLOUD_SUBREGION_SEED_SALT * (cloudIndex + 1));
        float placementAngle = WeatherPatternManager.hash01(placementSeed) * ((float) Math.PI * 2f);
        float placementDistanceT = WeatherPatternManager
                .hash01(placementSeed ^ EngineSetting.WEATHER_HASH_SALT_SECONDARY);
        float altitudeJitterT = WeatherPatternManager.hash01(placementSeed ^ EngineSetting.WEATHER_HASH_SALT_PRIMARY);

        float altitudeJitterBlocks = (altitudeJitterT - 0.5f) * 2f * EngineSetting.WEATHER_CLOUD_ALTITUDE_JITTER_BLOCKS;

        float subRadiusBlocks = radiusBlocks * EngineSetting.WEATHER_CLOUD_SUBREGION_RADIUS_RATIO;
        float subOffsetBlocks = radiusBlocks * EngineSetting.WEATHER_CLOUD_SUBREGION_OFFSET_RATIO * placementDistanceT;
        float subCenterX = centerXBlocks + (float) Math.cos(placementAngle) * subOffsetBlocks;
        float subCenterZ = centerZBlocks + (float) Math.sin(placementAngle) * subOffsetBlocks;

        bounds[index].set(
                subCenterX - subRadiusBlocks,
                subCenterZ - subRadiusBlocks,
                subCenterX + subRadiusBlocks,
                subCenterZ + subRadiusBlocks);

        patternState[index].set(
                pattern.getBlendedCloudCoverage(),
                pattern.getFadeAlpha(),
                distanceBlocks,
                rangeFade);

        boolean hasPrevious = cloudIndex < previousWeatherHandle.getCloudCount();
        boolean hasCurrent = cloudIndex < currentWeatherHandle.getCloudCount();

        float presenceWeight = 1f;
        if (!hasPrevious)
            presenceWeight = transitionT;
        else if (!hasCurrent)
            presenceWeight = 1f - transitionT;

        CloudHandle fromCloud = hasPrevious
                ? previousWeatherHandle.getCloudHandle(cloudIndex)
                : currentWeatherHandle.getCloudHandle(cloudIndex);
        CloudHandle toCloud = hasCurrent
                ? currentWeatherHandle.getCloudHandle(cloudIndex)
                : previousWeatherHandle.getCloudHandle(cloudIndex);

        float fromDensityMultiplier = hasPrevious
                ? previousWeatherHandle.getCloudDensityMultiplier(cloudIndex)
                : currentWeatherHandle.getCloudDensityMultiplier(cloudIndex);
        float toDensityMultiplier = hasCurrent
                ? currentWeatherHandle.getCloudDensityMultiplier(cloudIndex)
                : previousWeatherHandle.getCloudDensityMultiplier(cloudIndex);

        float fromAltitude = hasPrevious
                ? previousWeatherHandle.getCloudEffectiveAltitude(cloudIndex)
                : currentWeatherHandle.getCloudEffectiveAltitude(cloudIndex);
        float toAltitude = hasCurrent
                ? currentWeatherHandle.getCloudEffectiveAltitude(cloudIndex)
                : previousWeatherHandle.getCloudEffectiveAltitude(cloudIndex);

        var fromColor = fromCloud.getCloudColor();
        var toColor = toCloud.getCloudColor();

        float colorR = lerp(fromColor.x, toColor.x, transitionT);
        float colorG = lerp(fromColor.y, toColor.y, transitionT);
        float colorB = lerp(fromColor.z, toColor.z, transitionT);
        float scale = lerp(fromCloud.getScale(), toCloud.getScale(), transitionT);
        float saturation = lerp(fromCloud.getSaturation(), toCloud.getSaturation(), transitionT);
        float fullness = lerp(fromCloud.getFullness(), toCloud.getFullness(), transitionT);
        float verticalThickness = lerp(fromCloud.getVerticalThickness(), toCloud.getVerticalThickness(), transitionT);
        float density = lerp(fromCloud.getDensity(), toCloud.getDensity(), transitionT);
        float densityNoiseScale = lerp(fromCloud.getDensityNoiseScale(), toCloud.getDensityNoiseScale(), transitionT);
        float noiseWarpStrength = lerp(fromCloud.getNoiseWarpStrength(), toCloud.getNoiseWarpStrength(), transitionT);
        float coverageBias = lerp(fromCloud.getCoverageBias(), toCloud.getCoverageBias(), transitionT);
        float silhouetteSoftness = lerp(
                fromCloud.getSilhouetteSoftness(), toCloud.getSilhouetteSoftness(), transitionT);
        float cloudDriftSpeedScale = lerp(fromCloud.getDriftSpeedScale(), toCloud.getDriftSpeedScale(), transitionT);
        float spreadRatio = lerp(fromCloud.getSpreadRatio(), toCloud.getSpreadRatio(), transitionT);
        float sizeVarianceMin = lerp(fromCloud.getSizeVarianceMin(), toCloud.getSizeVarianceMin(), transitionT);
        float sizeVarianceMax = lerp(fromCloud.getSizeVarianceMax(), toCloud.getSizeVarianceMax(), transitionT);
        float elongationMin = lerp(fromCloud.getElongationMin(), toCloud.getElongationMin(), transitionT);
        float elongationMax = lerp(fromCloud.getElongationMax(), toCloud.getElongationMax(), transitionT);
        float altitude = lerp(fromAltitude, toAltitude, transitionT) + altitudeJitterBlocks;
        float perCloudDensityMultiplier = lerp(fromDensityMultiplier, toDensityMultiplier, transitionT);

        float resolvedDensity = density
                * pattern.getBlendedCloudDensityMultiplier()
                * perCloudDensityMultiplier
                * presenceWeight;

        cloudColorScale[index].set(colorR, colorG, colorB, scale);
        cloudMaterial[index].set(saturation, fullness, 0f, 0f);

        cloudShape[index].set(
                verticalThickness,
                altitude,
                resolvedDensity,
                pattern.getDriftSpeedScale() * cloudDriftSpeedScale);

        cloudNoise[index].set(densityNoiseScale, noiseWarpStrength, coverageBias, silhouetteSoftness);

        cloudVariance0[index].set(spreadRatio, sizeVarianceMin, sizeVarianceMax, elongationMin);

        // variance1.y is a per-slot identity (0/1/2), never an interpolated
        // archetype index — the shader hashes it together with patternSeed to
        // pick each puff's orientation/size-within-range. That identity has to
        // stay fixed for the entire lifetime of this cloud slot, including
        // across a weather-type cross-fade, or the hash reshuffles every time
        // its input crosses a fract() seam mid-transition, which reads as the
        // cloud briefly reorienting/resizing itself for no reason.
        cloudVariance1[index].set(
                elongationMax,
                (float) cloudIndex,
                WeatherPatternManager.hash01(pattern.getPatternKey() ^ EngineSetting.WEATHER_MAP_RENDER_SEED_MIX),
                0f);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static Vector4[] allocate(int size) {
        Vector4[] array = new Vector4[size];
        for (int i = 0; i < size; i++)
            array[i] = new Vector4();
        return array;
    }
}