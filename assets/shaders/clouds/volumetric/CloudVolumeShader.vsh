#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 3) in vec4  aInstance0; // xyz = world position, w = random seed
layout (location = 4) in vec4  aInstance1; // x = domain rotation, y = fade alpha, z = intensity, w = size variance
layout (location = 5) in float aInstance2; // elongation — long/short axis ratio, local X before rotation

#include "includes/CameraData.glsl"
#include "includes/CloudSettingsData.glsl"

uniform float u_cloudScale;
uniform float u_cloudVerticalThickness;

out vec3  vWorldPos;
out float vRandomSeed;
out float vFadeAlpha;
out float vIntensity;

flat out vec3  vBoxMin;
flat out vec3  vBoxMax;
flat out vec2  vHalfExtentXZ;
flat out vec2  vRot;

/*
* Builds this instance's box from its own world position, distance LOD
 * scale, and the elongation/rotation rolled once at stream-in (see
 * WeatherPatternManager). The mesh's local X axis is always the long axis
 * before rotation. vBoxMin/vBoxMax is the conservative axis-aligned bound
 * of that rotated rectangle for the fragment shader's raymarch; the true
 * (pre-rotation) half-extents travel separately in vHalfExtentXZ so the
 * fragment shader can still resolve a properly oriented oval silhouette.
 */
void main() {
    vec3 cameraRenderPos = (u_inverseView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;

    vec3  instancePos    = aInstance0.xyz;
    float randomSeed     = aInstance0.w;
    float domainRotation = aInstance1.x;
    float fadeAlpha      = aInstance1.y;
    float intensity      = aInstance1.z;
    float sizeVariance   = aInstance1.w;
    float elongation     = max(aInstance2, 1.0);

    float distFromCamera = length(instancePos.xz - cameraRenderPos.xz);
    float distanceT = clamp(distFromCamera / u_cloudHorizonDistance, 0.0, 1.0);
    float sizeScale = mix(u_cloudMaxScale, u_cloudMinScale, distanceT) * sizeVariance;

    float transitionSpan = max(u_cloudHorizonDistance - u_cloudTransitionStart, 0.001);
    float fadeT = clamp((distFromCamera - u_cloudTransitionStart) / transitionSpan, 0.0, 1.0);
    float horizonFade = 1.0 - smoothstep(0.0, 1.0, fadeT);

    float finalScaleY = u_cloudVerticalThickness * sizeScale;
    float halfX = (u_cloudScale * sizeScale * elongation) * 0.5;
    float halfZ = (u_cloudScale * sizeScale) * 0.5;

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

    float worldHalfX = abs(halfX * cosR) + abs(halfZ * sinR);
    float worldHalfZ = abs(halfX * sinR) + abs(halfZ * cosR);

    vBoxMin = vec3(instancePos.x - worldHalfX, instancePos.y, instancePos.z - worldHalfZ);
    vBoxMax = vec3(instancePos.x + worldHalfX, instancePos.y + finalScaleY, instancePos.z + worldHalfZ);

    vWorldPos = worldPos;
    vRandomSeed = randomSeed;
    vFadeAlpha = fadeAlpha * horizonFade;
    vIntensity = intensity;
    vHalfExtentXZ = vec2(halfX, halfZ);
    vRot = vec2(cosR, sinR);

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}