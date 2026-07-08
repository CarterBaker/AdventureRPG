#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in vec3  aNormal;
layout (location = 2) in vec2  aUV;
layout (location = 3) in vec3  aInstancePos;
layout (location = 4) in float aRandomSeed;
layout (location = 5) in float aFadeAlpha;

#include "includes/CameraData.glsl"
#include "includes/CloudSettingsData.glsl"

// Baked once per material clone in CloudRenderSystem.resolveMaterial() —
// one archetype's own shape numbers, shared by every instance drawn
// through this material.
uniform float u_cloudScale;
uniform float u_cloudVerticalThickness;

out vec3  vWorldPos;
out vec3  vLocalPos;
out vec3  vNormal;
out vec2  vUV;
out float vRandomSeed;
out float vFadeAlpha;

/*
* "The clouds will always be render distance at max away" (horizon streaming
 * radius, see OverheadManager) — cards near that radius are shrunk toward
 * u_cloudMinScale so their pop-in at the streaming edge is unnoticeable;
 * cards near the player grow toward u_cloudMaxScale. This is the LOD
 * handoff between the sky-dome-painted horizon clouds and these real,
 * flyable-under geometry clouds — a card is always alive somewhere in
 * between, so the transition is continuous rather than a hard swap.
 */
void main() {
    float distFromCamera = length(aInstancePos.xz - u_cameraPosition.xz);
    float distanceT = clamp(distFromCamera / u_cloudHorizonDistance, 0.0, 1.0);
    float sizeScale = mix(u_cloudMaxScale, u_cloudMinScale, distanceT);

    float finalScaleXZ = u_cloudScale * sizeScale;
    float finalScaleY = u_cloudVerticalThickness * sizeScale;

    vec3 scaledLocal = vec3(aPos.x * finalScaleXZ, aPos.y * finalScaleY, aPos.z * finalScaleXZ);
    vec3 worldPos = aInstancePos + scaledLocal;

    vWorldPos = worldPos;
    vLocalPos = aPos;
    vNormal = aNormal;
    vUV = aUV;
    vRandomSeed = aRandomSeed;
    vFadeAlpha = aFadeAlpha;

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}