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
* Sky-dome distant weather preview. Every active pattern is placed in the
 * dome at its own true bearing AND elevation angle, both derived from its
 * real world position, altitude, and vertical thickness relative to the
 * camera (see SkyWeatherPatternBranch) — a storm reads as genuinely
 * sitting where it physically is, at its own real height, rather than
 * smearing across the whole upper sky.
 *
 * u_skyElevationLimit is the highest elevation any active, cloudy
 * pattern's own footprint can reach this frame, with a margin for its own
 * falloff. Every direction above that line skips the pattern loop and
 * raymarch entirely — since real weather stays low relative to render
 * distance, this is what keeps looking straight up cheap instead of
 * paying full per-pattern, per-step cost across the entire screen.
 */

const float SKY_HORIZON_FADE_START = -0.02;
const float SKY_HORIZON_FADE_END   = 0.06;

const float SKY_NOISE_WORLD_SCALE       = 1.0 / 45.0;
const float SKY_LAYER_SCALE             = 180.0 * SKY_NOISE_WORLD_SCALE;
const float SKY_LAYER_THICKNESS         = 24.0  * SKY_NOISE_WORLD_SCALE;
const int   SKY_RAYMARCH_STEPS          = 8;
const float SKY_DETAIL_JITTER           = 0.35;
const float SKY_STEP_ALPHA_SCALE        = 0.45;
const float SKY_LIGHT_TAP_DISTANCE      = 9.0   * SKY_NOISE_WORLD_SCALE;
const float SKY_TOP_TINT_MIX            = 0.32;
const float SKY_SHADOW_TINT_MIX         = 0.28;
const float SKY_INTENSITY_DENSITY_FLOOR = 0.2;

const float SKY_PATTERN_WEIGHT_INNER_SCALE  = 0.6;
const float SKY_PATTERN_WEIGHT_OUTER_SCALE  = 1.6;
const float SKY_PATTERN_MIN_ANGULAR_WIDTH   = 0.05;
const float SKY_PATTERN_MIN_ANGULAR_HEIGHT  = 0.03;
const float SKY_PATTERN_AZIMUTH_EPSILON     = 0.0005;
const float SKY_PI                          = 3.14159265359;
const float SKY_TWO_PI                      = 6.28318530718;

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
    float elevation;
    float angularHeight;
};

// Wraps an angle difference into [-PI, PI].
float wrapAngleDelta(float delta) {
    return mod(delta + SKY_PI, SKY_TWO_PI) - SKY_PI;
}

WeatherPatternSample resolvePatternWeather(vec3 dir, float rayElevation) {
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
    result.elevation = 0.0;
    result.angularHeight = 0.0;

    // Azimuth-only ray bearing — guarded for the near-zenith case where the
    // horizontal component collapses toward zero.
    float horizLen = length(dir.xz);
    float rayAzimuth = horizLen > SKY_PATTERN_AZIMUTH_EPSILON
    ? atan(dir.x, -dir.z)
    : 0.0;

    float totalWeight = 0.0;

    for (int i = 0; i < u_patternCount; i++) {
        // Cheap elevation reject first — this alone skips the azimuth
        // wrap/trig for most patterns on most pixels, since a ray's own
        // elevation only ever lines up with a handful of active patterns.
        float elevEdge = max(u_patternAngularHeight[i], SKY_PATTERN_MIN_ANGULAR_HEIGHT);
        float elevDiff = abs(rayElevation - u_patternElevation[i]);

        if (elevDiff > elevEdge * SKY_PATTERN_WEIGHT_OUTER_SCALE)
        continue;

        float azimuthDiff = wrapAngleDelta(rayAzimuth - u_patternBearing[i]);
        float angularDist = abs(azimuthDiff);

        float azimuthEdge = max(u_patternAngularWidth[i], SKY_PATTERN_MIN_ANGULAR_WIDTH);
        float azimuthWeight = 1.0 - smoothstep(
            azimuthEdge * SKY_PATTERN_WEIGHT_INNER_SCALE,
            azimuthEdge * SKY_PATTERN_WEIGHT_OUTER_SCALE,
            angularDist);

        float elevationWeight = 1.0 - smoothstep(
            elevEdge * SKY_PATTERN_WEIGHT_INNER_SCALE,
            elevEdge * SKY_PATTERN_WEIGHT_OUTER_SCALE,
            elevDiff);

        float weight = azimuthWeight * elevationWeight * clamp(u_patternFadeAlpha[i], 0.0, 1.0);

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
        result.elevation           += u_patternElevation[i] * weight;
        result.angularHeight       += u_patternAngularHeight[i] * weight;
    }

    // How strongly ANY weather actually occupies this direction, saturating
    // at 1 so overlapping patterns never over-brighten.
    float presence = clamp(totalWeight, 0.0, 1.0);

    // Safe divide — when totalWeight is exactly 0 every accumulated field
    // is also exactly 0.
    float invWeight = totalWeight > 0.0 ? (1.0 / totalWeight) : 0.0;

    result.coverage            *= invWeight;
    result.color                *= invWeight;
    result.topColor             *= invWeight;
    result.shadowColor          *= invWeight;
    result.density               *= invWeight;
    result.shadeStrength        *= invWeight;
    result.rimStrength          *= invWeight;
    result.aoStrength           *= invWeight;
    result.brightness           *= invWeight;
    result.toonBands            *= invWeight;
    result.densityNoiseScale    *= invWeight;
    result.noiseWarpStrength    *= invWeight;
    result.coverageBias         *= invWeight;
    result.silhouetteSoftness   *= invWeight;
    result.intensity            *= invWeight;
    result.elevation            *= invWeight;
    result.angularHeight        *= invWeight;

    // Shape/color/position fields stay the pure weighted blend — they
    // describe WHAT nearby weather looks like and WHERE it actually sits.
    // coverage/density/intensity describe HOW MUCH of it is present in
    // this direction, so those get the presence scale.
    result.coverage  *= presence;
    result.density    *= presence;
    result.intensity  *= presence;

    return result;
}

vec4 calculateClouds(vec3 dir, float dailySeed) {
    if (dir.y < SKY_HORIZON_FADE_START)
    return vec4(0.0);

    // Nothing any active, cloudy pattern's own footprint could possibly
    // reach — skip the whole pattern loop and raymarch.
    if (dir.y > u_skyElevationLimit)
    return vec4(0.0);

    float horizonFade = smoothstep(SKY_HORIZON_FADE_START, SKY_HORIZON_FADE_END, dir.y);
    float rayElevation = asin(clamp(dir.y, -1.0, 1.0));

    WeatherPatternSample weather = resolvePatternWeather(dir, rayElevation);

    if (weather.coverage <= 0.001 && weather.density <= 0.001)
    return vec4(0.0);

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

    // heightT is this ray-sample's own position within THIS blended
    // pattern's real vertical span — 0 at its lowest edge, 1 at its
    // highest — never the raw view angle. A storm's own base/erosion
    // shape now tracks where it actually sits in the sky.
    float angularSpan = max(weather.angularHeight, SKY_PATTERN_MIN_ANGULAR_HEIGHT);
    float heightT = clamp(0.5 + (rayElevation - weather.elevation) / (2.0 * angularSpan), 0.0, 1.0);

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