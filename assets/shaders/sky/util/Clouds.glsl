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
* Sky-dome weather preview. Each ray direction is tested against every
 * active pattern in SkyWeatherPatternData and resolves to at most one
 * dominant pattern — never a blend of several, since averaging two unrelated
 * patterns' positions and seeds together produces a virtual cloud that
 * belongs to neither. The winning pattern is raymarched through its own
 * real world-space box, the same box the overhead volumetric layer streams
 * around, so the two visual layers always agree on where weather actually
 * is. Bearing/elevation/center/halfExtent/rotation all come from
 * SkyWeatherPatternBranch, derived from the same lobes the overhead system
 * renders.
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

const float SKY_PATTERN_WEIGHT_INNER_SCALE  = 0.55;
const float SKY_PATTERN_WEIGHT_OUTER_SCALE  = 1.5;
const float SKY_PATTERN_MIN_ANGULAR_WIDTH   = 0.05;
const float SKY_PATTERN_MIN_ANGULAR_HEIGHT  = 0.03;
const float SKY_PATTERN_AZIMUTH_EPSILON     = 0.0005;
const float SKY_PI                          = 3.14159265359;
const float SKY_TWO_PI                      = 6.28318530718;

float wrapAngleDelta(float delta) {
    return mod(delta + SKY_PI, SKY_TWO_PI) - SKY_PI;
}

/*
* Scans every active pattern and returns the index of whichever one this
 * ray sits most centrally inside, plus how strongly (0 at its own edge, up
 * to 1 at its own center) — used purely as a final opacity multiplier, never
 * to mix fields between patterns.
 */
int findDominantPattern(vec3 dir, float rayElevation, out float weight) {
    weight = 0.0;
    int best = -1;

    float horizLen = length(dir.xz);
    float rayAzimuth = horizLen > SKY_PATTERN_AZIMUTH_EPSILON ? atan(dir.x, -dir.z) : 0.0;

    for (int i = 0; i < u_patternCount; i++) {
        float fade = clamp(u_patternFadeAlpha[i], 0.0, 1.0);

        if (fade <= 0.0)
        continue;

        float elevEdge = max(u_patternAngularHeight[i], SKY_PATTERN_MIN_ANGULAR_HEIGHT);
        float elevDiff = abs(rayElevation - u_patternElevation[i]);

        if (elevDiff > elevEdge * SKY_PATTERN_WEIGHT_OUTER_SCALE)
        continue;

        float azimuthEdge = max(u_patternAngularWidth[i], SKY_PATTERN_MIN_ANGULAR_WIDTH);
        float azimuthDiff = abs(wrapAngleDelta(rayAzimuth - u_patternBearing[i]));

        if (azimuthDiff > azimuthEdge * SKY_PATTERN_WEIGHT_OUTER_SCALE)
        continue;

        float azimuthWeight = 1.0 - smoothstep(
            azimuthEdge * SKY_PATTERN_WEIGHT_INNER_SCALE,
            azimuthEdge * SKY_PATTERN_WEIGHT_OUTER_SCALE,
            azimuthDiff);

        float elevationWeight = 1.0 - smoothstep(
            elevEdge * SKY_PATTERN_WEIGHT_INNER_SCALE,
            elevEdge * SKY_PATTERN_WEIGHT_OUTER_SCALE,
            elevDiff);

        float candidateWeight = azimuthWeight * elevationWeight * fade;

        if (candidateWeight > weight) {
            weight = candidateWeight;
            best = i;
        }
    }

    return best;
}

