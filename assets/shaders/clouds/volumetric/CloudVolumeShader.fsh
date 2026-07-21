// CloudVolumeShader.fsh — clouds/volumetric/CloudVolumeShader.fsh
#version 330 core

in vec3  vWorldPos;
in float vRandomSeed;
in float vFadeAlpha;
in float vIntensity;
in float vDensityMultiplier;
flat in vec3 vBoxCenter;
flat in vec3 vHalfExtent;
flat in vec2 vRot;
flat in float vDetailFactor;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/CloudSettingsData.glsl"
#include "clouds/util/VolumetricCloudUtility.glsl"

/*
* Raymarches this instance's oriented box and writes a shaded albedo, a
 * density-gradient normal, and an ambient-occlusion term into the shared
 * deferred G-buffer, like every other surface in the world — the shared
 * Lighting.fsh pass still applies the real sun/moon diffuse term against
 * that normal. What this shader adds is a cheap raymarched self-shadow
 * (sampleCloudLightLift) used to bias ambient occlusion and to gate a
 * tinted silver-lining rim toward whichever side is actually backlit, so
 * the volume reads as lit from one side and shadowed on the other instead
 * of a single flat fresnel edge.
 *
 * Partial coverage is faked via an ordered Bayer dither rather than a
 * random hash, since this engine has no TAA to accumulate stochastic
 * samples over time. That dither only reads as visible stipple where the
 * accumulated alpha genuinely sits in a translucent middle band — the
 * density model in VolumetricCloudUtility keeps a cloud's interior pinned
 * near full density specifically so that band stays a thin edge shell
 * rather than the whole visible body.
 */

uniform vec3  u_cloudColor;
uniform float u_cloudDensity;
uniform float u_cloudDensityNoiseScale;
uniform float u_cloudNoiseWarpStrength;
uniform float u_cloudCoverageBias;
uniform float u_cloudSilhouetteSoftness;

const float CLOUD_STEP_SIZE_NEAR    = 3.0;
const float CLOUD_STEP_SIZE_FAR     = 9.0;
const float CLOUD_TIER_NEAR         = 60.0;
const float CLOUD_TIER_FAR          = 220.0;
const int   CLOUD_MIN_STEPS         = 16;
const int   CLOUD_MAX_STEPS         = 64;
const float CLOUD_EXTINCTION        = 0.18;
const float CLOUD_RIM_FRESNEL_POWER = 2.4;
const float CLOUD_RIM_LIGHT_STRENGTH          = 0.9;
const float CLOUD_AMBIENT_OCCLUSION_STRENGTH  = 0.45;
const float CLOUD_SELF_SHADOW_STRENGTH        = 0.35;
const float CLOUD_SKY_TINT_STRENGTH           = 0.22;
const float CLOUD_GROUND_BOUNCE_STRENGTH      = 0.16;
const float CLOUD_GROUND_BOUNCE_DARKEN        = 0.35;
const float CLOUD_DIRECT_BOUNCE_STRENGTH      = 0.14;

// Ordered 8x8 Bayer threshold matrix (values 0..63). Indexed by screen
// position modulo 8 on each axis, normalized to a [0,1) threshold below.
const float BAYER_8X8[64] = float[64](
    0.0, 32.0,  8.0, 40.0,  2.0, 34.0, 10.0, 42.0,
    48.0, 16.0, 56.0, 24.0, 50.0, 18.0, 58.0, 26.0,
    12.0, 44.0,  4.0, 36.0, 14.0, 46.0,  6.0, 38.0,
    60.0, 28.0, 52.0, 20.0, 62.0, 30.0, 54.0, 22.0,
    3.0, 35.0, 11.0, 43.0,  1.0, 33.0,  9.0, 41.0,
    51.0, 19.0, 59.0, 27.0, 49.0, 17.0, 57.0, 25.0,
    15.0, 47.0,  7.0, 39.0, 13.0, 45.0,  5.0, 37.0,
    63.0, 31.0, 55.0, 23.0, 61.0, 29.0, 53.0, 21.0);

float bayerThreshold(vec2 screenPos) {
    ivec2 cell = ivec2(mod(screenPos, 8.0));
    return (BAYER_8X8[cell.y * 8 + cell.x] + 0.5) / 64.0;
}

