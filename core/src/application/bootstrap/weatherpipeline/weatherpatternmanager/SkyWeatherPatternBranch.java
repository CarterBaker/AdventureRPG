package application.bootstrap.weatherpipeline.weatherpatternmanager;

import java.util.Arrays;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

class SkyWeatherPatternBranch extends BranchPackage {

    /*
     * Flattens WeatherPatternManager's active patterns into the
     * SkyWeatherPatternData UBO every frame. Each pattern is pushed as a
     * real world-space cloud volume (position, altitude, footprint) so the
     * sky raymarch (Clouds.glsl) samples true world positions through
     * SkyCloudUtility.glsl's world-scale shape functions, rather than an
     * ungrounded view-direction sample.
     */

    private static final int MAX_PATTERNS = EngineSetting.WEATHER_PATTERN_MAX_ACTIVE_COUNT;
    private static final float FOOTPRINT_RADIUS_CHUNKS = EngineSetting.WEATHER_PATTERN_SKY_FOOTPRINT_CHUNKS * 0.5f;
    private static final float DEFAULT_VERTICAL_THICKNESS = 12.0f;
    private static final float ELEVATION_LIMIT_MARGIN_RADIANS = (float) Math.toRadians(8.0);
    private static final float MIN_HALF_THICKNESS_BLOCKS = 1.0f;

    // A cloud's real vertical thickness reads as a paper-thin sliver once
    // its angular height drops far enough below eye resolution — this
    // floors the apparent thickness against distance so far patterns still
    // read as solid bodies. Close patterns are unaffected, since their
    // real thickness already exceeds the floor there.
    private static final float MIN_ANGULAR_HALF_THICKNESS_RATIO = (float) Math.tan(Math.toRadians(1.25));

    private WeatherPatternManager weatherPatternManager;
    private WeatherManager weatherManager;
    private WorldManager worldManager;
    private UBOManager uboManager;
    private PlayerManager playerManager;
    private WindowManager windowManager;

    private UBOHandle skyWeatherPatternData;

    private Float[] bearing;
    private Float[] elevation;
    private Float[] angularWidth;
    private Float[] angularHeight;
    private Float[] fadeAlpha;
    private Float[] intensity;
    private Float[] coverage;
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
    private Vector3[] center;
    private Vector3[] halfExtent;
    private Float[] seed;
    private Float[] domainRotation;

    private double frameHighestElevationTop;

    @Override
    protected void create() {
        this.bearing = newFloatArray();
        this.elevation = newFloatArray();
        this.angularWidth = newFloatArray();
        this.angularHeight = newFloatArray();
        this.fadeAlpha = newFloatArray();
        this.intensity = newFloatArray();
        this.coverage = newFloatArray();
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
        this.center = newVector3Array();
        this.halfExtent = newVector3Array();
        this.seed = newFloatArray();
        this.domainRotation = newFloatArray();
    }

    @Override
    protected void get() {
        this.weatherPatternManager = get(WeatherPatternManager.class);
        this.weatherManager = get(WeatherManager.class);
        this.worldManager = get(WorldManager.class);
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
        pushPatterns();
    }

