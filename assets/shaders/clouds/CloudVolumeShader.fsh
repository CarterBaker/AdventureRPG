#version 330 core

in vec3  vWorldPos;
in vec3  vNormal;
in float vRandomSeed;
in float vFadeAlpha;
in float vIntensity;
flat in vec3 vBoxMin;
flat in vec3 vBoxMax;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

#include "includes/CameraData.glsl"
#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "sky/util/CloudShapeUtility.glsl"

/*
* Volumetric raymarch through this instance's own AABB (vBoxMin/vBoxMax),
 * sampling the exact same sampleCloudDensity() the sky dome uses. Writes
 * only UNLIT shape data (shadeCloudUnlit()) plus a real accumulated AO into
 * gMaterial.b — Lighting.fsh lights this exactly once, same as terrain.
 *
 * CAMERA POSITION FIX: rayDir/camDist previously differenced vWorldPos
 * against u_cameraPosition directly. vWorldPos is expressed in the
 * player-chunk-recentered frame (see CloudVolumeShader.vsh's own doc
 * comment) — u_cameraPosition is not guaranteed to be, and LightingShader.fsh
 * deliberately never makes that assumption elsewhere in this engine. Both
 * uses now reconstruct the camera's position in the SAME frame via
 * `(u_inverseView * vec4(0,0,0,1)).xyz` — the identical technique
 * surface/includes/Height.glsl documents — removing any dependency on
 * u_cameraPosition sharing this shader's coordinate space.
 */

uniform vec3  u_cloudColor;
uniform float u_cloudDensity;
uniform float u_cloudEdgeSoftness;
uniform float u_cloudPuffJitter;

uniform vec3  u_cloudTopColor;
uniform int   u_cloudToonBands;
uniform float u_cloudDensityNoiseScale;
uniform float u_cloudNoiseWarpStrength;
uniform float u_cloudCoverageBias;
uniform float u_cloudSilhouetteSoftness;
uniform vec3  u_cloudShadowColor;
uniform float u_cloudShadeStrength;
uniform float u_cloudRimLightStrength;
uniform float u_cloudAmbientOcclusionStrength;
uniform float u_cloudBrightnessMultiplier;

const int   CLOUD_RAYMARCH_STEPS_NEAR       = 48;
const int   CLOUD_RAYMARCH_STEPS_FAR        = 16;
const float CLOUD_RAYMARCH_TIER_DISTANCE    = 128.0;
const float CLOUD_RAYMARCH_STEP_ALPHA_SCALE = 0.09;
const float CLOUD_LIGHT_TAP_DISTANCE        = 2.5;

const float INTENSITY_DENSITY_FLOOR = 0.4;

void main() {
    if (vFadeAlpha <= 0.001)
    discard;

    vec3 cameraRenderPos = (u_inverseView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;

    vec3 rayOrigin = vWorldPos;
    vec3 rayDir    = normalize(vWorldPos - cameraRenderPos);

    vec2 boxHit = intersectAABB(rayOrigin, rayDir, vBoxMin, vBoxMax);
    float marchStart = max(boxHit.x, 0.0);
    float marchLen   = max(boxHit.y - marchStart, 0.0);

    if (marchLen <= 0.001)
    discard;

    float camDist = length(vWorldPos - cameraRenderPos);
    int steps = camDist < CLOUD_RAYMARCH_TIER_DISTANCE ? CLOUD_RAYMARCH_STEPS_NEAR : CLOUD_RAYMARCH_STEPS_FAR;
    float stepSize = marchLen / float(steps);

    float sunWeight = clamp(u_sunIntensity / 0.3, 0.0, 1.0);
    vec3  lightDir  = normalize(mix(u_moonDirection, u_sunDirection, sunWeight));

    float intensityFactor = mix(INTENSITY_DENSITY_FLOOR, 1.0, clamp(vIntensity, 0.0, 1.0));
    float boxHeight        = max(vBoxMax.y - vBoxMin.y, 0.001);

    vec4  accum   = vec4(0.0);
    float accumAO = 0.0;

    for (int i = 0; i < CLOUD_RAYMARCH_STEPS_NEAR; i++) {
        if (i >= steps)
        break;

        if (accum.a > 0.97)
        break;

        vec3 p = rayOrigin + rayDir * (marchStart + (float(i) + 0.5) * stepSize);

        float heightT = clamp((p.y - vBoxMin.y) / boxHeight, 0.0, 1.0);

        float rawDensity = sampleCloudDensity(
            p, heightT, u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength, u_cloudPuffJitter,
            u_cloudCoverageBias, u_cloudSilhouetteSoftness, vRandomSeed, u_time);
        float density = rawDensity * u_cloudDensity * intensityFactor;

        if (density > 0.01) {
            float rawLit = sampleCloudDensity(
                p + lightDir * CLOUD_LIGHT_TAP_DISTANCE, heightT,
                u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength, u_cloudPuffJitter,
                u_cloudCoverageBias, u_cloudSilhouetteSoftness, vRandomSeed, u_time);
            float litDensity = rawLit * u_cloudDensity * intensityFactor;

            float lightLift = clamp((density - litDensity) * 2.0 + 0.5, 0.0, 1.0);

            float stepAO;
            vec3 shaded = shadeCloudUnlit(
                u_cloudColor, u_cloudTopColor, u_cloudShadowColor,
                heightT, lightLift, density,
                u_cloudToonBands, u_cloudShadeStrength,
                u_cloudAmbientOcclusionStrength, u_cloudBrightnessMultiplier,
                stepAO);

            float stepAlpha = clamp(density * CLOUD_RAYMARCH_STEP_ALPHA_SCALE * stepSize, 0.0, 1.0);
            float contribution = (1.0 - accum.a) * stepAlpha;

            accum.rgb += contribution * shaded;
            accumAO   += contribution * stepAO;
            accum.a   += contribution;
        }
    }

    float finalAlpha = clamp(accum.a * vFadeAlpha, 0.0, 1.0);

    if (finalAlpha <= 0.02)
    discard;

    float skyAltitude = clamp(rayDir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3  approxSky    = mix(u_skyHorizonColor, u_skyZenithColor, skyAltitude);
    vec3  blended      = mix(approxSky, accum.rgb, finalAlpha);

    float ao = mix(1.0, accumAO / max(accum.a, 0.0001), finalAlpha);

    vec3 normalView = normalize(mat3(u_view) * normalize(vNormal));

    gAlbedo   = vec4(blended, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(0.0, 0.0, ao, 1.0);
}