vec4 calculateClouds(vec3 dir, float dailySeed) {
    if (dir.y < SKY_HORIZON_FADE_START)
    return vec4(0.0);

    if (dir.y > u_skyElevationLimit)
    return vec4(0.0);

    float rayElevation = asin(clamp(dir.y, -1.0, 1.0));

    float patternWeight;
    int index = findDominantPattern(dir, rayElevation, patternWeight);

    if (index < 0 || patternWeight <= 0.001)
    return vec4(0.0);

    if (u_patternCoverage[index] <= 0.001 && u_patternDensity[index] <= 0.001)
    return vec4(0.0);

    vec3 center = u_patternCenter[index];
    vec3 halfExtent = u_patternHalfExtent[index];
    float rotation = u_patternDomainRotation[index];
    vec2 rot = vec2(cos(rotation), sin(rotation));

    vec3 cameraRenderPos = (u_inverseView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;

    vec2 boxHit = intersectSkyOBB(cameraRenderPos, dir, center, rot, halfExtent);
    float marchStart = max(boxHit.x, 0.0);
    float marchLen = max(boxHit.y - marchStart, 0.0);

    if (marchLen <= 0.001)
    return vec4(0.0);

    float horizonFade = smoothstep(SKY_HORIZON_FADE_START, SKY_HORIZON_FADE_END, dir.y);

    float sunWeight  = clamp(u_sunIntensity / 0.3, 0.0, 1.0);
    vec3  lightDir   = normalize(mix(u_moonDirection, u_sunDirection, sunWeight));
    vec3  lightColor = mix(u_moonColor, u_sunColor, sunWeight);
    float lightPower = mix(u_moonIntensity, u_sunIntensity, sunWeight);

    vec3 windOffset = vec3(
        u_windDriftOffset.x + dailySeed * 4.0,
        0.0,
        u_windDriftOffset.y + dailySeed * 3.0);

    vec3 topTint    = mix(u_patternTopColor[index], u_skyZenithColor, SKY_TOP_TINT_MIX);
    vec3 shadowTint = mix(u_patternShadowColor[index], u_skyHorizonColor * 0.4, SKY_SHADOW_TINT_MIX);

    float coverageBias = clamp(mix(u_patternCoverageBias[index], u_patternCoverage[index], 0.5), 0.0, 1.0);
    float intensityFactor = mix(SKY_INTENSITY_DENSITY_FLOOR, 1.0, clamp(u_patternIntensity[index], 0.0, 1.0));
    int   toonBands = max(int(u_patternToonBands[index] + 0.5), 1);
    float noiseScale = u_patternDensityNoiseScale[index] * SKY_NOISE_WORLD_SCALE;
    float warpStrength = u_patternNoiseWarpStrength[index];
    float silhouetteSoftness = u_patternSilhouetteSoftness[index];
    float seed = u_patternSeed[index];
    float density0 = u_patternDensity[index];
    float boxHeight = max(halfExtent.y * 2.0, 0.001);

    float stepSize = marchLen / float(SKY_RAYMARCH_STEPS);
    vec4 accum = vec4(0.0);

    for (int i = 0; i < SKY_RAYMARCH_STEPS; i++) {
        if (accum.a > 0.95)
        break;

        vec3 p = cameraRenderPos + dir * (marchStart + (float(i) + 0.5) * stepSize) + windOffset;
        vec3 rel = p - center;
        vec2 localXZ = worldToLocalXZ(rel.xz, rot);
        float heightT = clamp((rel.y + halfExtent.y) / boxHeight, 0.0, 1.0);

        float density = sampleSkyCloudDensity(
            vec3(localXZ.x, rel.y, localXZ.y), heightT, noiseScale, warpStrength,
            coverageBias, silhouetteSoftness, seed, u_time) * density0 * intensityFactor;

        if (density > 0.01) {
            vec3 litRel = (p + lightDir * SKY_LIGHT_TAP_DISTANCE) - center;
            vec2 litLocalXZ = worldToLocalXZ(litRel.xz, rot);
            float litHeightT = clamp((litRel.y + halfExtent.y) / boxHeight, 0.0, 1.0);

            float litDensity = sampleSkyCloudDensity(
                vec3(litLocalXZ.x, litRel.y, litLocalXZ.y), litHeightT, noiseScale, warpStrength,
                coverageBias, silhouetteSoftness, seed, u_time) * density0 * intensityFactor;

            float lightLift = clamp((density - litDensity) * 2.0 + 0.5, 0.0, 1.0);

            vec3 shaded = shadeSkyCloud(
                u_patternColor[index], topTint, shadowTint, lightColor, lightPower,
                heightT, lightLift, toonBands,
                u_patternShadeStrength[index], u_patternRimLightStrength[index],
                u_patternAmbientOcclusionStrength[index], u_patternBrightnessMultiplier[index]);

            float stepAlpha = clamp(density * SKY_STEP_ALPHA_SCALE * stepSize, 0.0, 1.0);
            accum.rgb += (1.0 - accum.a) * stepAlpha * shaded;
            accum.a   += (1.0 - accum.a) * stepAlpha;
        }
    }

    accum.a *= horizonFade * patternWeight;

    return accum;
}

#endif