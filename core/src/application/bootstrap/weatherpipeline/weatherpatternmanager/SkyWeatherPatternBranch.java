package application.bootstrap.weatherpipeline.weatherpatternmanager;

import java.util.Arrays;

import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/*
 * Flattens WeatherPatternManager's active patterns into the
 * SkyWeatherPatternData UBO every frame — one entry per pattern, carrying
 * its bearing and angular width as seen from the reference coordinate,
 * its own fade-in/out state, and its resolved primary cloud's shading
 * surface. A pattern whose weather defines no cloud archetype writes a
 * zero density and coverage bias, so it never paints a shape of its own —
 * it can only ever thin out real clouds nearby, never fake one.
 */
class SkyWeatherPatternBranch extends BranchPackage {

    private static final int MAX_PATTERNS = EngineSetting.WEATHER_PATTERN_MAX_ACTIVE_COUNT;
    private static final float FOOTPRINT_RADIUS_CHUNKS = EngineSetting.WEATHER_PATTERN_CELL_SIZE_CHUNKS * 0.5f;

    private WeatherPatternManager weatherPatternManager;
    private WeatherManager weatherManager;
    private WorldManager worldManager;
    private UBOManager uboManager;

    private UBOHandle skyWeatherPatternData;

    private Float[] bearing;
    private Float[] angularWidth;
    private Float[] fadeAlpha;
    private Float[] intensity;
    private Float[] coverage;
    private Float[] altitude;
    private Vector3[] color;
    private Vector3[] topColor;
    private Vector3[] shadowColor;
    private Float[] density;
    private Float[] shadeStrength;
    private Float[] rimLightStrength;
    private Float[] ambientOcclusionStrength;
    private Float[] brightnessMultiplier;
    private Float[] toonBands;
    private Float[] densityNoiseScale;
    private Float[] noiseWarpStrength;
    private Float[] coverageBias;
    private Float[] silhouetteSoftness;

    @Override
    protected void create() {
        this.bearing = newFloatArray();
        this.angularWidth = newFloatArray();
        this.fadeAlpha = newFloatArray();
        this.intensity = newFloatArray();
        this.coverage = newFloatArray();
        this.altitude = newFloatArray();
        this.color = newVector3Array();
        this.topColor = newVector3Array();
        this.shadowColor = newVector3Array();
        this.density = newFloatArray();
        this.shadeStrength = newFloatArray();
        this.rimLightStrength = newFloatArray();
        this.ambientOcclusionStrength = newFloatArray();
        this.brightnessMultiplier = newFloatArray();
        this.toonBands = newFloatArray();
        this.densityNoiseScale = newFloatArray();
        this.noiseWarpStrength = newFloatArray();
        this.coverageBias = newFloatArray();
        this.silhouetteSoftness = newFloatArray();
    }

    @Override
    protected void get() {
        this.weatherPatternManager = get(WeatherPatternManager.class);
        this.weatherManager = get(WeatherManager.class);
        this.worldManager = get(WorldManager.class);
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        this.skyWeatherPatternData = uboManager.getUBOHandleFromUBOName(EngineSetting.SKY_WEATHER_PATTERN_DATA_UBO);
    }

    @Override
    protected void update() {
        pushPatterns();
    }

