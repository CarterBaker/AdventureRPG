#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyWeatherPatternData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/WindData.glsl"
#include "sky/util/SkyCloudUtility.glsl"

/*
* Sky-dome distant weather preview. Every active pattern is blended, per
 * ray direction, out of the SkyWeatherPatternData UBO into one virtual
 * cloud layer, then raymarched entirely through SkyCloudUtility's
 * unbounded, world-scale density function. Never includes
 * clouds/util/VolumetricCloudUtility.glsl — that module belongs solely to
 * the overhead volumetric cloud objects. The pattern's real world-space
 * box (center / halfExtent) is used only to bound the raymarch's
 * start/end distance along the ray — never to shape or orient the noise.
 */

const float SKY_HORIZON_FADE_START = -0.02;
const float SKY_HORIZON_FADE_END   = 0.06;

const int   SKY_RAYMARCH_STEPS          = 20;
const float SKY_STEP_ALPHA_SCALE        = 0.11;
const float SKY_LIGHT_TAP_DISTANCE      = 28.0;
const float SKY_TOP_TINT_MIX            = 0.32;
const float SKY_SHADOW_TINT_MIX         = 0.28;
const float SKY_INTENSITY_DENSITY_FLOOR = 0.2;
const float SKY_NOISE_WORLD_SCALE       = 1.0 / 110.0;
const float SKY_DETAIL_JITTER           = 0.45;

const float SKY_PATTERN_WEIGHT_INNER_SCALE  = 0.6;
const float SKY_PATTERN_WEIGHT_OUTER_SCALE  = 1.6;
const float SKY_PATTERN_MIN_ANGULAR_WIDTH   = 0.05;
const float SKY_PATTERN_MIN_ANGULAR_HEIGHT  = 0.03;
const float SKY_PATTERN_AZIMUTH_EPSILON     = 0.0005;
const float SKY_PI                          = 3.14159265359;
const float SKY_TWO_PI                      = 6.28318530718;

struct SkyCloudVolume {
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
    vec3  center;
    vec3  halfExtent;
    float seed;
};

float wrapAngleDelta(float delta) {
    return mod(delta + SKY_PI, SKY_TWO_PI) - SKY_PI;
}

SkyCloudVolume resolveSkyCloudVolume(vec3 dir, float rayElevation) {
    SkyCloudVolume result;
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
    result.center = vec3(0.0);
    result.halfExtent = vec3(0.0);
    result.seed = 0.0;

    float horizLen = length(dir.xz);
    float rayAzimuth = horizLen > SKY_PATTERN_AZIMUTH_EPSILON
    ? atan(dir.x, -dir.z)
    : 0.0;

    float totalWeight = 0.0;

    for (int i = 0; i < u_patternCount; i++) {
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
        result.center              += u_patternCenter[i] * weight;
        result.halfExtent          += u_patternHalfExtent[i] * weight;
        result.seed                += u_patternSeed[i] * weight;
    }

    float presence = clamp(totalWeight, 0.0, 1.0);
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
    result.center                *= invWeight;
    result.halfExtent           *= invWeight;
    result.seed                  *= invWeight;

    result.coverage  *= presence;
    result.density    *= presence;
    result.intensity  *= presence;

    return result;
}

vec4 calculateClouds(vec3 dir, float dailySeed) {
    if (dir.y < SKY_HORIZON_FADE_START)
    return vec4(0.0);

    if (dir.y > u_skyElevationLimit)
    return vec4(0.0);

    float horizonFade = smoothstep(SKY_HORIZON_FADE_START, SKY_HORIZON_FADE_END, dir.y);
    float rayElevation = asin(clamp(dir.y, -1.0, 1.0));

    SkyCloudVolume weather = resolveSkyCloudVolume(dir, rayElevation);

    if (weather.coverage <= 0.001 && weather.density <= 0.001)
    return vec4(0.0);

    vec3 boxMin = weather.center - weather.halfExtent;
    vec3 boxMax = weather.center + weather.halfExtent;

    vec3 cameraRenderPos = (u_inverseView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;

    vec2 boxHit = intersectSkyAABB(cameraRenderPos, dir, boxMin, boxMax);
    float marchStart = max(boxHit.x, 0.0);
    float marchLen   = max(boxHit.y - marchStart, 0.0);

    if (marchLen <= 0.001)
    return vec4(0.0);

    float sunWeight  = clamp(u_sunIntensity / 0.3, 0.0, 1.0);
    vec3  lightDir   = normalize(mix(u_moonDirection, u_sunDirection, sunWeight));
    vec3  lightColor = mix(u_moonColor, u_sunColor, sunWeight);
    float lightPower = mix(u_moonIntensity, u_sunIntensity, sunWeight);

    vec3 windOffset = vec3(
        u_windDriftOffset.x + dailySeed * 4.0,
        0.0,
        u_windDriftOffset.y + dailySeed * 3.0);

    vec3 topTint    = mix(weather.topColor, u_skyZenithColor, SKY_TOP_TINT_MIX);
    vec3 shadowTint = mix(weather.shadowColor, u_skyHorizonColor * 0.4, SKY_SHADOW_TINT_MIX);

    float effectiveCoverageBias = clamp(mix(weather.coverageBias, weather.coverage, 0.5), 0.0, 1.0);

    float boxHeight = max(boxMax.y - boxMin.y, 0.001);
    float intensityFactor = mix(SKY_INTENSITY_DENSITY_FLOOR, 1.0, clamp(weather.intensity, 0.0, 1.0));
    int toonBands = max(int(weather.toonBands + 0.5), 1);
    float noiseScale = weather.densityNoiseScale * SKY_NOISE_WORLD_SCALE;

    float stepSize = marchLen / float(SKY_RAYMARCH_STEPS);

    vec4 accum = vec4(0.0);

    for (int i = 0; i < SKY_RAYMARCH_STEPS; i++) {
        if (accum.a > 0.95)
        break;

        vec3 p = cameraRenderPos + dir * (marchStart + (float(i) + 0.5) * stepSize) + windOffset;

        float heightT = clamp((p.y - boxMin.y) / boxHeight, 0.0, 1.0);

        float density = sampleSkyCloudDensity(
            p, heightT, noiseScale, weather.noiseWarpStrength, SKY_DETAIL_JITTER,
            effectiveCoverageBias, weather.silhouetteSoftness, weather.seed, u_time)
        * weather.density * intensityFactor;

        if (density > 0.01) {
            float litDensity = sampleSkyCloudDensity(
                p + lightDir * SKY_LIGHT_TAP_DISTANCE, heightT, noiseScale, weather.noiseWarpStrength,
                SKY_DETAIL_JITTER, effectiveCoverageBias, weather.silhouetteSoftness, weather.seed, u_time)
            * weather.density * intensityFactor;
            float lightLift = clamp((density - litDensity) * 2.0 + 0.5, 0.0, 1.0);

            vec3 shaded = shadeSkyCloudLit(
                weather.color, topTint, shadowTint, lightColor, lightPower,
                heightT, lightLift, density, toonBands,
                weather.shadeStrength, weather.rimStrength, weather.aoStrength, weather.brightness);

            float stepAlpha = clamp(density * SKY_STEP_ALPHA_SCALE * stepSize, 0.0, 1.0);
            accum.rgb += (1.0 - accum.a) * stepAlpha * shaded;
            accum.a   += (1.0 - accum.a) * stepAlpha;
        }
    }

    accum.a *= horizonFade;

    return accum;
}

#endif