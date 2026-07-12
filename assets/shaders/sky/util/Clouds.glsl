#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL

#include "includes/TimeData.glsl"
#include "includes/SkyWeatherPatternData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/WindData.glsl"
#include "sky/util/SkyCloudUtility.glsl"

/*
* Sky-dome distant weather preview. Raymarches a thin unbounded layer,
 * shaded by a per-fragment blend of every active pattern in
 * SkyWeatherPatternData. Each pattern is weighted by its angular distance
 * from the ray direction and by its own fade-in/out state, so a pattern
 * never appears or disappears with a hard pop, and a weather with no
 * cloud archetype never paints a shape of its own — it can only ever
 * thin out real clouds nearby.
 */

const float SKY_HORIZON_FADE_START = -0.02;
const float SKY_HORIZON_FADE_END   = 0.06;

const float SKY_NOISE_WORLD_SCALE       = 1.0 / 45.0;
const float SKY_LAYER_SCALE             = 180.0 * SKY_NOISE_WORLD_SCALE;
const float SKY_LAYER_THICKNESS         = 24.0  * SKY_NOISE_WORLD_SCALE;
const int   SKY_RAYMARCH_STEPS          = 8;
const float SKY_DETAIL_JITTER           = 0.35;
const float SKY_STEP_ALPHA_SCALE        = 0.65;
const float SKY_LIGHT_TAP_DISTANCE      = 9.0   * SKY_NOISE_WORLD_SCALE;
const float SKY_TOP_TINT_MIX            = 0.18;
const float SKY_SHADOW_TINT_MIX         = 0.15;
const float SKY_INTENSITY_DENSITY_FLOOR = 0.4;

const float SKY_PATTERN_WEIGHT_INNER_SCALE = 0.6;
const float SKY_PATTERN_WEIGHT_OUTER_SCALE = 1.6;
const float SKY_PATTERN_MIN_ANGULAR_WIDTH  = 0.05;

const vec3  SKY_DEFAULT_COLOR               = vec3(1.0);
const vec3  SKY_DEFAULT_TOP_COLOR           = vec3(1.0);
const vec3  SKY_DEFAULT_SHADOW_COLOR        = vec3(0.6, 0.63, 0.7);
const float SKY_DEFAULT_DENSITY             = 0.8;
const float SKY_DEFAULT_SHADE_STRENGTH      = 0.5;
const float SKY_DEFAULT_RIM_LIGHT_STRENGTH  = 0.35;
const float SKY_DEFAULT_AO_STRENGTH         = 0.4;
const float SKY_DEFAULT_BRIGHTNESS          = 1.0;
const float SKY_DEFAULT_TOON_BANDS          = 3.0;
const float SKY_DEFAULT_DENSITY_NOISE_SCALE = 1.0;
const float SKY_DEFAULT_NOISE_WARP_STRENGTH = 0.6;
const float SKY_DEFAULT_COVERAGE_BIAS       = 0.5;
const float SKY_DEFAULT_SILHOUETTE_SOFTNESS = 0.08;

struct WeatherPatternSample {
    float coverage;
    vec3  color;
    vec3  topColor;
    vec3  shadowColor;
    float density;
    float shadeStrength;
    float rimStrength;
    float aoStrength;
    float brightness;
    float toonBands;
    float densityNoiseScale;
    float noiseWarpStrength;
    float coverageBias;
    float silhouetteSoftness;
    float intensity;
};

WeatherPatternSample resolvePatternWeather(vec3 dir) {
    WeatherPatternSample result;
    result.coverage = 0.0;
    result.color = vec3(0.0);
    result.topColor = vec3(0.0);
    result.shadowColor = vec3(0.0);
    result.density = 0.0;
    result.shadeStrength = 0.0;
    result.rimStrength = 0.0;
    result.aoStrength = 0.0;
    result.brightness = 0.0;
    result.toonBands = 0.0;
    result.densityNoiseScale = 0.0;
    result.noiseWarpStrength = 0.0;
    result.coverageBias = 0.0;
    result.silhouetteSoftness = 0.0;
    result.intensity = 0.0;

    float totalWeight = 0.0;

    for (int i = 0; i < u_patternCount; i++) {
        vec3 patternDir = vec3(sin(u_patternBearing[i]), 0.0, -cos(u_patternBearing[i]));
        float angularDist = acos(clamp(dot(dir, patternDir), -1.0, 1.0));

        float edge = max(u_patternAngularWidth[i], SKY_PATTERN_MIN_ANGULAR_WIDTH);
        float geometricWeight = 1.0 - smoothstep(
            edge * SKY_PATTERN_WEIGHT_INNER_SCALE,
            edge * SKY_PATTERN_WEIGHT_OUTER_SCALE,
            angularDist);

        float weight = geometricWeight * clamp(u_patternFadeAlpha[i], 0.0, 1.0);

        if (weight <= 0.0001)
        continue;

        totalWeight += weight;

        result.coverage           += u_patternCoverage[i] * weight;
        result.color              += u_patternColor[i] * weight;
        result.topColor           += u_patternTopColor[i] * weight;
        result.shadowColor        += u_patternShadowColor[i] * weight;
        result.density             += u_patternDensity[i] * weight;
        result.shadeStrength       += u_patternShadeStrength[i] * weight;
        result.rimStrength         += u_patternRimLightStrength[i] * weight;
        result.aoStrength          += u_patternAmbientOcclusionStrength[i] * weight;
        result.brightness          += u_patternBrightnessMultiplier[i] * weight;
        result.toonBands           += u_patternToonBands[i] * weight;
        result.densityNoiseScale   += u_patternDensityNoiseScale[i] * weight;
        result.noiseWarpStrength   += u_patternNoiseWarpStrength[i] * weight;
        result.coverageBias        += u_patternCoverageBias[i] * weight;
        result.silhouetteSoftness  += u_patternSilhouetteSoftness[i] * weight;
        result.intensity           += u_patternIntensity[i] * weight;
    }

    if (totalWeight <= 0.0001) {
        result.color = SKY_DEFAULT_COLOR;
        result.topColor = SKY_DEFAULT_TOP_COLOR;
        result.shadowColor = SKY_DEFAULT_SHADOW_COLOR;
        result.density = SKY_DEFAULT_DENSITY;
        result.shadeStrength = SKY_DEFAULT_SHADE_STRENGTH;
        result.rimStrength = SKY_DEFAULT_RIM_LIGHT_STRENGTH;
        result.aoStrength = SKY_DEFAULT_AO_STRENGTH;
        result.brightness = SKY_DEFAULT_BRIGHTNESS;
        result.toonBands = SKY_DEFAULT_TOON_BANDS;
        result.densityNoiseScale = SKY_DEFAULT_DENSITY_NOISE_SCALE;
        result.noiseWarpStrength = SKY_DEFAULT_NOISE_WARP_STRENGTH;
        result.coverageBias = SKY_DEFAULT_COVERAGE_BIAS;
        result.silhouetteSoftness = SKY_DEFAULT_SILHOUETTE_SOFTNESS;
        return result;
    }

    float invWeight = 1.0 / totalWeight;
    result.coverage           *= invWeight;
    result.color              *= invWeight;
    result.topColor           *= invWeight;
    result.shadowColor        *= invWeight;
    result.density             *= invWeight;
    result.shadeStrength       *= invWeight;
    result.rimStrength         *= invWeight;
    result.aoStrength          *= invWeight;
    result.brightness          *= invWeight;
    result.toonBands           *= invWeight;
    result.densityNoiseScale   *= invWeight;
    result.noiseWarpStrength   *= invWeight;
    result.coverageBias        *= invWeight;
    result.silhouetteSoftness  *= invWeight;
    result.intensity           *= invWeight;

    return result;
}

