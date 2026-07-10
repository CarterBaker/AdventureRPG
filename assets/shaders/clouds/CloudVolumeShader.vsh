#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in vec3  aNormal;
layout (location = 2) in vec2  aUV;

#include "includes/CameraData.glsl"
#include "includes/CloudSettingsData.glsl"
#include "includes/PlayerPositionData.glsl"

// Baked once per instance in CloudRenderSystem.createInstance() — one
// archetype's own shape numbers, shared by every cloud drawn through this
// same CloudType (each with its own cloned MaterialInstance, exactly like
// terrain clones one shared material per chunk mesh).
uniform float u_cloudScale;
uniform float u_cloudVerticalThickness;

// Per-instance state — this specific cloud object's own position, seed,
// and live fade/intensity. Clouds now draw through the exact same generic
// ModelInstance/MaterialInstance path terrain uses (see CloudRenderSystem
// and RenderManager.pushRenderCall()) — one ordinary, non-instanced draw
// call per cloud object rather than a GPU-instanced batch — so these
// arrive as plain uniforms on this draw's own material, refreshed every
// frame in CloudRenderSystem.updateInstance(), instead of as per-vertex
// instance attributes read off a shared instance VBO with a divisor of 1.
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

// This instance's own world-space (render-space) AABB, computed once here
// rather than re-derived per fragment. flat since every fragment of this
// instance's draw call shares the exact same box — see
// CloudVolumeShader.fsh's raymarch, which finds where its view ray exits
// this box to know how far to march.
flat out vec3 vBoxMin;
flat out vec3 vBoxMax;

/*
* Player-chunk-relative instance position. u_cloudInstanceChunk carries
 * this cloud's home chunk index directly as a genuine integer uniform —
 * precise at any distance from world origin, since it's a whole chunk
 * count, never a fractional world coordinate (no floatBitsToInt bit-
 * punning needed here, unlike the old GPU-instanced version of this
 * shader — a uniform can just BE an ivec2). u_cloudInstanceLocal carries
 * the small in-chunk remainder (wind drift fraction converted to blocks)
 * plus altitude. Reconstructing render-space position here, relative to
 * u_playerChunkX/Z (see PlayerPositionData.glsl), mirrors
 * StandardItemShader.vsh's identical technique exactly — u_cameraPosition
 * and u_viewProjection are themselves expressed in that same
 * player-chunk-recentered space (see StandardSurfaceShader's own
 * u_gridPosition offset for the terrain-side equivalent), so an instance
 * position built any other way silently drifts out of agreement with the
 * camera transform as the player moves — which is what previously read as
 * clouds "snapping" between chunks. See CloudRenderSystem.updateInstance()
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
 *
 * vIntensity passes the cell's live weather-strength value straight
 * through untouched — it is a fragment-level density modulation (see
 * CloudVolumeShader.fsh), not a geometric one, so it never affects size
 * or position here.
 *
 * vBoxMin/vBoxMax are this instance's AABB in that same render space,
 * built from the identical instancePos/finalScaleXZ/finalScaleY used to
 * place the mesh itself, so the fragment shader's raymarch always bounds
 * itself to exactly the box GL actually rasterized — never a mismatched
 * or stale volume.
 */
void main() {
    float relChunkX = float(u_cloudInstanceChunk.x - u_playerChunkX);
    float relChunkZ = float(u_cloudInstanceChunk.y - u_playerChunkZ);

    vec3 instancePos = vec3(
        relChunkX * 16.0 + u_cloudInstanceLocal.x,
        u_cloudInstanceLocal.y,
        relChunkZ * 16.0 + u_cloudInstanceLocal.z);

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

    vBoxMin = vec3(instancePos.x - finalScaleXZ * 0.5, instancePos.y, instancePos.z - finalScaleXZ * 0.5);
    vBoxMax = vec3(instancePos.x + finalScaleXZ * 0.5, instancePos.y + finalScaleY, instancePos.z + finalScaleXZ * 0.5);

    vWorldPos = worldPos;
    vNormal = aNormal;
    vRandomSeed = u_cloudInstanceRandomSeed;
    vFadeAlpha = u_cloudInstanceFadeAlpha * horizonFade;
    vIntensity = u_cloudInstanceIntensity;

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}