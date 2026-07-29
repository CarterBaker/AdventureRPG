package application.bootstrap.weatherpipeline.skymanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SkyColorSystem extends SystemPackage {

    /*
     * The single authoritative source of every weather-pipeline daytime
     * color: horizon, zenith, cloud, and fog. Replicates SkyColor.glsl's
     * altitude keyframes CPU-side for every active grid, derives cloud and
     * fog tints from those same keyframes, and pushes all four into that
     * grid's own SkyColorData UBO every frame. Season tint and sunrise/
     * sunset color come from SeasonColorBlendBranch; current temperature
     * is resolved per grid from WeatherPatternManager and biases the
     * sunrise/sunset glow and cloud color toward a cold or hot accent
     * palette independently for each grid.
     */

    private static final float[] NIGHT_TOP = {
            EngineSetting.SKY_NIGHT_TOP_R, EngineSetting.SKY_NIGHT_TOP_G, EngineSetting.SKY_NIGHT_TOP_B };
    private static final float[] NIGHT_BOTTOM = {
            EngineSetting.SKY_NIGHT_BOTTOM_R, EngineSetting.SKY_NIGHT_BOTTOM_G, EngineSetting.SKY_NIGHT_BOTTOM_B };
    private static final float[] DAY_TOP = {
            EngineSetting.SKY_DAY_TOP_R, EngineSetting.SKY_DAY_TOP_G, EngineSetting.SKY_DAY_TOP_B };
    private static final float[] DAY_BOTTOM = {
            EngineSetting.SKY_DAY_BOTTOM_R, EngineSetting.SKY_DAY_BOTTOM_G, EngineSetting.SKY_DAY_BOTTOM_B };

    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;
    private WeatherPatternManager weatherPatternManager;
    private ClockManager clockManager;
    private ClockHandle clockHandle;
    private SeasonBlendSystem seasonBlendSystem;

    // Scratch — reused every push, never reallocated
    private final float[] temperatureAccentScratch = new float[3];
    private final float[] cycleScratch = new float[4];
    private final float[] horizonScratch = new float[3];
    private final float[] zenithScratch = new float[3];
    private final float[] cloudColorScratch = new float[3];
    private final float[] fogColorScratch = new float[3];
    private final float[] offsetScratch = new float[3];
    private final float[] sunriseScratch = new float[3];

    private final Vector3 horizonVector = new Vector3();
    private final Vector3 zenithVector = new Vector3();
    private final Vector3 cloudColorVector = new Vector3();
    private final Vector3 fogColorVector = new Vector3();

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.weatherPatternManager = get(WeatherPatternManager.class);
        this.clockManager = get(ClockManager.class);
    }

    @Override
    protected void awake() {
        this.clockHandle = clockManager.getClockHandle();
    }

    // Assignment \\

    void assignData(SeasonBlendSystem seasonBlendBranch) {
        this.seasonBlendSystem = seasonBlendBranch;
    }

    // Update \\

    @Override
    protected void update() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++) {
            GridInstance grid = (GridInstance) elements[i];
            resolveTemperatureAccent(weatherPatternManager.getCurrentTemperature(grid));
            pushData(grid);
        }
    }

    // Temperature Accent \\

    private void resolveTemperatureAccent(float temperature) {

        float t = remapClamped(
                temperature,
                EngineSetting.SKY_TEMPERATURE_COLD_REFERENCE,
                EngineSetting.SKY_TEMPERATURE_HOT_REFERENCE);

        temperatureAccentScratch[0] = lerp(
                EngineSetting.SKY_TEMPERATURE_COLD_ACCENT_R, EngineSetting.SKY_TEMPERATURE_HOT_ACCENT_R, t);
        temperatureAccentScratch[1] = lerp(
                EngineSetting.SKY_TEMPERATURE_COLD_ACCENT_G, EngineSetting.SKY_TEMPERATURE_HOT_ACCENT_G, t);
        temperatureAccentScratch[2] = lerp(
                EngineSetting.SKY_TEMPERATURE_COLD_ACCENT_B, EngineSetting.SKY_TEMPERATURE_HOT_ACCENT_B, t);
    }

    // Push \\

    private void pushData(GridInstance grid) {

        float t = (float) grid.getLocationTimeStruct().getVisualTimeOfDay();
        float yearProgress = (float) clockHandle.getVisualYearProgress();
        float dailyRandom = clockHandle.getRandomNoiseFromDay();
        float dailyVar = computeDailyVariationMask(t);

        computeCycleFactors(t);
        float dayF = cycleScratch[0];
        float sunriseF = cycleScratch[2];
        float sunsetF = cycleScratch[3];

        blend2(horizonScratch, NIGHT_BOTTOM, cycleScratch[1], DAY_BOTTOM, dayF);
        blend2(zenithScratch, NIGHT_TOP, cycleScratch[1], DAY_TOP, dayF);

        Vector3 seasonTint = seasonBlendSystem.getTintColorForYearProgress(yearProgress);
        float dailySeasonStr = dailyRandom * dailyVar;
        float seasonStrength = dayF * dailySeasonStr * EngineSetting.SKY_SEASONAL_STRENGTH_SCALE;
        float tintScale = EngineSetting.SKY_SEASONAL_TINT_OFFSET_SCALE;

        offsetScratch[0] = (seasonTint.x - 1.0f) * tintScale;
        offsetScratch[1] = (seasonTint.y - 1.0f) * tintScale;
        offsetScratch[2] = (seasonTint.z - 1.0f) * tintScale;

        addScaled(horizonScratch, offsetScratch, seasonStrength);
        addScaled(zenithScratch, offsetScratch, seasonStrength);

        offsetScratch[0] = fract(dailyRandom) * EngineSetting.SKY_DAILY_OFFSET_R_SCALE
                + EngineSetting.SKY_DAILY_OFFSET_R_BIAS;
        offsetScratch[1] = fract(dailyRandom * EngineSetting.SKY_DAILY_HASH_G) * EngineSetting.SKY_DAILY_OFFSET_G_SCALE
                + EngineSetting.SKY_DAILY_OFFSET_G_BIAS;
        offsetScratch[2] = fract(dailyRandom * EngineSetting.SKY_DAILY_HASH_B) * EngineSetting.SKY_DAILY_OFFSET_B_SCALE
                + EngineSetting.SKY_DAILY_OFFSET_B_BIAS;

        float dailyStrength = dailyVar * dayF;
        addScaled(horizonScratch, offsetScratch, dailyStrength);
        addScaled(zenithScratch, offsetScratch, dailyStrength);

        Vector3 seasonSunrise = seasonBlendSystem.getSunriseColorForYearProgress(yearProgress);
        sunriseScratch[0] = seasonSunrise.x;
        sunriseScratch[1] = seasonSunrise.y;
        sunriseScratch[2] = seasonSunrise.z;

        blend2(sunriseScratch, sunriseScratch, 1f - EngineSetting.SKY_TEMPERATURE_ACCENT_STRENGTH,
                temperatureAccentScratch, EngineSetting.SKY_TEMPERATURE_ACCENT_STRENGTH);
        float ssF = Math.min(1.0f, sunriseF + sunsetF);

        lerpInPlace(horizonScratch, sunriseScratch, ssF);
        lerpInPlace(zenithScratch, sunriseScratch, ssF);

        blend2(cloudColorScratch, horizonScratch, EngineSetting.SKY_CLOUD_COLOR_HORIZON_WEIGHT,
                zenithScratch, 1f - EngineSetting.SKY_CLOUD_COLOR_HORIZON_WEIGHT);
        lerpInPlace(cloudColorScratch, temperatureAccentScratch, ssF * EngineSetting.SKY_CLOUD_COLOR_ACCENT_STRENGTH);

        float gray = (horizonScratch[0] + horizonScratch[1] + horizonScratch[2]) * 0.333f;
        horizonScratch[0] += (gray - horizonScratch[0]) * EngineSetting.SKY_HORIZON_DESATURATION;
        horizonScratch[1] += (gray - horizonScratch[1]) * EngineSetting.SKY_HORIZON_DESATURATION;
        horizonScratch[2] += (gray - horizonScratch[2]) * EngineSetting.SKY_HORIZON_DESATURATION;

        fogColorScratch[0] = horizonScratch[0] + EngineSetting.SKY_FOG_COLOR_LIFT;
        fogColorScratch[1] = horizonScratch[1] + EngineSetting.SKY_FOG_COLOR_LIFT;
        fogColorScratch[2] = horizonScratch[2] + EngineSetting.SKY_FOG_COLOR_LIFT;

        UBOInstance skyColorUBO = grid.getSkyColorUBO();

        horizonVector.set(horizonScratch[0], horizonScratch[1], horizonScratch[2]);
        zenithVector.set(zenithScratch[0], zenithScratch[1], zenithScratch[2]);
        cloudColorVector.set(cloudColorScratch[0], cloudColorScratch[1], cloudColorScratch[2]);
        fogColorVector.set(fogColorScratch[0], fogColorScratch[1], fogColorScratch[2]);

        skyColorUBO.updateUniform("u_skyHorizonColor", horizonVector);
        skyColorUBO.updateUniform("u_skyZenithColor", zenithVector);
        skyColorUBO.updateUniform("u_skyCloudColor", cloudColorVector);
        skyColorUBO.updateUniform("u_skyFogColor", fogColorVector);
        uboManager.push(skyColorUBO);
    }

    // Cycle Factors \\

    private void computeCycleFactors(float t) {

        float sunriseMin = (float) EngineSetting.CLOCK_SUNRISE_MIN;
        float sunriseMax = (float) EngineSetting.CLOCK_SUNRISE_MAX;
        float sunsetMin = (float) EngineSetting.CLOCK_SUNSET_MIN;
        float sunsetMax = (float) EngineSetting.CLOCK_SUNSET_MAX;

        float sunrise = bellCurve(t, sunriseMin, (sunriseMin + sunriseMax) * 0.5f, sunriseMax);
        float sunset = bellCurve(t, sunsetMin, (sunsetMin + sunsetMax) * 0.5f, sunsetMax);
        float day = Math.max(0.0f, smoothstep(sunriseMin, sunriseMax, t) - smoothstep(sunsetMin, sunsetMax, t));
        float night = Math.max(0.0f, 1.0f - day - sunrise - sunset);

        cycleScratch[0] = day;
        cycleScratch[1] = night;
        cycleScratch[2] = sunrise;
        cycleScratch[3] = sunset;
    }

    // Daily Variation Mask \\

    private float computeDailyVariationMask(float t) {
        float rise = smoothstep((float) EngineSetting.CLOCK_SUNRISE_MIN, (float) EngineSetting.CLOCK_SUNRISE_MAX, t);
        float set = smoothstep((float) EngineSetting.CLOCK_SUNSET_MIN, (float) EngineSetting.CLOCK_SUNSET_MAX, t);
        return Math.max(0.0f, rise - set);
    }

    // Math Helpers \\

    private void blend2(float[] target, float[] a, float wa, float[] b, float wb) {
        target[0] = a[0] * wa + b[0] * wb;
        target[1] = a[1] * wa + b[1] * wb;
        target[2] = a[2] * wa + b[2] * wb;
    }

    private void addScaled(float[] target, float[] source, float scale) {
        target[0] += source[0] * scale;
        target[1] += source[1] * scale;
        target[2] += source[2] * scale;
    }

    private void lerpInPlace(float[] target, float[] goal, float t) {
        target[0] += (goal[0] - target[0]) * t;
        target[1] += (goal[1] - target[1]) * t;
        target[2] += (goal[2] - target[2]) * t;
    }

    private float smoothstep(float edge0, float edge1, float t) {
        t = Math.max(0.0f, Math.min(1.0f, (t - edge0) / (edge1 - edge0)));
        return t * t * (3.0f - 2.0f * t);
    }

    private float bellCurve(float t, float min, float peak, float max) {
        if (t <= min || t >= max)
            return 0.0f;
        return t <= peak ? smoothstep(min, peak, t) : 1.0f - smoothstep(peak, max, t);
    }

    private float fract(float v) {
        return v - (float) Math.floor(v);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float remapClamped(float value, float low, float high) {
        float t = (value - low) / Math.max(high - low, 0.0001f);
        return Math.max(0.0f, Math.min(1.0f, t));
    }
}