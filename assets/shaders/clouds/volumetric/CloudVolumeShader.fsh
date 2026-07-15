// CloudVolumeShader.fsh — clouds/volumetric/CloudVolumeShader.fsh
#version 330 core

in vec3  vWorldPos;
in float vRandomSeed;
in float vFadeAlpha;
in float vIntensity;
flat in vec3 vBoxCenter;
flat in vec3 vHalfExtent;
flat in vec2 vRot;
flat in float vDetailFactor;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/CloudSettingsData.glsl"
#include "clouds/util/VolumetricCloudUtility.glsl"

/*
* Raymarches this instance's oriented box and writes an unlit albedo, a
 * real density-gradient surface normal, and a self-shadow occlusion term
 * into the shared deferred G-buffer — the same three channels terrain
 * writes. Directional sun/moon response is deliberately left entirely to
 * the shared Lighting.fsh pass rather than computed here too — baking our
 * own sun response on top of what Lighting.fsh already applies to every
 * G-buffer pixel is what made clouds read as permanently dark regardless
 * of time of day. shadowColor/topColor/rim stay ours to apply since they
 * are archetype material properties, not a second light source.
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

const float CLOUD_STEP_SIZE_NEAR    = 4.0;
const float CLOUD_STEP_SIZE_FAR     = 12.0;
const float CLOUD_TIER_NEAR         = 60.0;
const float CLOUD_TIER_FAR          = 220.0;
const int   CLOUD_MIN_STEPS         = 14;
const int   CLOUD_MAX_STEPS         = 56;
const float CLOUD_EXTINCTION        = 0.07;
const float CLOUD_RIM_FRESNEL_POWER = 2.4;

void main() {
    if (vFadeAlpha <= 0.001)
    discard;

    vec3 cameraRenderPos = (u_inverseView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;
    vec3 rayDir = normalize(vWorldPos - cameraRenderPos);

    vec2 boxHit = intersectCloudBox(cameraRenderPos, rayDir, vBoxCenter, vRot, vHalfExtent);
    float marchStart = max(boxHit.x, 0.0);
    float marchLen = max(boxHit.y - marchStart, 0.0);

    if (marchLen <= 0.001)
    discard;

    float camDist = length(vWorldPos - cameraRenderPos);
    float thicknessStep = clamp(vHalfExtent.y * 2.0 / 5.0, 1.5, 8.0);
    float distanceStep = mix(CLOUD_STEP_SIZE_NEAR, CLOUD_STEP_SIZE_FAR,
        smoothstep(CLOUD_TIER_NEAR, CLOUD_TIER_FAR, camDist));
    float targetStepSize = max(thicknessStep, distanceStep);

    int steps = clamp(int(marchLen / targetStepSize), CLOUD_MIN_STEPS, CLOUD_MAX_STEPS);
    float stepSize = marchLen / float(steps);
    float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453123);

    float boxHeight = max(vHalfExtent.y * 2.0, 0.0001);
    float baseY = vBoxCenter.y - vHalfExtent.y;

    vec4  accum = vec4(0.0);
    float opticalDepth = 0.0;
    float transmittance = 1.0;

    vec3  peakPos = vWorldPos;
    float peakContribution = 0.0;

    for (int i = 0; i < CLOUD_MAX_STEPS; i++) {
        if (i >= steps || transmittance < 0.02)
        break;

        float t = marchStart + (float(i) + dither) * stepSize;
        vec3  p = cameraRenderPos + rayDir * t;

        float heightT = clamp((p.y - baseY) / boxHeight, 0.0, 1.0);

        float rawDensity = sampleCloudDensity(
            p, vBoxCenter, vRot, vHalfExtent, heightT,
            u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength,
            u_cloudCoverageBias, u_cloudSilhouetteSoftness,
            vDetailFactor, vRandomSeed, u_time);
        float density = rawDensity * u_cloudDensity * vIntensity;

        if (density > 0.01) {
            float stepTransmittance = exp(-density * CLOUD_EXTINCTION * stepSize);
            float stepAlpha = 1.0 - stepTransmittance;
            float contribution = transmittance * stepAlpha;

            vec3 tint = mix(u_cloudColor, u_cloudTopColor, heightT);

            accum.rgb += contribution * tint;
            accum.a   += contribution;
            opticalDepth += contribution * density;

            if (contribution > peakContribution) {
                peakContribution = contribution;
                peakPos = p;
            }

            transmittance *= stepTransmittance;
        }
    }

    float finalAlpha = clamp(accum.a * vFadeAlpha, 0.0, 1.0);

    if (finalAlpha <= 0.02)
    discard;

    vec3 albedoColor = accum.rgb / max(accum.a, 0.0001);

    float gradientEpsilon = max(min(vHalfExtent.x, vHalfExtent.z) * 0.04, 0.05);
    vec3 cloudNormalWorld = sampleCloudGradientNormal(
        peakPos, vBoxCenter, vRot, vHalfExtent,
        u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength,
        u_cloudCoverageBias, u_cloudSilhouetteSoftness,
        vDetailFactor, vRandomSeed, u_time, gradientEpsilon);

    float rim = pow(1.0 - clamp(dot(-rayDir, cloudNormalWorld), 0.0, 1.0), CLOUD_RIM_FRESNEL_POWER)
    * u_cloudRimLightStrength;
    albedoColor += vec3(1.0) * rim;

    float shadeAmount = clamp(opticalDepth * u_cloudShadeStrength, 0.0, 1.0);
    float bands = max(float(u_cloudToonBands), 2.0);
    float banded = floor(shadeAmount * bands) / max(bands - 1.0, 1.0);
    albedoColor = mix(albedoColor, u_cloudShadowColor, banded * 0.65);

    float ao = 1.0 - clamp(opticalDepth * u_cloudAmbientOcclusionStrength, 0.0, u_cloudAmbientOcclusionStrength);
    float fogT = clamp(smoothstep(u_cloudHorizonDistance * 0.4, u_cloudHorizonDistance * 0.95, camDist), 0.0, 0.5);

    vec3 normalView = normalize(mat3(u_view) * cloudNormalWorld);

    vec4 peakClip = u_viewProjection * vec4(peakPos, 1.0);
    gl_FragDepth = clamp(peakClip.z / peakClip.w * 0.5 + 0.5, 0.0, 1.0);

    gAlbedo   = vec4(clamp(albedoColor * u_cloudBrightnessMultiplier, 0.0, 1.0), finalAlpha);
    gNormal   = vec4(normalView, finalAlpha);
    gMaterial = vec4(fogT, 0.0, ao, finalAlpha);
}