vec4 calculateClouds(vec3 dir, float dailySeed) {
    if (dir.y < SKY_HORIZON_FADE_START)
    return vec4(0.0);

    float horizonFade = smoothstep(SKY_HORIZON_FADE_START, SKY_HORIZON_FADE_END, dir.y);

    if (horizonFade <= 0.001)
    return vec4(0.0);

    WeatherPatternSample weather = resolvePatternWeather(dir);

    float sunWeight  = clamp(u_sunIntensity / 0.3, 0.0, 1.0);
    vec3  lightDir   = normalize(mix(u_moonDirection, u_sunDirection, sunWeight));
    vec3  lightColor = mix(u_moonColor, u_sunColor, sunWeight);
    float lightPower = mix(u_moonIntensity, u_sunIntensity, sunWeight);

    vec3 wind = vec3(
        u_windDriftOffset.x + dailySeed,
        0.0,
        u_windDriftOffset.y + dailySeed * 1.3) * SKY_NOISE_WORLD_SCALE;

    float stepDepth = SKY_LAYER_THICKNESS / float(SKY_RAYMARCH_STEPS);

    vec3 topTint    = mix(weather.topColor, u_skyZenithColor, SKY_TOP_TINT_MIX);
    vec3 shadowTint = mix(weather.shadowColor, u_skyHorizonColor * 0.4, SKY_SHADOW_TINT_MIX);

    float effectiveCoverageBias = clamp(mix(weather.coverageBias, weather.coverage, 0.5), 0.0, 1.0);
    float heightT = clamp(dir.y / mix(0.45, 1.0, clamp(weather.coverage, 0.0, 1.0)), 0.0, 1.0);
    float intensityFactor = mix(SKY_INTENSITY_DENSITY_FLOOR, 1.0, clamp(weather.intensity, 0.0, 1.0));

    int toonBands = max(int(weather.toonBands + 0.5), 1);

    vec4 accum = vec4(0.0);

    for (int i = 0; i < SKY_RAYMARCH_STEPS; i++) {
        if (accum.a > 0.95)
        break;

        vec3 p = dir * (SKY_LAYER_SCALE + float(i) * stepDepth) + wind;

        float density = sampleSkyCloudDensity(
            p, heightT, weather.densityNoiseScale, weather.noiseWarpStrength, SKY_DETAIL_JITTER,
            effectiveCoverageBias, weather.silhouetteSoftness, dailySeed, u_time)
        * weather.density * intensityFactor;

        if (density > 0.01) {
            float litDensity = sampleSkyCloudDensity(
                p + lightDir * SKY_LIGHT_TAP_DISTANCE, heightT, weather.densityNoiseScale,
                weather.noiseWarpStrength, SKY_DETAIL_JITTER, effectiveCoverageBias,
                weather.silhouetteSoftness, dailySeed, u_time)
            * weather.density * intensityFactor;
            float lightLift = clamp((density - litDensity) * 2.0 + 0.5, 0.0, 1.0);

            vec3 shaded = shadeSkyCloudLit(
                weather.color, topTint, shadowTint, lightColor, lightPower,
                heightT, lightLift, density, toonBands,
                weather.shadeStrength, weather.rimStrength, weather.aoStrength, weather.brightness);

            float stepAlpha = clamp(density * SKY_STEP_ALPHA_SCALE, 0.0, 1.0);
            accum.rgb += (1.0 - accum.a) * stepAlpha * shaded;
            accum.a   += (1.0 - accum.a) * stepAlpha;
        }
    }

    accum.a *= horizonFade;

    return accum;
}

#endif