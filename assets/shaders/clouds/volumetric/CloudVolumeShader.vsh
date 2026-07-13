#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aUV;
layout (location = 3) in vec4 aInstance0; // xyz = world position, w = random seed
layout (location = 4) in vec4 aInstance1; // x = domain rotation, y = fade alpha, z = intensity, w = size variance

#include "includes/CameraData.glsl"
#include "includes/CloudSettingsData.glsl"

uniform float u_cloudScale;
uniform float u_cloudVerticalThickness;

out vec3  vWorldPos;
out vec3  vNormal;
out float vRandomSeed;
out float vDomainRotation;
out float vFadeAlpha;
out float vIntensity;

flat out vec3 vBoxMin;
flat out vec3 vBoxMax;

/*
* Per-instance placement now arrives entirely through instanced vertex
 * attributes, written once per instance per frame by CloudRenderSystem into
 * the shared CompositeBufferInstance for this cloud archetype — never
 * through per-draw uniforms. This is what actually lets every instance
 * sharing one CloudHandle render in a single glDrawElementsInstanced call.
 * sizeVariance scales the whole instance uniformly so no two clouds of the
 * same archetype read as identically sized.
 *
 * cameraRenderPos is reconstructed from the view matrix rather than read
 * from u_cameraPosition directly, since instancePos already arrives in the
 * player-chunk-recentered render frame every other vertex here shares — see
 * LightingShader.fsh for the same technique applied elsewhere.
 */
void main() {
    vec3 cameraRenderPos = (u_inverseView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;

    vec3  instancePos    = aInstance0.xyz;
    float randomSeed     = aInstance0.w;
    float domainRotation = aInstance1.x;
    float fadeAlpha      = aInstance1.y;
    float intensity      = aInstance1.z;
    float sizeVariance   = aInstance1.w;

    float distFromCamera = length(instancePos.xz - cameraRenderPos.xz);
    float distanceT = clamp(distFromCamera / u_cloudHorizonDistance, 0.0, 1.0);
    float sizeScale = mix(u_cloudMaxScale, u_cloudMinScale, distanceT) * sizeVariance;

    float transitionSpan = max(u_cloudHorizonDistance - u_cloudTransitionStart, 0.001);
    float fadeT = clamp((distFromCamera - u_cloudTransitionStart) / transitionSpan, 0.0, 1.0);
    float horizonFade = 1.0 - smoothstep(0.0, 1.0, fadeT);

    float finalScaleXZ = u_cloudScale * sizeScale;
    float finalScaleY = u_cloudVerticalThickness * sizeScale;

    vec3 scaledLocal = vec3(aPos.x * finalScaleXZ, aPos.y * finalScaleY, aPos.z * finalScaleXZ);
    vec3 worldPos = instancePos + scaledLocal;

    vBoxMin = vec3(instancePos.x - finalScaleXZ * 0.5, instancePos.y, instancePos.z - finalScaleXZ * 0.5);
    vBoxMax = vec3(instancePos.x + finalScaleXZ * 0.5, instancePos.y + finalScaleY, instancePos.z + finalScaleXZ * 0.5);

    vWorldPos = worldPos;
    vNormal = aNormal;
    vRandomSeed = randomSeed;
    vDomainRotation = domainRotation;
    vFadeAlpha = fadeAlpha * horizonFade;
    vIntensity = intensity;

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}