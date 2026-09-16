#ifndef SURFACE_TESSELLATION_TIER_GLSL
#define SURFACE_TESSELLATION_TIER_GLSL

#include "includes/SettingsData.glsl"
#include "includes/PlayerPositionData.glsl"

// Shared tier-distance math for StandardSurfaceShader's tcs/tes/fsh and for WaterShader. Tessellation
// density is decided from the chunk that owns the patch's origin block rather than from a patch or edge
// midpoint: merged quads never cross a chunk, so every face of a given block resolves to one key, and a
// long quad whose center drifts into the neighbouring chunk can no longer pick a different tier from the
// perpendicular face it shares an edge with. Every tier scales its level by block size, so vertex spacing
// is uniform (0.25 blocks near, 1.0 blocks beyond) and two patches of different sizes always place
// vertices at the same positions along any shared edge, which is what keeps world curvature and distant
// rise, both nonlinear in position, from splitting a long quad away from a short one. Displacement
// strength fades to zero a full chunk inside the density boundary, so wherever two chunks disagree on
// density the surface there is already flat.

const float DISTANT_RISE_START_MARGIN_BLOCKS = 512.0;
const float NEAR_TESSELLATION_DENSITY        = 4.0;
const float MAX_TESSELLATION_LEVEL           = 64.0;
const float NEAR_FADE_END_CHUNKS             = 1.0;
const float NEAR_FADE_BAND_CHUNKS            = 1.0;

float getTier1MaxSqDist() {
    float halfD        = u_renderDistance * 0.5 - 0.5;
    float marginChunks = DISTANT_RISE_START_MARGIN_BLOCKS / (u_chunkSize * sqrt(2.0));
    float farHalfD     = max(halfD - marginChunks, 1.0);
    return farHalfD * farHalfD * 2.0;
}

float getTier0MaxSqDist() {
    float r = max(u_nearTessellationRadius, 1.0);
    return min(2.0 * r * r + 0.5, getTier1MaxSqDist());
}

float computeDistanceFromCenterSq(vec3 worldPos) {
    vec2 fromPlayerChunks = (worldPos.xz - u_playerPosition.xz) / u_chunkSize;
    return dot(fromPlayerChunks, fromPlayerChunks);
}

vec3 getPatchChunkCenter(vec3 corner0, vec3 corner2, vec3 normal) {
    vec3 inside = (corner0 + corner2) * 0.5 - normal * 0.5;
    vec2 cell   = floor(inside.xz / u_chunkSize);
    return vec3((cell.x + 0.5) * u_chunkSize, inside.y, (cell.y + 0.5) * u_chunkSize);
}

// Center of the block that owns the patch's first corner, pulled half a block back along the face normal
// so a face lying exactly on a chunk boundary plane still resolves to the chunk that owns it.
vec3 getOriginBlockChunkCenter(vec3 corner0, vec3 corner1, vec3 corner3, vec3 normal) {
    vec3 tangentA = normalize(corner1 - corner0);
    vec3 tangentB = normalize(corner3 - corner0);

    vec3 inside = corner0 + (tangentA + tangentB) * 0.5 - normal * 0.5;
    vec2 cell   = floor(inside.xz / u_chunkSize);

    return vec3((cell.x + 0.5) * u_chunkSize, inside.y, (cell.y + 0.5) * u_chunkSize);
}

float getEdgeTessLevel(float chunkDistSq, float blockSize, float tier0MaxSqDist, float tier1MaxSqDist) {
    if (chunkDistSq <= tier0MaxSqDist)
    return clamp(blockSize * NEAR_TESSELLATION_DENSITY, 1.0, MAX_TESSELLATION_LEVEL);
    return clamp(blockSize, 1.0, MAX_TESSELLATION_LEVEL);
}

float getNearStrength(float distSq, float tier0MaxSqDist) {
    float dist      = sqrt(distSq);
    float tier0Dist = sqrt(tier0MaxSqDist);
    float fadeEnd   = max(tier0Dist - NEAR_FADE_END_CHUNKS, 0.0);
    float fadeStart = max(fadeEnd - NEAR_FADE_BAND_CHUNKS, 0.0);
    return 1.0 - smoothstep(fadeStart, fadeEnd, dist);
}

#endif