    private void pushPatterns() {

        long referenceCoordinate = weatherManager.getReferenceCoordinate();
        int referenceChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int referenceChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        float cameraY = resolveCameraY();

        Arrays.fill(fadeAlpha, 0f);

        Long2ObjectOpenHashMap<WeatherPatternStruct> activePatterns = weatherPatternManager.getActivePatterns();

        this.frameHighestElevationTop = -Math.PI / 2.0;

        for (WeatherPatternStruct pattern : activePatterns.values())
            writeEntry(pattern.getSlot(), pattern, referenceChunkX, referenceChunkZ, worldWidthChunks,
                    worldHeightChunks, cameraY);

        double elevationLimit = Math.min(frameHighestElevationTop + ELEVATION_LIMIT_MARGIN_RADIANS, Math.PI / 2.0);

        skyWeatherPatternData.updateUniform("u_patternCount", MAX_PATTERNS);
        skyWeatherPatternData.updateUniform("u_patternBearing", bearing);
        skyWeatherPatternData.updateUniform("u_patternElevation", elevation);
        skyWeatherPatternData.updateUniform("u_patternAngularWidth", angularWidth);
        skyWeatherPatternData.updateUniform("u_patternAngularHeight", angularHeight);
        skyWeatherPatternData.updateUniform("u_patternFadeAlpha", fadeAlpha);
        skyWeatherPatternData.updateUniform("u_patternIntensity", intensity);
        skyWeatherPatternData.updateUniform("u_patternCoverage", coverage);
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
        skyWeatherPatternData.updateUniform("u_patternCenter", center);
        skyWeatherPatternData.updateUniform("u_patternHalfExtent", halfExtent);
        skyWeatherPatternData.updateUniform("u_patternSeed", seed);
        skyWeatherPatternData.updateUniform("u_patternDomainRotation", domainRotation);
        skyWeatherPatternData.updateUniform("u_skyElevationLimit", (float) Math.sin(elevationLimit));

        uboManager.push(skyWeatherPatternData);
    }

