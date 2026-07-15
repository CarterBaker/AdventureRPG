// Clouds.glsl — sky/util/Clouds.glsl
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

// Sky-dome distant weather. Every active pattern is raymarched as the exact
// world-space box the overhead volumetric system itself streams real cloud
// objects within, so the sky shows weather as if it were genuinely out
// there rather than a blended stand-in. Patterns composite in array order —
// two distinct systems sharing a ray is rare enough at this scale that
// depth-sorting 64 boxes per pixel isn't worth the cost.

const float HORIZON_FADE_START     = -0.03;
const float HORIZON_FADE_END       = 0.05;
const float ELEVATION_LIMIT_MARGIN = 0.12;

const float LIGHT_TAP_DISTANCE    = 24.0;
const float STEP_ALPHA_SCALE      = 0.05;
const float INTENSITY_FLOOR       = 0.3;
const int   MIN_STEPS_PER_PATTERN = 10;
const int   MAX_STEPS_PER_PATTERN = 28;
const float DESIRED_SAMPLES       = 22.0;
const float MIN_STEP_SIZE         = 6.0;
const float MAX_STEP_SIZE         = 40.0;

vec4 marchSkyPattern(
    int i,
    vec3 rayOrigin,
    vec3 rayDir,
    vec3 lightDir,
    vec3 lightColor,
    float lightPower,
    vec3 skyAmbient) {
    float fade = clamp(u_patternFadeAlpha[i], 0.0, 1.0);

    if (fade <= 0.001 || u_patternDensity[i] <= 0.001 || u_patternCoverage[i] <= 0.001)
    return vec4(0.0);

    vec3 halfExtent = u_patternHalfExtent[i];
    vec3 boxMin = u_patternCenter[i] - halfExtent;
    vec3 boxMax = u_patternCenter[i] + halfExtent;

    vec2 hit = intersectSkyCloudBox(rayOrigin, rayDir, boxMin, boxMax);
    float marchStart = max(hit.x, 0.0);
    float marchLen = hit.y - marchStart;

    if (marchLen <= 0.5)
    return vec4(0.0);

    float coverageBlend = clamp(mix(u_patternCoverageBias[i], u_patternCoverage[i], 0.5), 0.0, 1.0);
    float intensityFactor = mix(INTENSITY_FLOOR, 1.0, clamp(u_patternIntensity[i], 0.0, 1.0));

    float rawStep = clamp(marchLen / DESIRED_SAMPLES, MIN_STEP_SIZE, MAX_STEP_SIZE);
    int steps = clamp(int(marchLen / rawStep), MIN_STEPS_PER_PATTERN, MAX_STEPS_PER_PATTERN);
    float stepSize = marchLen / float(steps);

    float boxHeight = max(halfExtent.y * 2.0, 0.001);
    vec3 windOffset = vec3(u_windDriftOffset.x, 0.0, u_windDriftOffset.y);

    vec4 accum = vec4(0.0);

    for (int s = 0; s < MAX_STEPS_PER_PATTERN; s++) {
        if (s >= steps || accum.a > 0.96)
        break;

        float t = marchStart + (float(s) + 0.5) * stepSize;
        vec3 p = rayOrigin + rayDir * t + windOffset;

        float heightT = clamp((p.y - boxMin.y) / boxHeight, 0.0, 1.0);

        float density = sampleSkyCloudDensity(
            p, u_patternCenter[i], halfExtent, heightT,
            u_patternDensityNoiseScale[i], u_patternNoiseWarpStrength[i],
            coverageBlend, u_patternSilhouetteSoftness[i],
            u_patternSeed[i], u_patternDomainRotation[i], u_time)
        * u_patternDensity[i] * intensityFactor;

        if (density <= 0.01)
        continue;

        float litDensity = sampleSkyCloudDensity(
            p + lightDir * LIGHT_TAP_DISTANCE, u_patternCenter[i], halfExtent, heightT,
            u_patternDensityNoiseScale[i], u_patternNoiseWarpStrength[i],
            coverageBlend, u_patternSilhouetteSoftness[i],
            u_patternSeed[i], u_patternDomainRotation[i], u_time)
        * u_patternDensity[i] * intensityFactor;

        float lightLift = clamp((density - litDensity) * 2.4 + 0.5, 0.0, 1.0);

        vec3 shaded = shadeSkyCloud(
            u_patternColor[i], u_patternTopColor[i], u_patternShadowColor[i],
            skyAmbient, lightColor, lightPower,
            heightT, lightLift, density, u_patternToonBands[i],
            u_patternShadeStrength[i], u_patternRimLightStrength[i],
            u_patternAmbientOcclusionStrength[i], u_patternBrightnessMultiplier[i]);

        float stepAlpha = clamp(1.0 - exp(-density * STEP_ALPHA_SCALE * stepSize), 0.0, 1.0);
        accum.rgb += (1.0 - accum.a) * stepAlpha * shaded;
        accum.a   += (1.0 - accum.a) * stepAlpha;
    }

    accum.a *= fade;
    return accum;
}

vec4 calculateClouds(vec3 dir) {
    if (dir.y < HORIZON_FADE_START)
    return vec4(0.0);

    float horizonFade = smoothstep(HORIZON_FADE_START, HORIZON_FADE_END, dir.y);
    float elevationFade = 1.0 - smoothstep(u_skyElevationLimit, u_skyElevationLimit + ELEVATION_LIMIT_MARGIN, dir.y);

    if (horizonFade <= 0.001 || elevationFade <= 0.001)
    return vec4(0.0);

    vec3 cameraRenderPos = (u_inverseView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;

    float sunWeight  = clamp(u_sunIntensity / 0.3, 0.0, 1.0);
    vec3  lightDir   = normalize(mix(u_moonDirection, u_sunDirection, sunWeight));
    vec3  lightColor = mix(u_moonColor, u_sunColor, sunWeight);
    float lightPower = mix(u_moonIntensity, u_sunIntensity, sunWeight);
    vec3  skyAmbient = mix(u_skyHorizonColor, u_skyZenithColor, clamp(dir.y * 0.5 + 0.5, 0.0, 1.0));

    vec4 accum = vec4(0.0);

    for (int i = 0; i < u_patternCount; i++) {
        if (accum.a > 0.97)
        break;

        vec4 result = marchSkyPattern(i, cameraRenderPos, dir, lightDir, lightColor, lightPower, skyAmbient);

        if (result.a <= 0.0)
        continue;

        accum.rgb += (1.0 - accum.a) * result.rgb;
        accum.a   += (1.0 - accum.a) * result.a;
    }

    accum.a *= horizonFade * elevationFade;

    return accum;
}

#endif