    private void pushPatterns() {

        long referenceCoordinate = weatherManager.getReferenceCoordinate();
        int referenceChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int referenceChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        Long2ObjectOpenHashMap<WeatherPatternStruct> activePatterns = weatherPatternManager.getActivePatterns();

        int count = 0;

        for (WeatherPatternStruct pattern : activePatterns.values()) {

            if (count >= MAX_PATTERNS)
                break;

            writeEntry(count, pattern, referenceChunkX, referenceChunkZ, worldWidthChunks, worldHeightChunks);
            count++;
        }

        skyWeatherPatternData.updateUniform("u_patternCount", count);
        skyWeatherPatternData.updateUniform("u_patternBearing", bearing);
        skyWeatherPatternData.updateUniform("u_patternAngularWidth", angularWidth);
        skyWeatherPatternData.updateUniform("u_patternFadeAlpha", fadeAlpha);
        skyWeatherPatternData.updateUniform("u_patternIntensity", intensity);
        skyWeatherPatternData.updateUniform("u_patternCoverage", coverage);
        skyWeatherPatternData.updateUniform("u_patternAltitude", altitude);
        skyWeatherPatternData.updateUniform("u_patternColor", color);
        skyWeatherPatternData.updateUniform("u_patternTopColor", topColor);
        skyWeatherPatternData.updateUniform("u_patternShadowColor", shadowColor);
        skyWeatherPatternData.updateUniform("u_patternDensity", density);
        skyWeatherPatternData.updateUniform("u_patternShadeStrength", shadeStrength);
        skyWeatherPatternData.updateUniform("u_patternRimLightStrength", rimLightStrength);
        skyWeatherPatternData.updateUniform("u_patternAmbientOcclusionStrength", ambientOcclusionStrength);
        skyWeatherPatternData.updateUniform("u_patternBrightnessMultiplier", brightnessMultiplier);
        skyWeatherPatternData.updateUniform("u_patternToonBands", toonBands);
        skyWeatherPatternData.updateUniform("u_patternDensityNoiseScale", densityNoiseScale);
        skyWeatherPatternData.updateUniform("u_patternNoiseWarpStrength", noiseWarpStrength);
        skyWeatherPatternData.updateUniform("u_patternCoverageBias", coverageBias);
        skyWeatherPatternData.updateUniform("u_patternSilhouetteSoftness", silhouetteSoftness);

        uboManager.push(skyWeatherPatternData);
    }

