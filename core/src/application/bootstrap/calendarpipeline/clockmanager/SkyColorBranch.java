package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class SkyColorBranch extends BranchPackage {

    /*
     * Replicates SkyColor.glsl CPU-side at altitude=0 (horizon) and altitude=1
     * (zenith), then pushes both to SkyColorData UBO each frame. The sky shader
     * reads these directly instead of recomputing them. Chunk shaders use
     * u_skyHorizonColor + u_maxDistanceFromCenter for distance fog blending.
     *
     * NOTE: computeCycleFactors() and computeSeasonFactors() mirror
     * DayNightCycle.glsl and SeasonCycle.glsl — verify against your GLSL if
     * results diverge.
     */

    // Internal
    private UBOManager uboManager;
    private ClockHandle clockHandle;

    // UBO
    private UBOHandle skyColorData;

    // Cached
    private float maxDistanceFromCenter;

    // Internal \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        this.skyColorData = uboManager.getUBOHandleFromUBOName(EngineSetting.SKY_COLOR_UBO);
        float radius = settings.maxRenderDistance / 2f;
        this.maxDistanceFromCenter = radius * radius;
    }

    // Assignment \\

    void assignData(ClockHandle clockHandle) {
        this.clockHandle = clockHandle;
    }

    // Update \\

    @Override
    protected void update() {
        pushData();
    }

    // Push \\

    private void pushData() {

        float t = (float) clockHandle.getVisualTimeOfDay();
        float yearProgress = (float) clockHandle.getVisualYearProgress();
        float dailyRandom = clockHandle.getRandomNoiseFromDay() / 2147483647.0f;
        float dailyVar = computeDailyVariationMask(t);

        float[] cycle = computeCycleFactors(t);
        float dayF = cycle[0];
        float nightF = cycle[1];
        float sunriseF = cycle[2];
        float sunsetF = cycle[3];

        float[] season = computeSeasonFactors(yearProgress);
        float springF = season[0];
        float summerF = season[1];
        float fallF = season[2];
        float winterF = season[3];

        float[] nightTop = { EngineSetting.SKY_NIGHT_TOP_R, EngineSetting.SKY_NIGHT_TOP_G,
                EngineSetting.SKY_NIGHT_TOP_B };
        float[] nightBottom = { EngineSetting.SKY_NIGHT_BOTTOM_R, EngineSetting.SKY_NIGHT_BOTTOM_G,
                EngineSetting.SKY_NIGHT_BOTTOM_B };
        float[] dayTop = { EngineSetting.SKY_DAY_TOP_R, EngineSetting.SKY_DAY_TOP_G, EngineSetting.SKY_DAY_TOP_B };
        float[] dayBottom = { EngineSetting.SKY_DAY_BOTTOM_R, EngineSetting.SKY_DAY_BOTTOM_G,
                EngineSetting.SKY_DAY_BOTTOM_B };

        float[] horizon = blend2(nightBottom, nightF, dayBottom, dayF);
        float[] zenith = blend2(nightTop, nightF, dayTop, dayF);

        float[] winterTint = { EngineSetting.SKY_WINTER_TINT_R, EngineSetting.SKY_WINTER_TINT_G,
                EngineSetting.SKY_WINTER_TINT_B };
        float[] summerTint = { EngineSetting.SKY_SUMMER_TINT_R, EngineSetting.SKY_SUMMER_TINT_G,
                EngineSetting.SKY_SUMMER_TINT_B };
        float[] springTint = { EngineSetting.SKY_SPRING_TINT_R, EngineSetting.SKY_SPRING_TINT_G,
                EngineSetting.SKY_SPRING_TINT_B };
        float[] fallTint = { EngineSetting.SKY_FALL_TINT_R, EngineSetting.SKY_FALL_TINT_G,
                EngineSetting.SKY_FALL_TINT_B };

        float[] seasonTint = blend4(winterTint, winterF, summerTint, summerF, springTint, springF, fallTint, fallF);
        float dailySeasonStr = dailyRandom * dailyVar;
        float seasonStrength = dayF * dailySeasonStr * EngineSetting.SKY_SEASONAL_STRENGTH_SCALE;
        float tintScale = EngineSetting.SKY_SEASONAL_TINT_OFFSET_SCALE;
        float[] seasonOffset = {
                (seasonTint[0] - 1.0f) * tintScale,
                (seasonTint[1] - 1.0f) * tintScale,
                (seasonTint[2] - 1.0f) * tintScale
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

        float[] winterSS = { EngineSetting.SKY_WINTER_SUNRISE_R, EngineSetting.SKY_WINTER_SUNRISE_G,
                EngineSetting.SKY_WINTER_SUNRISE_B };
        float[] summerSS = { EngineSetting.SKY_SUMMER_SUNRISE_R, EngineSetting.SKY_SUMMER_SUNRISE_G,
                EngineSetting.SKY_SUMMER_SUNRISE_B };
        float[] springSS = { EngineSetting.SKY_SPRING_SUNRISE_R, EngineSetting.SKY_SPRING_SUNRISE_G,
                EngineSetting.SKY_SPRING_SUNRISE_B };
        float[] fallSS = { EngineSetting.SKY_FALL_SUNRISE_R, EngineSetting.SKY_FALL_SUNRISE_G,
                EngineSetting.SKY_FALL_SUNRISE_B };

        float[] seasonSS = blend4(winterSS, winterF, summerSS, summerF, springSS, springF, fallSS, fallF);
        float ssF = Math.min(1.0f, sunriseF + sunsetF);

        lerpInPlace(horizon, seasonSS, ssF);
        lerpInPlace(zenith, seasonSS, ssF);

        float gray = (horizon[0] + horizon[1] + horizon[2]) * 0.333f;
        lerpInPlace(horizon, new float[] { gray, gray, gray }, EngineSetting.SKY_HORIZON_DESATURATION);

        skyColorData.updateUniform("u_skyHorizonColor", new Vector3(horizon[0], horizon[1], horizon[2]));
        skyColorData.updateUniform("u_skyZenithColor", new Vector3(zenith[0], zenith[1], zenith[2]));
        skyColorData.updateUniform("u_maxDistanceFromCenter", maxDistanceFromCenter);
        uboManager.push(skyColorData);
    }
    // Cycle Factors \\
    // Mirrors DayNightCycle.glsl — verify breakpoints match your GLSL

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

    // Season Factors \\
    // Mirrors SeasonCycle.glsl — verify breakpoints match your GLSL

    private float[] computeSeasonFactors(float y) {

        float spring = triangleWave(y, 0.00f, 0.25f, 0.50f);
        float summer = triangleWave(y, 0.25f, 0.50f, 0.75f);
        float fall = triangleWave(y, 0.50f, 0.75f, 1.00f);
        float winter = Math.max(0.0f, 1.0f - spring - summer - fall);

        return new float[] { spring, summer, fall, winter };
    }

    // Daily Variation Mask \\
    // Mirrors calculateDailyVariation() in SkyNoise.glsl — avoids midnight pop

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

    private float[] blend4(float[] a, float wa, float[] b, float wb,
            float[] c, float wc, float[] d, float wd) {
        return new float[] {
                a[0] * wa + b[0] * wb + c[0] * wc + d[0] * wd,
                a[1] * wa + b[1] * wb + c[1] * wc + d[1] * wd,
                a[2] * wa + b[2] * wb + c[2] * wc + d[2] * wd
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

    private float triangleWave(float y, float start, float peak, float end) {
        if (y <= start || y >= end)
            return 0.0f;
        return y <= peak ? smoothstep(start, peak, y) : 1.0f - smoothstep(peak, end, y);
    }

    private float fract(float v) {
        return v - (float) Math.floor(v);
    }
}