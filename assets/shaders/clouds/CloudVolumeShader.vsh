#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in vec3  aNormal;
layout (location = 2) in vec2  aUV;
layout (location = 3) in vec2  aInstanceChunk;  // chunkX, chunkZ — reinterpreted int bits
layout (location = 4) in vec3  aInstanceLocal;  // localX, altitudeY, localZ
layout (location = 5) in float aRandomSeed;
layout (location = 6) in float aFadeAlpha;

#include "includes/CameraData.glsl"
#include "includes/CloudSettingsData.glsl"
#include "includes/PlayerPositionData.glsl"

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
* Player-chunk-relative instance position. aInstanceChunk carries this
 * instance's home chunk index as reinterpreted int bits — precise at any
 * distance from world origin, since it's a whole chunk count, never a
 * fractional world coordinate. aInstanceLocal carries the small in-chunk
 * remainder (wind drift fraction converted to blocks) plus altitude.
 * Reconstructing render-space position here, relative to
 * u_playerChunkX/Z (see PlayerPositionData.glsl), mirrors
 * StandardItemShader.vsh's identical technique exactly — u_cameraPosition
 * and u_viewProjection are themselves expressed in that same
 * player-chunk-recentered space (see StandardSurfaceShader's own
 * u_gridPosition offset for the terrain-side equivalent), so an instance
 * position built any other way — e.g. a raw absolute chunk*CHUNK_SIZE
 * float, as before this fix — silently drifts out of agreement with the
 * camera transform as the player moves, which is what previously read as
 * clouds "snapping" between chunks. See CloudRenderSystem.addInstance()
 * for the CPU-side half of this encoding.
 *
 * "The clouds will always be render distance at max away" (horizon
 * streaming radius, see OverheadManager) — cards near that radius are
 * shrunk toward u_cloudMinScale so their pop-in at the streaming edge is
 * unnoticeable; cards near the player grow toward u_cloudMaxScale. This is
 * the LOD handoff between the sky-dome-painted horizon clouds and these
 * real, flyable-under geometry clouds — a card is always alive somewhere
 * in between, so the transition is continuous rather than a hard swap.
 * u_cloudHorizonDistance itself is derived from whichever is smaller of
 * the actual configured render distance and the weather simulation's near
 * range (see CloudRenderSystem.pushCloudSettings()) — never a fixed
 * distance — so this LOD always spans exactly the terrain that is
 * actually drawn: a cloud never reads as floating over ground that was
 * never rendered underneath it.
 *
 * Horizon alpha transition. Size LOD alone still lets a fully-opaque,
 * merely-small cloud pop in/out right at the streaming edge —
 * OverheadManager retiring or streaming in a cell is itself a hard
 * existence change, softened only by that cell's OWN fade timer (see
 * OverheadCellStruct.fadeAlpha), which is time-based, not distance-based,
 * so a fast-moving camera can still outrun it. horizonFade closes that
 * gap: starting at u_cloudTransitionStart (a ratio of the way to the
 * horizon — see EngineSetting.CLOUD_VOLUME_FADE_START_RATIO) alpha ramps
 * smoothly to 0 by u_cloudHorizonDistance, the same radius OverheadManager
 * streams within, so a card has always faded to fully invisible before it
 * is ever actually retired, regardless of how fast the camera closes that
 * distance. u_cloudSkyViewDistance is deliberately not read here: it
 * bounds the SKY DOME's own far-range representation (see Clouds.glsl /
 * RegionSampleBranch's far-range sampling), a different projection
 * entirely from these world-space instances, which never exist beyond
 * u_cloudHorizonDistance in the first place.
 */
void main() {
    int chunkX = floatBitsToInt(aInstanceChunk.x);
    int chunkZ = floatBitsToInt(aInstanceChunk.y);

    float relChunkX = float(chunkX - u_playerChunkX);
    float relChunkZ = float(chunkZ - u_playerChunkZ);

    vec3 instancePos = vec3(
        relChunkX * 16.0 + aInstanceLocal.x,
        aInstanceLocal.y,
        relChunkZ * 16.0 + aInstanceLocal.z);

    float distFromCamera = length(instancePos.xz - u_cameraPosition.xz);
    float distanceT = clamp(distFromCamera / u_cloudHorizonDistance, 0.0, 1.0);
    float sizeScale = mix(u_cloudMaxScale, u_cloudMinScale, distanceT);

    float transitionSpan = max(u_cloudHorizonDistance - u_cloudTransitionStart, 0.001);
    float fadeT = clamp((distFromCamera - u_cloudTransitionStart) / transitionSpan, 0.0, 1.0);
    float horizonFade = 1.0 - smoothstep(0.0, 1.0, fadeT);

    float finalScaleXZ = u_cloudScale * sizeScale;
    float finalScaleY = u_cloudVerticalThickness * sizeScale;

    vec3 scaledLocal = vec3(aPos.x * finalScaleXZ, aPos.y * finalScaleY, aPos.z * finalScaleXZ);
    vec3 worldPos = instancePos + scaledLocal;

    vWorldPos = worldPos;
    vLocalPos = aPos;
    vNormal = aNormal;
    vUV = aUV;
    vRandomSeed = aRandomSeed;
    vFadeAlpha = aFadeAlpha * horizonFade;

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}