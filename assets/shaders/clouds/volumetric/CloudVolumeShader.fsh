#version 330 core

in vec3  vWorldPos;
in float vRandomSeed;
in float vDomainRotation;
in float vFadeAlpha;
in float vIntensity;
flat in vec3 vBoxMin;
flat in vec3 vBoxMax;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "clouds/util/VolumetricCloudUtility.glsl"

/*
* Raymarches this instance's own AABB (vBoxMin/vBoxMax) via
 * sampleVolumetricCloudDensity(). Writes unlit shape data
 * (shadeCloudUnlit()) plus a real accumulated AO into gMaterial.b —
 * Lighting.fsh lights this exactly once, same as terrain.
 *
 * Ray origin is the CAMERA, not this fragment's own surface position. This
 * box gets back-face culled (see RenderSystem) so only its near side
 * rasterizes when the camera is outside it, but using the camera as origin
 * regardless means the march region never depends on which triangle
 * triggered the fragment, and degrades correctly if the camera ends up
 * inside the box: marchStart just clamps to 0 and the march begins at the
 * camera instead of never reaching a wall it's already past.
 *
 * gNormal is the density field's own gradient at the raymarch's single
 * most-visible sample (see VolumetricCloudUtility.glsl's
 * volumetricCloudGradientNormal()), not this instance's flat box-face
 * normal — the box-face normal is what lit every cloud as six flat,
 * hard-edged panels once Lighting.fsh applied real directional light to it.
 */

uniform vec3  u_cloudColor;
uniform float u_cloudDensity;

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
    vec3 rayDir = normalize(vWorldPos - cameraRenderPos);

    vec2 boxHit = intersectAABB(cameraRenderPos, rayDir, vBoxMin, vBoxMax);
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
    float boxHeight       = max(vBoxMax.y - vBoxMin.y, 0.001);
    float gradientEpsilon = max(length(vBoxMax - vBoxMin) * 0.01, 0.05);

    vec4  accum   = vec4(0.0);
    float accumAO = 0.0;

    vec3  peakPos          = vWorldPos;
    float peakContribution = 0.0;

    for (int i = 0; i < CLOUD_RAYMARCH_STEPS_NEAR; i++) {
        if (i >= steps)
        break;

        if (accum.a > 0.97)
        break;

        vec3 p = cameraRenderPos + rayDir * (marchStart + (float(i) + 0.5) * stepSize);

        float heightT = clamp((p.y - vBoxMin.y) / boxHeight, 0.0, 1.0);

        float rawDensity = sampleVolumetricCloudDensity(
            p, vBoxMin, vBoxMax, heightT,
            u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength,
            u_cloudCoverageBias, u_cloudSilhouetteSoftness,
            vRandomSeed, u_time, vDomainRotation);
        float density = rawDensity * u_cloudDensity * intensityFactor;

        if (density > 0.01) {
            float rawLit = sampleVolumetricCloudDensity(
                p + lightDir * CLOUD_LIGHT_TAP_DISTANCE, vBoxMin, vBoxMax, heightT,
                u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength,
                u_cloudCoverageBias, u_cloudSilhouetteSoftness,
                vRandomSeed, u_time, vDomainRotation);
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

            if (contribution > peakContribution) {
                peakContribution = contribution;
                peakPos = p;
            }

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

    vec3 cloudNormalWorld = volumetricCloudGradientNormal(
        peakPos, vBoxMin, vBoxMax,
        u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength,
        u_cloudCoverageBias, u_cloudSilhouetteSoftness,
        vRandomSeed, u_time, vDomainRotation, gradientEpsilon);

    vec3 normalView = normalize(mat3(u_view) * cloudNormalWorld);

    gAlbedo   = vec4(blended, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(0.0, 0.0, ao, 1.0);
}