    private void writeEntry(
            int index,
            WeatherPatternStruct pattern,
            int referenceChunkX,
            int referenceChunkZ,
            int worldWidthChunks,
            int worldHeightChunks) {

        double dx = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkX(), referenceChunkX, worldWidthChunks);
        double dz = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkZ(), referenceChunkZ, worldHeightChunks);
        double distanceChunks = Math.sqrt(dx * dx + dz * dz);

        bearing[index] = (float) Math.atan2(dx, -dz);
        angularWidth[index] = (float) Math.atan2(FOOTPRINT_RADIUS_CHUNKS, Math.max(distanceChunks, 0.001));
        fadeAlpha[index] = pattern.getFadeAlpha();
        intensity[index] = pattern.getIntensity();

        WeatherHandle weatherHandle = pattern.getWeatherHandle();
        CloudChanceStruct primaryCloud = weatherHandle.getPrimaryCloud();
        boolean hasCloud = primaryCloud != null;

        coverage[index] = weatherHandle.getCloudCoverage();
        altitude[index] = resolveAltitude(primaryCloud);

        color[index].set(resolveColorR(primaryCloud), resolveColorG(primaryCloud), resolveColorB(primaryCloud));
        topColor[index].set(
                resolveTopColorR(primaryCloud), resolveTopColorG(primaryCloud), resolveTopColorB(primaryCloud));
        shadowColor[index].set(
                resolveShadowColorR(primaryCloud), resolveShadowColorG(primaryCloud),
                resolveShadowColorB(primaryCloud));

        density[index] = hasCloud ? resolveDensity(primaryCloud) : 0f;
        shadeStrength[index] = resolveShadeStrength(primaryCloud);
        rimLightStrength[index] = resolveRimLightStrength(primaryCloud);
        ambientOcclusionStrength[index] = resolveAmbientOcclusionStrength(primaryCloud);
        brightnessMultiplier[index] = resolveBrightnessMultiplier(primaryCloud);
        toonBands[index] = resolveToonBands(primaryCloud);
        densityNoiseScale[index] = resolveDensityNoiseScale(primaryCloud);
        noiseWarpStrength[index] = resolveNoiseWarpStrength(primaryCloud);
        coverageBias[index] = hasCloud ? resolveCoverageBias(primaryCloud) : 0f;
        silhouetteSoftness[index] = resolveSilhouetteSoftness(primaryCloud);
    }

    private Float[] newFloatArray() {
        Float[] array = new Float[MAX_PATTERNS];
        Arrays.fill(array, 0f);
        return array;
    }

    private Vector3[] newVector3Array() {
        Vector3[] array = new Vector3[MAX_PATTERNS];
        for (int i = 0; i < MAX_PATTERNS; i++)
            array[i] = new Vector3();
        return array;
    }

    private float resolveAltitude(CloudChanceStruct cloud) {
        return cloud != null ? cloud.getEffectiveAltitude() : EngineSetting.CLOUD_DEFAULT_SKY_ALTITUDE;
    }

    private float resolveColorR(CloudChanceStruct cloud) {
        return cloud != null ? cloud.getCloudHandle().getCloudColor().x : EngineSetting.CLOUD_DEFAULT_SKY_COLOR_R;
    }

    private float resolveColorG(CloudChanceStruct cloud) {
        return cloud != null ? cloud.getCloudHandle().getCloudColor().y : EngineSetting.CLOUD_DEFAULT_SKY_COLOR_G;
    }

    private float resolveColorB(CloudChanceStruct cloud) {
        return cloud != null ? cloud.getCloudHandle().getCloudColor().z : EngineSetting.CLOUD_DEFAULT_SKY_COLOR_B;
    }

    private float resolveTopColorR(CloudChanceStruct cloud) {
        return cloud != null ? cloud.getCloudHandle().getTopColor().x : EngineSetting.CLOUD_DEFAULT_SKY_TOP_COLOR_R;
    }

    private float resolveTopColorG(CloudChanceStruct cloud) {
        return cloud != null ? cloud.getCloudHandle().getTopColor().y : EngineSetting.CLOUD_DEFAULT_SKY_TOP_COLOR_G;
    }

    private float resolveTopColorB(CloudChanceStruct cloud) {
        return cloud != null ? cloud.getCloudHandle().getTopColor().z : EngineSetting.CLOUD_DEFAULT_SKY_TOP_COLOR_B;
    }

    private float resolveShadowColorR(CloudChanceStruct cloud) {
        return cloud != null ? cloud.getCloudHandle().getShadowColor().x : EngineSetting.CLOUD_DEFAULT_SHADOW_COLOR_R;
    }

    private float resolveShadowColorG(CloudChanceStruct cloud) {
        return cloud != null ? cloud.getCloudHandle().getShadowColor().y : EngineSetting.CLOUD_DEFAULT_SHADOW_COLOR_G;
    }

    private float resolveShadowColorB(CloudChanceStruct cloud) {
        return cloud != null ? cloud.getCloudHandle().getShadowColor().z : EngineSetting.CLOUD_DEFAULT_SHADOW_COLOR_B;
    }

    private float resolveDensity(CloudChanceStruct cloud) {
        return cloud.getCloudHandle().getDensity();
    }

    private float resolveShadeStrength(CloudChanceStruct cloud) {
        return cloud != null ? cloud.getCloudHandle().getShadeStrength() : EngineSetting.CLOUD_DEFAULT_SHADE_STRENGTH;
    }

    private float resolveRimLightStrength(CloudChanceStruct cloud) {
        return cloud != null
                ? cloud.getCloudHandle().getRimLightStrength()
                : EngineSetting.CLOUD_DEFAULT_RIM_LIGHT_STRENGTH;
    }

    private float resolveAmbientOcclusionStrength(CloudChanceStruct cloud) {
        return cloud != null
                ? cloud.getCloudHandle().getAmbientOcclusionStrength()
                : EngineSetting.CLOUD_DEFAULT_AMBIENT_OCCLUSION_STRENGTH;
    }

    private float resolveBrightnessMultiplier(CloudChanceStruct cloud) {
        return cloud != null
                ? cloud.getCloudHandle().getBrightnessMultiplier()
                : EngineSetting.CLOUD_DEFAULT_BRIGHTNESS_MULTIPLIER;
    }

    private float resolveToonBands(CloudChanceStruct cloud) {
        return cloud != null ? (float) cloud.getCloudHandle().getToonBands() : EngineSetting.CLOUD_DEFAULT_TOON_BANDS;
    }

    private float resolveDensityNoiseScale(CloudChanceStruct cloud) {
        return cloud != null
                ? cloud.getCloudHandle().getDensityNoiseScale()
                : EngineSetting.CLOUD_DEFAULT_DENSITY_NOISE_SCALE;
    }

    private float resolveNoiseWarpStrength(CloudChanceStruct cloud) {
        return cloud != null
                ? cloud.getCloudHandle().getNoiseWarpStrength()
                : EngineSetting.CLOUD_DEFAULT_NOISE_WARP_STRENGTH;
    }

    private float resolveCoverageBias(CloudChanceStruct cloud) {
        return cloud.getCloudHandle().getCoverageBias();
    }

    private float resolveSilhouetteSoftness(CloudChanceStruct cloud) {
        return cloud != null
                ? cloud.getCloudHandle().getSilhouetteSoftness()
                : EngineSetting.CLOUD_DEFAULT_SILHOUETTE_SOFTNESS;
    }
}