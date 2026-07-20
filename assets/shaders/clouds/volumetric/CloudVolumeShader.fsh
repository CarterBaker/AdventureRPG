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
#include "includes/CloudSettingsData.glsl"
#include "clouds/util/VolumetricCloudUtility.glsl"

/*
* Raymarches this instance's oriented box and writes an unlit albedo, a
 * density-gradient normal, and an ambient-occlusion term into the shared
 * deferred G-buffer, exactly like every other surface in the world.
 * Directional sun/moon response is left entirely to the shared Lighting.fsh
 * pass. Partial coverage is faked via an ordered Bayer dither rather than a
 * random hash — this engine has no TAA to accumulate stochastic samples
 * over time, so a single-frame random dither reads as visible static; a
 * fixed, evenly-distributed ordered matrix does not. u_cloudDensity is this
 * archetype's own intrinsic density; vDensityMultiplier is the resolved
 * per-instance weather multiplier on top of it.
 */

uniform vec3  u_cloudColor;
uniform float u_cloudDensity;
uniform float u_cloudDensityNoiseScale;
uniform float u_cloudNoiseWarpStrength;
uniform float u_cloudCoverageBias;
uniform float u_cloudSilhouetteSoftness;

const float CLOUD_STEP_SIZE_NEAR    = 4.0;
const float CLOUD_STEP_SIZE_FAR     = 12.0;
const float CLOUD_TIER_NEAR         = 60.0;
const float CLOUD_TIER_FAR          = 220.0;
const int   CLOUD_MIN_STEPS         = 14;
const int   CLOUD_MAX_STEPS         = 56;
const float CLOUD_EXTINCTION        = 0.07;
const float CLOUD_RIM_FRESNEL_POWER = 2.4;
const float CLOUD_RIM_LIGHT_STRENGTH          = 0.35;
const float CLOUD_AMBIENT_OCCLUSION_STRENGTH  = 0.4;
const float CLOUD_SKY_TINT_STRENGTH           = 0.22;
const float CLOUD_GROUND_BOUNCE_STRENGTH      = 0.16;
const float CLOUD_GROUND_BOUNCE_DARKEN        = 0.35;

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
// Call of Duty: Advanced Warfare") — a well-distributed spatial hash with
// none of the clustering or sin()-precision artifacts a naive
// fract(sin(dot(...))) hash produces at typical screen-space magnitudes.
// Used only to jitter raymarch sample positions, never for the alpha test.
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

    float ao = 1.0 - clamp(opticalDepth * CLOUD_AMBIENT_OCCLUSION_STRENGTH, 0.0, CLOUD_AMBIENT_OCCLUSION_STRENGTH);
    albedoColor *= ao;

    float rim = pow(1.0 - clamp(dot(-rayDir, cloudNormalWorld), 0.0, 1.0), CLOUD_RIM_FRESNEL_POWER)
    * CLOUD_RIM_LIGHT_STRENGTH;
    albedoColor += vec3(1.0) * rim;

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
    // opaque into the G-buffer; see the class doc comment for why this
    // exists at all instead of real blending.
    if (finalAlpha < bayerThreshold(gl_FragCoord.xy))
    discard;

    gAlbedo   = vec4(clamp(albedoColor, 0.0, 1.0), 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(fogT, 0.0, ao, 1.0);
}