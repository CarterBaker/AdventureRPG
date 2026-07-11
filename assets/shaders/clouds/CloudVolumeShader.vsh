#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in vec3  aNormal;
layout (location = 2) in vec2  aUV;

#include "includes/CameraData.glsl"
#include "includes/CloudSettingsData.glsl"
#include "includes/PlayerPositionData.glsl"

uniform float u_cloudScale;
uniform float u_cloudVerticalThickness;

uniform ivec2 u_cloudInstanceChunk;       // this cloud's home chunk index — exact at any distance from origin
uniform vec3  u_cloudInstanceLocal;       // localX, altitudeY, localZ — in-chunk remainder + altitude
uniform float u_cloudInstanceRandomSeed;  // stable per-cloud shape/warp seed
uniform float u_cloudInstanceFadeAlpha;   // streaming fade — see OverheadCellStruct.fadeAlpha
uniform float u_cloudInstanceIntensity;   // live weather strength — see OverheadCellStruct.getIntensity()

out vec3  vWorldPos;
out vec3  vNormal;
out float vRandomSeed;
out float vFadeAlpha;
out float vIntensity;

flat out vec3 vBoxMin;
flat out vec3 vBoxMax;

/*
* Player-chunk-relative instance position — see CloudRenderSystem
 * .updateInstance() for the CPU-side half of this encoding; mirrors
 * StandardItemShader.vsh exactly. u_view/u_viewProjection are themselves
 * expressed in this same player-chunk-recentered space (see
 * StandardSurfaceShader's own u_gridPosition offset for the terrain-side
 * equivalent) — a position built any other way silently drifts out of
 * agreement with the camera transform as the player moves.
 *
 * CAMERA POSITION FIX: this shader must never read u_cameraPosition
 * directly for a manual world-space distance calculation against
 * instancePos. Whether u_cameraPosition happens to already sit in this
 * same recentered frame is not something to assume — LightingShader.fsh
 * avoids exactly this trap by reconstructing every view-relative vector
 * from the view/projection matrices instead of ever differencing
 * u_cameraPosition against a world position. surface/includes/Height.glsl
 * documents the correct reconstruction, now used here too:
 * `(u_inverseView * vec4(0,0,0,1)).xyz` recovers the camera's own position
 * in WHATEVER frame the view matrix operates in, guaranteed to match
 * instancePos with no assumption required. Reading u_cameraPosition
 * directly here previously left the LOD/fade math (and
 * CloudVolumeShader.fsh's raymarch direction) silently comparing two
 * different coordinate frames — on its own enough to fade every cloud to
 * fully invisible regardless of where it was actually placed.
 */
void main() {
    vec3 cameraRenderPos = (u_inverseView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;

    float relChunkX = float(u_cloudInstanceChunk.x - u_playerChunkX);
    float relChunkZ = float(u_cloudInstanceChunk.y - u_playerChunkZ);

    vec3 instancePos = vec3(
        relChunkX * 16.0 + u_cloudInstanceLocal.x,
        u_cloudInstanceLocal.y,
        relChunkZ * 16.0 + u_cloudInstanceLocal.z);

    float distFromCamera = length(instancePos.xz - cameraRenderPos.xz);
    float distanceT = clamp(distFromCamera / u_cloudHorizonDistance, 0.0, 1.0);
    float sizeScale = mix(u_cloudMaxScale, u_cloudMinScale, distanceT);

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
    vRandomSeed = u_cloudInstanceRandomSeed;
    vFadeAlpha = u_cloudInstanceFadeAlpha * horizonFade;
    vIntensity = u_cloudInstanceIntensity;

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}