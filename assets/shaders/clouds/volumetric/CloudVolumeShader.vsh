// CloudVolumeShader.vsh — clouds/volumetric/CloudVolumeShader.vsh
#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 3) in vec4  aInstance0; // xyz = world position, w = random seed
layout (location = 4) in vec4  aInstance1; // x = domain rotation, y = fade alpha, z = intensity, w = size variance
layout (location = 5) in float aInstance2; // elongation
layout (location = 6) in float aInstance3; // density multiplier

#include "includes/CameraData.glsl"
#include "includes/CloudSettingsData.glsl"
#include "includes/TimeData.glsl"

uniform float u_cloudScale;
uniform float u_cloudVerticalThickness;

out vec3  vWorldPos;
flat out float vFadeAlpha;
flat out float vIntensity;
flat out float vDensityMultiplier;
flat out float vDetailFactor;
flat out float vElongation;
flat out vec3  vSeedOffset;
flat out vec3  vTimeDrift;

flat out vec3 vBoxCenter;
flat out vec3 vHalfExtent;
flat out vec2 vRot;

/*
* Builds this instance's own oriented box at full physical size — only alpha
 * fades approaching the horizon, so a cloud is always its true size or
 * invisible, never a shrunken sliver. u_cloudMinScale/u_cloudMaxScale are
 * reused here as a distance-based noise-detail factor (see
 * VolumetricCloudUtility.glsl), not a geometric scale.
 *
 * Everything the fragment shader's raymarch would otherwise recompute on
 * every one of its many density samples, but which is actually constant
 * across the whole instance — the elongation ratio, the domain-warp seed
 * offset, and the time-based drift offset — is resolved once here instead.
 */
void main() {
    vec3 cameraRenderPos = (u_inverseView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;

    vec3  instancePos       = aInstance0.xyz;
    float randomSeed        = aInstance0.w;
    float domainRotation    = aInstance1.x;
    float fadeAlpha         = aInstance1.y;
    float intensity         = aInstance1.z;
    float sizeVariance      = aInstance1.w;
    float elongation        = max(aInstance2, 1.0);
    float densityMultiplier = aInstance3;

    float distFromCamera = length(instancePos.xz - cameraRenderPos.xz);
    float distanceT = clamp(distFromCamera / max(u_cloudHorizonDistance, 0.001), 0.0, 1.0);

    float transitionSpan = max(u_cloudHorizonDistance - u_cloudTransitionStart, 0.001);
    float fadeT = clamp((distFromCamera - u_cloudTransitionStart) / transitionSpan, 0.0, 1.0);
    float horizonFade = 1.0 - smoothstep(0.0, 1.0, fadeT);

    vDetailFactor = mix(u_cloudMaxScale, u_cloudMinScale, distanceT);

    float finalScaleY = u_cloudVerticalThickness * sizeVariance;
    float halfX = (u_cloudScale * sizeVariance * elongation) * 0.5;
    float halfZ = (u_cloudScale * sizeVariance) * 0.5;
    float halfY = finalScaleY * 0.5;

    float cosR = cos(domainRotation);
    float sinR = sin(domainRotation);

    vec2 localXZ = vec2(aPos.x * halfX * 2.0, aPos.z * halfZ * 2.0);
    vec2 rotatedXZ = vec2(
        localXZ.x * cosR - localXZ.y * sinR,
        localXZ.x * sinR + localXZ.y * cosR);

    vec3 worldPos = vec3(
        instancePos.x + rotatedXZ.x,
        instancePos.y + aPos.y * finalScaleY,
        instancePos.z + rotatedXZ.y);

    vWorldPos   = worldPos;
    vFadeAlpha  = fadeAlpha * horizonFade;
    vIntensity  = intensity;
    vDensityMultiplier = densityMultiplier;

    // vElongation == halfX / halfZ exactly (both share the same
    // scale*sizeVariance*0.5 factor) — reusing it here instead of the
    // fragment shader re-deriving that ratio per sample.
    vElongation = elongation;
    vSeedOffset = vec3(randomSeed * 173.13, randomSeed * 57.31, randomSeed * 91.7);
    vTimeDrift  = vec3(u_time * 0.006, 0.0, u_time * 0.004);

    vBoxCenter  = vec3(instancePos.x, instancePos.y + halfY, instancePos.z);
    vHalfExtent = vec3(halfX, halfY, halfZ);
    vRot        = vec2(cosR, sinR);

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}