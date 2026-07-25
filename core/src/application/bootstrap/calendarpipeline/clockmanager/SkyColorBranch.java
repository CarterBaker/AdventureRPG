package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SkyColorBranch extends BranchPackage {

    /*
     * Replicates SkyColor.glsl CPU-side at altitude=0 (horizon) and
     * altitude=1 (zenith) for every active grid, pushing both into that
     * grid's own SkyColorData UBOInstance each frame so each window's sky
     * shader reads the correct color for wherever its own player sits.
     * Season tint and sunrise/sunset color come from the calendar's own
     * seasons via SeasonBlendBranch, which is location-independent; time of
     * day is not, so each grid reads its own LocationTimeStruct.
     */

    // Internal
    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;
    private ClockHandle clockHandle;
    private SeasonBlendBranch seasonBlendBranch;

    // Internal \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // Assignment \\

    void assignData(ClockHandle clockHandle, SeasonBlendBranch seasonBlendBranch) {
        this.clockHandle = clockHandle;
        this.seasonBlendBranch = seasonBlendBranch;
    }

    // Update \\

    @Override
    protected void update() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        for (int i = 0; i < size; i++)
            pushData((GridInstance) elements[i]);
    }

    // Push \\

    private void pushData(GridInstance grid) {

        float t = (float) grid.getLocationTimeStruct().getVisualTimeOfDay();
        float yearProgress = (float) clockHandle.getVisualYearProgress();
        float dailyRandom = clockHandle.getRandomNoiseFromDay();
        float dailyVar = computeDailyVariationMask(t);

        float[] cycle = computeCycleFactors(t);
        float dayF = cycle[0];
        float nightF = cycle[1];
        float sunriseF = cycle[2];
        float sunsetF = cycle[3];

        float[] nightTop = { EngineSetting.SKY_NIGHT_TOP_R, EngineSetting.SKY_NIGHT_TOP_G,
                EngineSetting.SKY_NIGHT_TOP_B };
        float[] nightBottom = { EngineSetting.SKY_NIGHT_BOTTOM_R, EngineSetting.SKY_NIGHT_BOTTOM_G,
                EngineSetting.SKY_NIGHT_BOTTOM_B };
        float[] dayTop = { EngineSetting.SKY_DAY_TOP_R, EngineSetting.SKY_DAY_TOP_G, EngineSetting.SKY_DAY_TOP_B };
        float[] dayBottom = { EngineSetting.SKY_DAY_BOTTOM_R, EngineSetting.SKY_DAY_BOTTOM_G,
                EngineSetting.SKY_DAY_BOTTOM_B };

        float[] horizon = blend2(nightBottom, nightF, dayBottom, dayF);
        float[] zenith = blend2(nightTop, nightF, dayTop, dayF);

        Vector3 seasonTint = seasonBlendBranch.getTintColorForYearProgress(yearProgress);
        float dailySeasonStr = dailyRandom * dailyVar;
        float seasonStrength = dayF * dailySeasonStr * EngineSetting.SKY_SEASONAL_STRENGTH_SCALE;
        float tintScale = EngineSetting.SKY_SEASONAL_TINT_OFFSET_SCALE;
        float[] seasonOffset = {
                (seasonTint.x - 1.0f) * tintScale,
                (seasonTint.y - 1.0f) * tintScale,
                (seasonTint.z - 1.0f) * tintScale
        };

        addScaled(horizon, seasonOffset, seasonStrength);
        addScaled(zenith, seasonOffset, seasonStrength);

        float[] dailyOffset = {
                fract(dailyRandom) * EngineSetting.SKY_DAILY_OFFSET_R_SCALE + EngineSetting.SKY_DAILY_OFFSET_R_BIAS,
                fract(dailyRandom * EngineSetting.SKY_DAILY_HASH_G) * EngineSetting.SKY_DAILY_OFFSET_G_SCALE
                        + EngineSetting.SKY_DAILY_OFFSET_G_BIAS,
                fract(dailyRandom * EngineSetting.SKY_DAILY_HASH_B) * EngineSetting.SKY_DAILY_OFFSET_B_SCALE
                        + EngineSetting.SKY_DAILY_OFFSET_B_BIAS
        };

        float dailyStrength = dailyVar * dayF;
        addScaled(horizon, dailyOffset, dailyStrength);
        addScaled(zenith, dailyOffset, dailyStrength);

        Vector3 seasonSunrise = seasonBlendBranch.getSunriseColorForYearProgress(yearProgress);
        float[] seasonSS = { seasonSunrise.x, seasonSunrise.y, seasonSunrise.z };
        float ssF = Math.min(1.0f, sunriseF + sunsetF);

        lerpInPlace(horizon, seasonSS, ssF);
        lerpInPlace(zenith, seasonSS, ssF);

        float gray = (horizon[0] + horizon[1] + horizon[2]) * 0.333f;
        lerpInPlace(horizon, new float[] { gray, gray, gray }, EngineSetting.SKY_HORIZON_DESATURATION);

        UBOInstance skyColorUBO = grid.getSkyColorUBO();
        skyColorUBO.updateUniform("u_skyHorizonColor", new Vector3(horizon[0], horizon[1], horizon[2]));
        skyColorUBO.updateUniform("u_skyZenithColor", new Vector3(zenith[0], zenith[1], zenith[2]));
        uboManager.push(skyColorUBO);
    }

    // Cycle Factors \\

    private float[] computeCycleFactors(float t) {

        float sunriseMin = (float) EngineSetting.CLOCK_SUNRISE_MIN;
        float sunriseMax = (float) EngineSetting.CLOCK_SUNRISE_MAX;
        float sunsetMin = (float) EngineSetting.CLOCK_SUNSET_MIN;
        float sunsetMax = (float) EngineSetting.CLOCK_SUNSET_MAX;

        float sunrise = bellCurve(t, sunriseMin, (sunriseMin + sunriseMax) * 0.5f, sunriseMax);
        float sunset = bellCurve(t, sunsetMin, (sunsetMin + sunsetMax) * 0.5f, sunsetMax);
        float day = Math.max(0.0f, smoothstep(sunriseMin, sunriseMax, t) - smoothstep(sunsetMin, sunsetMax, t));
        float night = Math.max(0.0f, 1.0f - day - sunrise - sunset);

        return new float[] { day, night, sunrise, sunset };
    }

    // Daily Variation Mask \\

    private float computeDailyVariationMask(float t) {
        float rise = smoothstep((float) EngineSetting.CLOCK_SUNRISE_MIN, (float) EngineSetting.CLOCK_SUNRISE_MAX, t);
        float set = smoothstep((float) EngineSetting.CLOCK_SUNSET_MIN, (float) EngineSetting.CLOCK_SUNSET_MAX, t);
        return Math.max(0.0f, rise - set);
    }

    // Math Helpers \\

    private float[] blend2(float[] a, float wa, float[] b, float wb) {
        return new float[] {
                a[0] * wa + b[0] * wb,
                a[1] * wa + b[1] * wb,
                a[2] * wa + b[2] * wb
        };
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
}