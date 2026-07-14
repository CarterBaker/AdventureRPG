#version 330 core

in vec3  vWorldPos;
in float vRandomSeed;
in float vFadeAlpha;
in float vIntensity;
flat in vec3 vBoxCenter;
flat in vec3 vHalfExtent;
flat in vec2 vRot;

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
* Raymarches this instance's own oriented box in local space. Shape comes
 * from VolumetricCloudUtility's ellipsoid-bounded noise field; this shader
 * owns step sizing, the light tap, fake sky/rim/powder shading, and final
 * compositing into the G-buffer. gl_FragDepth is written from the
 * raymarch's own peak surface point rather than the box's flat entry face,
 * so overlapping cloud instances occlude each other along their actual
 * rounded silhouettes instead of along a hard box plane.
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

const float CLOUD_STEP_SIZE_NEAR     = 5.0;
const float CLOUD_STEP_SIZE_FAR      = 14.0;
const float CLOUD_TIER_NEAR          = 60.0;
const float CLOUD_TIER_FAR           = 220.0;
const int   CLOUD_MIN_STEPS          = 12;
const int   CLOUD_MAX_STEPS          = 48;
const float CLOUD_STEP_ALPHA_SCALE   = 0.11;
const float CLOUD_LIGHT_TAP_DISTANCE = 3.0;
const float CLOUD_RIM_FRESNEL_POWER  = 2.2;
const float CLOUD_INTENSITY_FLOOR    = 0.4;

void main() {
    if (vFadeAlpha <= 0.001)
    discard;

    vec3 cameraRenderPos = (u_inverseView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;
    vec3 rayDir = normalize(vWorldPos - cameraRenderPos);

    vec2 boxHit = intersectCloudOBB(cameraRenderPos, rayDir, vBoxCenter, vRot, vHalfExtent);
    float marchStart = max(boxHit.x, 0.0);
    float marchLen   = max(boxHit.y - marchStart, 0.0);

    if (marchLen <= 0.001)
    discard;

    float camDist = length(vWorldPos - cameraRenderPos);
    float thicknessStep = clamp(vHalfExtent.y * 2.0 / 6.0, 2.0, 9.0);
    float distanceStep = mix(CLOUD_STEP_SIZE_NEAR, CLOUD_STEP_SIZE_FAR,
        smoothstep(CLOUD_TIER_NEAR, CLOUD_TIER_FAR, camDist));
    float targetStepSize = max(thicknessStep, distanceStep);

    int steps = clamp(int(marchLen / targetStepSize), CLOUD_MIN_STEPS, CLOUD_MAX_STEPS);
    float stepSize = marchLen / float(steps);

    float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453123);

    float sunWeight = clamp(u_sunIntensity / 0.3, 0.0, 1.0);
    vec3  lightDir  = normalize(mix(u_moonDirection, u_sunDirection, sunWeight));

    float skyAltitude = clamp(rayDir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3  skyAmbient  = mix(u_skyHorizonColor, u_skyZenithColor, skyAltitude);

    float intensityFactor = mix(CLOUD_INTENSITY_FLOOR, 1.0, clamp(vIntensity, 0.0, 1.0));
    float boxHeight = max(vHalfExtent.y * 2.0, 0.0001);
    float baseY = vBoxCenter.y - vHalfExtent.y;
    float gradientEpsilon = max(min(vHalfExtent.x, vHalfExtent.z) * 0.05, 0.06);

    vec4  accum   = vec4(0.0);
    float accumAO = 0.0;

    vec3  peakPos          = vWorldPos;
    float peakContribution = 0.0;

    for (int i = 0; i < CLOUD_MAX_STEPS; i++) {
        if (i >= steps)
        break;

        if (accum.a > 0.97)
        break;

        vec3 p = cameraRenderPos + rayDir * (marchStart + (float(i) + dither) * stepSize);

        float heightT = clamp((p.y - baseY) / boxHeight, 0.0, 1.0);

        float rawDensity = sampleVolumetricCloudDensity(
            p, vBoxCenter, vRot, vHalfExtent, heightT,
            u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength,
            u_cloudCoverageBias, u_cloudSilhouetteSoftness,
            vRandomSeed, u_time);
        float density = rawDensity * u_cloudDensity * intensityFactor;

        if (density > 0.01) {
            float rawLit = sampleVolumetricCloudDensity(
                p + lightDir * CLOUD_LIGHT_TAP_DISTANCE, vBoxCenter, vRot, vHalfExtent, heightT,
                u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength,
                u_cloudCoverageBias, u_cloudSilhouetteSoftness,
                vRandomSeed, u_time);
            float litDensity = rawLit * u_cloudDensity * intensityFactor;

            float lightLift = clamp((density - litDensity) * 2.2 + 0.5, 0.0, 1.0);
            float powder = 1.0 - exp(-density * 2.4);

            float stepAO;
            vec3 shaded = shadeCloudFluffy(
                u_cloudColor, u_cloudTopColor, u_cloudShadowColor, skyAmbient,
                heightT, lightLift, density, powder,
                u_cloudToonBands, u_cloudShadeStrength,
                u_cloudAmbientOcclusionStrength, u_cloudBrightnessMultiplier,
                stepAO);

            float stepAlpha = clamp(density * CLOUD_STEP_ALPHA_SCALE * stepSize, 0.0, 1.0);
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

    vec3 cloudNormalWorld = volumetricCloudGradientNormal(
        peakPos, vBoxCenter, vRot, vHalfExtent,
        u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength,
        u_cloudCoverageBias, u_cloudSilhouetteSoftness,
        vRandomSeed, u_time, gradientEpsilon);

    float rim = pow(1.0 - clamp(dot(-rayDir, cloudNormalWorld), 0.0, 1.0), CLOUD_RIM_FRESNEL_POWER) * u_cloudRimLightStrength;
    vec3  rimmed  = accum.rgb + skyAmbient * rim * finalAlpha;
    vec3  blended = mix(skyAmbient, rimmed, finalAlpha);

    float ao = mix(1.0, accumAO / max(accum.a, 0.0001), finalAlpha);

    vec3 normalView = normalize(mat3(u_view) * cloudNormalWorld);

    vec4 peakClip = u_viewProjection * vec4(peakPos, 1.0);
    gl_FragDepth = clamp(peakClip.z / peakClip.w * 0.5 + 0.5, 0.0, 1.0);

    gAlbedo   = vec4(blended, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(0.0, 0.0, ao, 1.0);
}