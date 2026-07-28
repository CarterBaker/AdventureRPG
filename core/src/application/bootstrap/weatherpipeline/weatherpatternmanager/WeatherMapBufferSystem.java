package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WeatherMapBufferSystem extends SystemPackage {

    /*
     * Flattens the active weather-pattern set into the shared WeatherMapData
     * UBO every frame. One array slot is written per (pattern, cloud entry)
     * pair, so a pattern suggesting several cloud archetypes occupies several
     * consecutive slots sharing the same bounds/distance but carrying
     * different cloud settings. Patterns are written nearest-first so the
     * fixed-capacity UBO degrades gracefully if the active set ever exceeds
     * it. The near/outer sampling ranges are written once on awake — they
     * never change at runtime, so the per-frame update only ever touches the
     * entry data. Consumed by the sky dome and the overhead volumetric mesh.
     */

    private static final long RENDER_SEED_MIX = 0x94D049BB133111EBL;

    // Internal
    private WeatherPatternManager weatherPatternManager;
    private UBOManager uboManager;

    // GPU
    private UBOHandle weatherMapData;

    // Scratch — sized once at create(), mutated in place every frame
    private Vector4[] bounds;
    private Vector4[] patternState;
    private Vector4[] cloudColorScale;
    private Vector4[] cloudMaterial;
    private Vector4[] cloudShape;
    private Vector4[] cloudNoise;
    private Vector4[] cloudVariance0;
    private Vector4[] cloudVariance1;

    private ObjectArrayList<WeatherPatternStruct> sortScratch;

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

        this.sortScratch = new ObjectArrayList<>(EngineSetting.WEATHER_PATTERN_MAX_ACTIVE_COUNT);
    }

    @Override
    protected void get() {
        this.weatherPatternManager = get(WeatherPatternManager.class);
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {

        this.weatherMapData = uboManager.getUBOHandleFromUBOName(EngineSetting.WEATHER_MAP_UBO);

        weatherMapData.updateUniform("u_weatherOuterRangeChunks", weatherPatternManager.getOuterRangeChunks());
        weatherMapData.updateUniform("u_weatherNearRangeChunks", weatherPatternManager.getNearRangeChunks());

        uboManager.push(weatherMapData);
    }

    @Override
    protected void update() {

        int entryCount = writeEntries();

        weatherMapData.updateUniform("u_weatherBounds", bounds);
        weatherMapData.updateUniform("u_weatherPatternState", patternState);
        weatherMapData.updateUniform("u_weatherCloudColorScale", cloudColorScale);
        weatherMapData.updateUniform("u_weatherCloudMaterial", cloudMaterial);
        weatherMapData.updateUniform("u_weatherCloudShape", cloudShape);
        weatherMapData.updateUniform("u_weatherCloudNoise", cloudNoise);
        weatherMapData.updateUniform("u_weatherCloudVariance0", cloudVariance0);
        weatherMapData.updateUniform("u_weatherCloudVariance1", cloudVariance1);
        weatherMapData.updateUniform("u_weatherEntryCount", entryCount);

        uboManager.push(weatherMapData);
    }

    // Flatten \\

    private int writeEntries() {

        sortScratch.clear();
        sortScratch.addAll(weatherPatternManager.getActivePatterns().values());
        sortScratch.sort((a, b) -> Float.compare(
                a.getDistanceFromReferenceChunks(), b.getDistanceFromReferenceChunks()));

        int capacity = EngineSetting.WEATHER_MAP_UBO_MAX_ENTRIES;
        int entryCount = 0;

        for (int i = 0; i < sortScratch.size() && entryCount < capacity; i++) {

            WeatherPatternStruct pattern = sortScratch.get(i);
            WeatherHandle weatherHandle = pattern.getWeatherHandle();
            ObjectArrayList<CloudChanceStruct> cloudEntries = weatherHandle.getCloudEntries();

            for (int c = 0; c < cloudEntries.size() && entryCount < capacity; c++) {
                writeEntry(entryCount, pattern, weatherHandle, cloudEntries.get(c));
                entryCount++;
            }
        }

        return entryCount;
    }

    private void writeEntry(
            int index,
            WeatherPatternStruct pattern,
            WeatherHandle weatherHandle,
            CloudChanceStruct cloudEntry) {

        Vector4 patternBounds = pattern.getBounds();
        bounds[index].set(patternBounds.x, patternBounds.y, patternBounds.z, patternBounds.w);

        patternState[index].set(
                pattern.getDistanceFromReferenceChunks(),
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