// Interleaved Gradient Noise (Jimenez, "Next Generation Post Processing in
// Call of Duty: Advanced Warfare") — a well-distributed spatial hash used
// only to jitter raymarch sample positions, never for the alpha test.
float interleavedGradientNoise(vec2 screenPos) {
    return fract(52.9829189 * fract(dot(screenPos, vec2(0.06711056, 0.00583715))));
}

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
    float dither = interleavedGradientNoise(gl_FragCoord.xy);

    float boxHeight = max(vHalfExtent.y * 2.0, 0.0001);
    float baseY = vBoxCenter.y - vHalfExtent.y;

    vec4  accum = vec4(0.0);
    float opticalDepth = 0.0;
    float transmittance = 1.0;

    vec3  peakPos = vWorldPos;
    float peakDensity = 0.0;
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
        float density = rawDensity * u_cloudDensity * vDensityMultiplier * vIntensity;

        if (density > 0.01) {
            float stepTransmittance = exp(-density * CLOUD_EXTINCTION * stepSize);
            float stepAlpha = 1.0 - stepTransmittance;
            float contribution = transmittance * stepAlpha;

            accum.rgb += contribution * u_cloudColor;
            accum.a   += contribution;
            opticalDepth += contribution * density;

            if (contribution > peakContribution) {
                peakContribution = contribution;
                peakPos = p;
                peakDensity = density;
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

    // Light direction — blend sun/moon exactly like every other lit surface,
    // so the cloud's own self-shadow and rim agree with whichever body is
    // actually driving the scene's light this time of day.
    float sunWeight = clamp(u_sunIntensity / 0.3, 0.0, 1.0);
    vec3  lightDir = normalize(mix(u_moonDirection, u_sunDirection, sunWeight));
    vec3  lightColor = mix(u_moonColor, u_sunColor, sunWeight);
    float lightPower = mix(u_moonIntensity, u_sunIntensity, sunWeight);

    float tapDistance = clamp(min(vHalfExtent.x, vHalfExtent.z) * 0.3, 3.0, 18.0);
    float lightLift = sampleCloudLightLift(
        peakPos, vBoxCenter, vRot, vHalfExtent,
        u_cloudDensityNoiseScale, u_cloudNoiseWarpStrength,
        u_cloudCoverageBias, u_cloudSilhouetteSoftness,
        vDetailFactor, vRandomSeed, u_time,
        lightDir, tapDistance, peakDensity);

    float ao = 1.0 - clamp(opticalDepth * CLOUD_AMBIENT_OCCLUSION_STRENGTH, 0.0, CLOUD_AMBIENT_OCCLUSION_STRENGTH);
    ao = clamp(ao + (lightLift - 0.5) * CLOUD_SELF_SHADOW_STRENGTH, 0.0, 1.0);
    albedoColor *= ao;

    // Silver lining — a fresnel rim gated by how directly the view ray is
    // looking back toward the light, so it only shows up on genuinely
    // backlit edges rather than every edge regardless of light direction.
    float backlit = pow(clamp(dot(rayDir, lightDir), 0.0, 1.0), 1.5);
    float rimFresnel = pow(1.0 - clamp(dot(-rayDir, cloudNormalWorld), 0.0, 1.0), CLOUD_RIM_FRESNEL_POWER);
    float rim = rimFresnel * mix(0.1, 1.0, backlit) * lightLift * CLOUD_RIM_LIGHT_STRENGTH;
    albedoColor += lightColor * lightPower * rim;

    // A small direct-light bounce accent — not a full relight (the shared
    // deferred pass already applies the real sun/moon diffuse against the
    // gradient normal), just enough to warm the lit side and cool the
    // shadowed side so the volume reads as genuinely lit from one direction.
    albedoColor += lightColor * lightPower * (lightLift - 0.5) * CLOUD_DIRECT_BOUNCE_STRENGTH;

    float upFacing = clamp(cloudNormalWorld.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 skyTint = mix(u_skyHorizonColor, u_skyZenithColor, upFacing);
    vec3 groundBounce = u_skyHorizonColor * CLOUD_GROUND_BOUNCE_DARKEN;
    albedoColor = mix(albedoColor, skyTint, CLOUD_SKY_TINT_STRENGTH * upFacing);
    albedoColor = mix(albedoColor, groundBounce, CLOUD_GROUND_BOUNCE_STRENGTH * (1.0 - upFacing) * ao);

    float fogT = clamp(smoothstep(u_cloudHorizonDistance * 0.4, u_cloudHorizonDistance * 0.95, camDist), 0.0, 0.5);

    vec3 normalView = normalize(mat3(u_view) * cloudNormalWorld);

    vec4 peakClip = u_viewProjection * vec4(peakPos, 1.0);
    gl_FragDepth = clamp(peakClip.z / peakClip.w * 0.5 + 0.5, 0.0, 1.0);

    // Ordered alpha test — every fragment that survives this writes fully
    // opaque into the G-buffer; see the top-of-file comment for why this
    // exists at all instead of real blending.
    if (finalAlpha < bayerThreshold(gl_FragCoord.xy))
    discard;

    gAlbedo   = vec4(clamp(albedoColor, 0.0, 1.0), 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(fogT, 0.0, ao, 1.0);
}