    private void writeEntry(
            int index,
            WeatherPatternStruct pattern,
            int referenceChunkX,
            int referenceChunkZ,
            int worldWidthChunks,
            int worldHeightChunks,
            float cameraY) {

        double dx = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkX(), referenceChunkX, worldWidthChunks);
        double dz = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkZ(), referenceChunkZ, worldHeightChunks);
        double distanceChunks = Math.sqrt(dx * dx + dz * dz);
        double distanceBlocks = Math.max(distanceChunks * EngineSetting.CHUNK_SIZE, 1.0);

        WeatherHandle targetWeather = pattern.getWeatherHandle();
        WeatherHandle sourceWeather = pattern.getPreviousWeatherHandle();
        float blend = smoothstep01(pattern.getTransitionT());

        CloudChanceStruct targetCloud = targetWeather.getPrimaryCloud();
        CloudChanceStruct sourceCloud = sourceWeather.getPrimaryCloud();

        float visualScale = lerp(sourceWeather.getVisualScale(), targetWeather.getVisualScale(), blend);
        float footprintRadiusChunks = FOOTPRINT_RADIUS_CHUNKS * visualScale;

        bearing[index] = (float) Math.atan2(dx, -dz);
        angularWidth[index] = (float) Math.atan2(footprintRadiusChunks, Math.max(distanceChunks, 0.001));

        float baseAltitude = lerp(resolveAltitude(sourceCloud), resolveAltitude(targetCloud), blend);
        float rawVerticalThickness = lerp(
                resolveVerticalThickness(sourceCloud), resolveVerticalThickness(targetCloud), blend);

        float minVisibleThickness = (float) distanceBlocks * MIN_ANGULAR_HALF_THICKNESS_RATIO * 2f;
        float verticalThickness = Math.max(rawVerticalThickness, minVisibleThickness);

        double topRelativeY = (baseAltitude + verticalThickness) - cameraY;
        double bottomRelativeY = baseAltitude - cameraY;
        double elevTop = Math.atan2(topRelativeY, distanceBlocks);
        double elevBottom = Math.atan2(bottomRelativeY, distanceBlocks);

        elevation[index] = (float) ((elevTop + elevBottom) * 0.5);
        angularHeight[index] = (float) Math.max((elevTop - elevBottom) * 0.5, 0.0);

        fadeAlpha[index] = pattern.getFadeAlpha();
        intensity[index] = pattern.getIntensity();

        float coverageBlend = lerp(sourceWeather.getCloudCoverage(), targetWeather.getCloudCoverage(), blend);
        coverage[index] = coverageBlend;

        if (pattern.getFadeAlpha() > 0.0f && coverageBlend > 0.001f)
            frameHighestElevationTop = Math.max(frameHighestElevationTop, elevTop);

        color[index].set(
                lerp(resolveColorR(sourceCloud), resolveColorR(targetCloud), blend),
                lerp(resolveColorG(sourceCloud), resolveColorG(targetCloud), blend),
                lerp(resolveColorB(sourceCloud), resolveColorB(targetCloud), blend));
        topColor[index].set(
                lerp(resolveTopColorR(sourceCloud), resolveTopColorR(targetCloud), blend),
                lerp(resolveTopColorG(sourceCloud), resolveTopColorG(targetCloud), blend),
                lerp(resolveTopColorB(sourceCloud), resolveTopColorB(targetCloud), blend));
        shadowColor[index].set(
                lerp(resolveShadowColorR(sourceCloud), resolveShadowColorR(targetCloud), blend),
                lerp(resolveShadowColorG(sourceCloud), resolveShadowColorG(targetCloud), blend),
                lerp(resolveShadowColorB(sourceCloud), resolveShadowColorB(targetCloud), blend));

        float sourceDensity = sourceCloud != null ? resolveDensity(sourceCloud) : 0f;
        float targetDensity = targetCloud != null ? resolveDensity(targetCloud) : 0f;
        density[index] = lerp(sourceDensity, targetDensity, blend);

        shadeStrength[index] = lerp(resolveShadeStrength(sourceCloud), resolveShadeStrength(targetCloud), blend);
        rimLightStrength[index] = lerp(
                resolveRimLightStrength(sourceCloud), resolveRimLightStrength(targetCloud), blend);
        ambientOcclusionStrength[index] = lerp(
                resolveAmbientOcclusionStrength(sourceCloud), resolveAmbientOcclusionStrength(targetCloud), blend);
        brightnessMultiplier[index] = lerp(
                resolveBrightnessMultiplier(sourceCloud), resolveBrightnessMultiplier(targetCloud), blend);
        toonBands[index] = lerp(resolveToonBands(sourceCloud), resolveToonBands(targetCloud), blend);
        densityNoiseScale[index] = lerp(
                resolveDensityNoiseScale(sourceCloud), resolveDensityNoiseScale(targetCloud), blend);
        noiseWarpStrength[index] = lerp(
                resolveNoiseWarpStrength(sourceCloud), resolveNoiseWarpStrength(targetCloud), blend);

        float sourceCoverageBias = sourceCloud != null ? resolveCoverageBias(sourceCloud) : 0f;
        float targetCoverageBias = targetCloud != null ? resolveCoverageBias(targetCloud) : 0f;
        coverageBias[index] = lerp(sourceCoverageBias, targetCoverageBias, blend);

        silhouetteSoftness[index] = lerp(
                resolveSilhouetteSoftness(sourceCloud), resolveSilhouetteSoftness(targetCloud), blend);

        float altitudeMid = baseAltitude + verticalThickness * 0.5f;
        float footprintRadiusBlocks = footprintRadiusChunks * EngineSetting.CHUNK_SIZE;
        float halfThickness = Math.max(verticalThickness * 0.5f, MIN_HALF_THICKNESS_BLOCKS);

        center[index].set(
                (float) (dx * EngineSetting.CHUNK_SIZE),
                altitudeMid,
                (float) (dz * EngineSetting.CHUNK_SIZE));
        halfExtent[index].set(footprintRadiusBlocks, halfThickness, footprintRadiusBlocks);

        WeatherPatternLobeStruct[] lobes = pattern.getLobes();
        seed[index] = lobes.length > 0 ? lobes[0].getRandomSeed() : 0f;
        domainRotation[index] = lobes.length > 0 ? lobes[0].getDomainRotation() : 0f;
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

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float smoothstep01(float t) {
        float c = Math.max(0f, Math.min(1f, t));
        return c * c * (3f - 2f * c);
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

    private float resolveAltitude(CloudChanceStruct cloud) {
        return cloud != null ? cloud.getEffectiveAltitude() : EngineSetting.CLOUD_DEFAULT_SKY_ALTITUDE;
    }

    private float resolveVerticalThickness(CloudChanceStruct cloud) {
        return cloud != null ? cloud.getCloudHandle().getVerticalThickness() : DEFAULT_VERTICAL_THICKNESS;
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