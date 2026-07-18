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
* Sky-dome distant weather preview. Every active cloud lobe is its own real
 * oriented box in world space — the same box the overhead volumetric system
 * builds — raymarched independently and composited front-to-back. Only
 * lobes inside the horizon ring (see SkyWeatherPatternBranch) ever reach
 * this UBO, so this pass never overlaps the physically-instanced clouds
 * the overhead system draws close to the player.
 * Output is premultiplied (rgb already alpha-weighted) — consumers must
 * add rgb directly rather than re-blending it against alpha a second time.
 */

const float HORIZON_FADE_START = -0.02;
const float HORIZON_FADE_END   = 0.06;

const int   MAX_RAYMARCH_STEPS = 28;
const int   MIN_RAYMARCH_STEPS = 6;
const float DESIRED_SAMPLE_COUNT = 18.0;
const float MIN_STEP_SIZE = 10.0;
const float MAX_STEP_SIZE = 80.0;
const float STEP_ALPHA_SCALE = 0.05;
const float LIGHT_TAP_DISTANCE = 40.0;
const float SKY_CLOUD_NOISE_WORLD_SCALE = 1.0 / 220.0;

vec4 marchSkyCloud(int index, vec3 rayOrigin, vec3 rayDir, vec3 lightDir, vec3 lightColor, float lightPower) {
    vec3 boxCenter = u_cloudCenter[index];
    vec3 halfExtent = u_cloudHalfExtent[index];
    float rotation = u_cloudDomainRotation[index];
    vec2 rot = vec2(cos(rotation), sin(rotation));

    vec2 boxHit = intersectSkyCloudBox(rayOrigin, rayDir, boxCenter, rot, halfExtent);
    float marchStart = max(boxHit.x, 0.0);
    float marchLen = max(boxHit.y - marchStart, 0.0);

    if (marchLen <= 0.001)
    return vec4(0.0);

    // Visibility scales directly off this lobe's own fade alpha (which
    // already folds in the horizon-ring edge fade) and its pattern's live
    // intensity — no artificial floor.
    float visibility = u_cloudFadeAlpha[index] * clamp(u_cloudIntensity[index], 0.0, 1.0);

    if (visibility <= 0.001)
    return vec4(0.0);

    float rawStepSize = clamp(marchLen / DESIRED_SAMPLE_COUNT, MIN_STEP_SIZE, MAX_STEP_SIZE);
    int steps = clamp(int(marchLen / rawStepSize), MIN_RAYMARCH_STEPS, MAX_RAYMARCH_STEPS);
    float stepSize = marchLen / float(steps);

    float boxHeight = max(halfExtent.y * 2.0, 0.001);
    float baseY = boxCenter.y - halfExtent.y;

    float seed = u_cloudSeed[index];
    float noiseScale = u_cloudDensityNoiseScale[index] * SKY_CLOUD_NOISE_WORLD_SCALE;
    float warpStrength = u_cloudNoiseWarpStrength[index];
    float coverageBias = u_cloudCoverageBias[index];
    float softness = u_cloudSilhouetteSoftness[index];
    float densityScale = u_cloudDensity[index];

    vec4 accum = vec4(0.0);

    for (int i = 0; i < MAX_RAYMARCH_STEPS; i++) {
        if (i >= steps || accum.a > 0.96)
        break;

        float t = marchStart + (float(i) + 0.5) * stepSize;
        vec3 p = rayOrigin + rayDir * t + vec3(u_windDriftOffset.x, 0.0, u_windDriftOffset.y);

        float heightT = clamp((p.y - baseY) / boxHeight, 0.0, 1.0);

        float density = sampleSkyCloudDensity(
            p, boxCenter, rot, halfExtent, heightT,
            noiseScale, warpStrength, coverageBias, softness, seed, u_time) * densityScale;

        if (density > 0.01) {
            float litDensity = sampleSkyCloudDensity(
                p + lightDir * LIGHT_TAP_DISTANCE, boxCenter, rot, halfExtent, heightT,
                noiseScale, warpStrength, coverageBias, softness, seed, u_time) * densityScale;

            float lightLift = clamp((density - litDensity) * 2.0 + 0.5, 0.0, 1.0);

            vec3 shaded = shadeSkyCloudLit(u_cloudColor[index], lightColor, lightPower, heightT, lightLift);

            float stepAlpha = clamp(1.0 - exp(-density * STEP_ALPHA_SCALE * stepSize), 0.0, 1.0);
            accum.rgb += (1.0 - accum.a) * stepAlpha * shaded;
            accum.a   += (1.0 - accum.a) * stepAlpha;
        }
    }

    // Both channels scaled together so a low-visibility lobe contributes
    // proportionally less of its color AND its coverage to the composite —
    // scaling alpha alone left full-brightness color bleeding through even
    // when a lobe was meant to read as nearly invisible.
    accum.rgb *= visibility;
    accum.a   *= visibility;

    return accum;
}

vec4 calculateClouds(vec3 dir) {
    if (dir.y < HORIZON_FADE_START)
    return vec4(0.0);

    float horizonFade = smoothstep(HORIZON_FADE_START, HORIZON_FADE_END, dir.y);
    float elevationFade = 1.0 - smoothstep(u_skyElevationFadeStart, u_skyElevationLimit, dir.y);
    float edgeFade = horizonFade * elevationFade;

    if (edgeFade <= 0.0005)
    return vec4(0.0);

    vec3 cameraRenderPos = (u_inverseView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;

    float sunWeight = clamp(u_sunIntensity / 0.3, 0.0, 1.0);
    vec3 lightDir = normalize(mix(u_moonDirection, u_sunDirection, sunWeight));
    vec3 lightColor = mix(u_moonColor, u_sunColor, sunWeight);
    float lightPower = mix(u_moonIntensity, u_sunIntensity, sunWeight);

    vec4 accum = vec4(0.0);

    for (int i = 0; i < u_cloudCount; i++) {
        if (accum.a > 0.97)
        break;

        vec4 contribution = marchSkyCloud(i, cameraRenderPos, dir, lightDir, lightColor, lightPower);

        if (contribution.a <= 0.0005)
        continue;

        accum.rgb += (1.0 - accum.a) * contribution.rgb;
        accum.a   += (1.0 - accum.a) * contribution.a;
    }

    accum.rgb *= edgeFade;
    accum.a   *= edgeFade;

    return accum;
}

#endif