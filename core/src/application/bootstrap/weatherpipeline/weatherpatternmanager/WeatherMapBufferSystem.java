package application.bootstrap.weatherpipeline.weatherpatternmanager;

import java.util.Arrays;

import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
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
     * Flattens the shared active weather-pattern pool into each active
     * grid's own WeatherMapData UBO every frame. One array slot is written
     * per (pattern, cloud entry) pair, nearest-first RELATIVE TO THAT GRID —
     * two players standing far enough apart need different distances, a
     * different near/far split, and possibly different entries entirely, so
     * every grid gets its own sort, its own entry count, and its own near-
     * range count rather than sharing one global view. The overhead
     * volumetric render system draws its box mesh instanced exactly this
     * grid's own near-range-entry-count times and indexes gl_InstanceID
     * directly into this same grid's UBO slots, so the leading N entries
     * must always be exactly that grid's near-range entries. The static
     * near/outer sampling ranges are seeded once per grid at grid creation
     * (see GridBuildSystem) — only the per-frame entry data is written here.
     */

    private static final long RENDER_SEED_MIX = 0x94D049BB133111EBL;

    // Internal
    private WeatherPatternManager weatherPatternManager;
    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;
    private WorldManager worldManager;

    // Scratch — sized once at create(), mutated in place every frame
    private Vector4[] bounds;
    private Vector4[] patternState;
    private Vector4[] cloudColorScale;
    private Vector4[] cloudMaterial;
    private Vector4[] cloudShape;
    private Vector4[] cloudNoise;
    private Vector4[] cloudVariance0;
    private Vector4[] cloudVariance1;

    // Per-grid sort scratch — cleared and refilled for every grid, every frame
    private ObjectArrayList<WeatherPatternStruct> sortScratch;
    private float[] distanceScratch;
    private Integer[] indexScratch;

    // Internal \\

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

        int patternCapacity = EngineSetting.WEATHER_PATTERN_MAX_ACTIVE_COUNT;

        this.sortScratch = new ObjectArrayList<>(patternCapacity);
        this.distanceScratch = new float[patternCapacity];
        this.indexScratch = new Integer[patternCapacity];
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

    // Flatten \\

    private void writeEntriesForGrid(GridInstance grid) {

        long referenceCoordinate = grid.getActiveChunkCoordinate();
        int refChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int refChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        float outerRangeChunks = weatherPatternManager.getOuterRangeChunks();
        float nearRangeChunks = weatherPatternManager.getNearRangeChunks();

        sortScratch.clear();

        for (WeatherPatternStruct pattern : weatherPatternManager.getActivePatterns().values()) {

            double dx = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkX(), refChunkX, worldWidthChunks);
            double dz = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkZ(), refChunkZ, worldHeightChunks);
            float distanceChunks = (float) Math.sqrt(dx * dx + dz * dz);

            if (distanceChunks > outerRangeChunks)
                continue;

            int slot = sortScratch.size();
            sortScratch.add(pattern);
            distanceScratch[slot] = distanceChunks;
            indexScratch[slot] = slot;
        }

        int patternCount = sortScratch.size();

        Arrays.sort(indexScratch, 0, patternCount, (a, b) -> Float.compare(distanceScratch[a], distanceScratch[b]));

        int capacity = EngineSetting.WEATHER_MAP_UBO_MAX_ENTRIES;
        int entryCount = 0;
        int resolvedNearRangeCount = -1;

        for (int i = 0; i < patternCount && entryCount < capacity; i++) {

            int sortedIndex = indexScratch[i];
            WeatherPatternStruct pattern = sortScratch.get(sortedIndex);
            float distanceChunks = distanceScratch[sortedIndex];

            // Sorted ascending, so the first pattern beyond the near range
            // marks the boundary — every entry after it is guaranteed to be
            // at least as far, so entryCount right now is exactly how many
            // leading slots this grid's overhead box mesh should instance
            // against.
            if (resolvedNearRangeCount < 0 && distanceChunks > nearRangeChunks)
                resolvedNearRangeCount = entryCount;

            WeatherHandle weatherHandle = pattern.getWeatherHandle();
            ObjectArrayList<CloudChanceStruct> cloudEntries = weatherHandle.getCloudEntries();

            for (int c = 0; c < cloudEntries.size() && entryCount < capacity; c++) {
                writeEntry(entryCount, pattern, distanceChunks, weatherHandle, cloudEntries.get(c));
                entryCount++;
            }
        }

        grid.setWeatherMapNearRangeCount(resolvedNearRangeCount < 0 ? entryCount : resolvedNearRangeCount);

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

        uboManager.push(weatherMapUBO);
    }

    private void writeEntry(
            int index,
            WeatherPatternStruct pattern,
            float distanceChunks,
            WeatherHandle weatherHandle,
            CloudChanceStruct cloudEntry) {

        Vector4 patternBounds = pattern.getBounds();
        bounds[index].set(patternBounds.x, patternBounds.y, patternBounds.z, patternBounds.w);

        patternState[index].set(
                distanceChunks,
                pattern.getIntensity(),
                pattern.getSpread(),
                pattern.getFadeAlpha());

        var cloudHandle = cloudEntry.getCloudHandle();
        var color = cloudHandle.getCloudColor();

        cloudColorScale[index].set(color.x, color.y, color.z, cloudHandle.getScale());

        cloudMaterial[index].set(cloudHandle.getSaturation(), cloudHandle.getFullness(), 0f, 0f);

        float resolvedDensity = cloudHandle.getDensity()
                * weatherHandle.getCloudDensityMultiplier()
                * cloudEntry.getDensityMultiplier();

        cloudShape[index].set(
                cloudHandle.getVerticalThickness(),
                cloudEntry.getEffectiveAltitude(),
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

    // Utility \\

    private static Vector4[] allocate(int size) {
        Vector4[] array = new Vector4[size];
        for (int i = 0; i < size; i++)
            array[i] = new Vector4();
        return array;
    }
}