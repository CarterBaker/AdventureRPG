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
#include "includes/SkyColorData.glsl"
#include "includes/CloudSettingsData.glsl"
#include "clouds/util/VolumetricCloudUtility.glsl"

/*
* Raymarches this instance's oriented box and writes an unlit albedo, a
 * real density-gradient surface normal, and a self-shadow occlusion term
 * into the shared deferred G-buffer — the same three channels terrain
 * writes. Directional sun/moon response is left entirely to the shared
 * Lighting.fsh pass. Sky zenith/horizon color and a fake ground-bounce
 * tint are baked into the albedo here instead, the same way shadowColor/
 * topColor already are — material variation, not a second light source.
 * Every surviving fragment writes alpha = 1.0 on all three outputs: this
 * pass runs with blending enabled, and any partial alpha here gets
 * silently scaled down against the frame's black clear color by the ROP
 * blend equation — which is what previously made every cloud read as
 * unlit black regardless of the sun. A dithered cutoff against
 * finalAlpha stands in for the soft edge a partial alpha can no longer
 * represent.
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
const float CLOUD_SKY_TINT_STRENGTH      = 0.22;
const float CLOUD_GROUND_BOUNCE_STRENGTH = 0.16;
const float CLOUD_GROUND_BOUNCE_DARKEN   = 0.35;

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

    // Sky/ground tint — fake bounce lighting baked directly into the
    // unlit material color rather than computed as a real light source.
    // Top-facing surface picks up a fraction of the sky's own current
    // zenith/horizon color, so a cloud tints correctly with time of day
    // and season and agrees with the sky dome's own clouds (see
    // SkyCloudUtility.glsl's identical topTint technique). Underside
    // surface picks up a muted, darkened fraction of horizon color
    // standing in for ambient light bounced up off the terrain below —
    // grazing horizon-level light is what actually reaches the ground —
    // scaled down further wherever the cloud reads as thick/self-shadowed,
    // since bounce light doesn't reach a cloud's dense interior either.
    float upFacing = clamp(cloudNormalWorld.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 skyTint = mix(u_skyHorizonColor, u_skyZenithColor, upFacing);
    vec3 groundBounce = u_skyHorizonColor * CLOUD_GROUND_BOUNCE_DARKEN;
    albedoColor = mix(albedoColor, skyTint, CLOUD_SKY_TINT_STRENGTH * upFacing);
    albedoColor = mix(albedoColor, groundBounce,
        CLOUD_GROUND_BOUNCE_STRENGTH * (1.0 - upFacing) * (1.0 - shadeAmount));

    float ao = 1.0 - clamp(opticalDepth * u_cloudAmbientOcclusionStrength, 0.0, u_cloudAmbientOcclusionStrength);
    float fogT = clamp(smoothstep(u_cloudHorizonDistance * 0.4, u_cloudHorizonDistance * 0.95, camDist), 0.0, 0.5);

    vec3 normalView = normalize(mat3(u_view) * cloudNormalWorld);

    vec4 peakClip = u_viewProjection * vec4(peakPos, 1.0);
    gl_FragDepth = clamp(peakClip.z / peakClip.w * 0.5 + 0.5, 0.0, 1.0);

    // Stochastic alpha test — decorrelated from the raymarch jitter hash
    // above so the dither pattern doesn't lock to the same screen-space
    // grid as the sampling offsets. Every fragment that survives this
    // writes fully opaque into the G-buffer; see the class doc comment.
    float alphaDither = fract(sin(dot(gl_FragCoord.xy, vec2(39.9898, 61.233))) * 24634.6345);

    if (finalAlpha < alphaDither)
    discard;

    gAlbedo   = vec4(clamp(albedoColor * u_cloudBrightnessMultiplier, 0.0, 1.0), 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(fogT, 0.0, ao, 1.0);
}