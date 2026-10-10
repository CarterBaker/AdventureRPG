#ifndef SURFACE_TESSELLATION_TIER_GLSL
#define SURFACE_TESSELLATION_TIER_GLSL

#include "includes/SettingsData.glsl"
#include "includes/PlayerPositionData.glsl"

// Shared detail-radius math for StandardSurfaceShader's vsh/tcs/tes/fsh and the water's shore warp. One radius, in
// blocks, scopes every piece of near detail: the bevel, natural distortion, height relief and edge warp, the normal,
// specular and ambient occlusion maps, and the full growth of every covering. All of it shares one strength, full
// out to DETAIL_FADE_BAND_BLOCKS short of the radius and fading to nothing at the radius itself, so nothing pops in
// ahead of the rest. Tessellation density is decided from the chunk that owns the patch's origin sub-block rather
// than from a patch or edge midpoint: merged quads never cross a chunk, so every face of a given block resolves to
// one key, and a long quad whose center drifts into the neighbouring chunk can no longer pick a different density
// from the perpendicular face it shares an edge with. A chunk takes the near density while its center lies within
// DETAIL_DENSITY_MARGIN_BLOCKS beyond the radius; the margin exceeds half a chunk's diagonal, so every position the
// strength reaches lies in a near chunk and every edge where two densities meet is already flat. The near density
// places a vertex every quarter block on every patch, and the far density one vertex per edge-word entry — a block
// on block quads, a sub-block on sub-block quads — so every level is a whole number, every tessellated vertex lies on
// the quarter-block lattice, and two patches of different sizes place their shared vertices at the same lattice
// points. DETAIL_DENSITY_MARGIN_BLOCKS must match EngineSetting.NATURAL_NOISE_DETAIL_DENSITY_MARGIN_BLOCKS and
// SUB_BLOCK_SIZE EngineSetting.SUB_BLOCK_SIZE.

const float SUB_BLOCK_SIZE               = 0.5;
const float DETAIL_FADE_BAND_BLOCKS      = 8.0;
const float DETAIL_DENSITY_MARGIN_BLOCKS = 12.0;
const float NEAR_TESSELLATION_DENSITY    = 4.0;
const float MAX_TESSELLATION_LEVEL       = 64.0;
const float ORIGIN_SUB_BLOCK_HALF_SIZE   = SUB_BLOCK_SIZE * 0.5;

// Squared horizontal distance in blocks from the player
float computeDistanceFromPlayerSq(vec3 worldPos) {
    vec2 fromPlayer = worldPos.xz - u_playerPosition.xz;
    return dot(fromPlayer, fromPlayer);
}

// Strength of every piece of near detail at a position, 1 inside the radius' fade band down to 0 at the radius
float getDetailStrength(vec3 worldPos) {
    float fadeEnd   = max(u_detailRadius, 0.0);
    float fadeStart = max(fadeEnd - DETAIL_FADE_BAND_BLOCKS, 0.0);
    return 1.0 - smoothstep(fadeStart, fadeEnd, sqrt(computeDistanceFromPlayerSq(worldPos)));
}

float getNearDensityMaxSqDist() {
    float densityRadius = max(u_detailRadius, 0.0) + DETAIL_DENSITY_MARGIN_BLOCKS;
    return densityRadius * densityRadius;
}

// Center of the sub-block that owns the patch's first corner, pulled a quarter block back along the face
// normal so a face lying exactly on a chunk boundary plane still resolves to the chunk that owns it. A
// quarter block is inside the first sub-block on every axis, so the probe never lands on a sub-block or
// chunk boundary whether the patch is block or sub-block sized.
vec3 getOriginBlockChunkCenter(vec3 corner0, vec3 corner1, vec3 corner3, vec3 normal) {
    vec3 tangentA = normalize(corner1 - corner0);
    vec3 tangentB = normalize(corner3 - corner0);

    vec3 inside = corner0 + (tangentA + tangentB - normal) * ORIGIN_SUB_BLOCK_HALF_SIZE;
    vec2 cell   = floor(inside.xz / u_chunkSize);

    return vec3((cell.x + 0.5) * u_chunkSize, inside.y, (cell.y + 0.5) * u_chunkSize);
}

// sizeBlocks is the edge's length in blocks and entryBlocks the length one edge-word entry covers.
float getEdgeTessLevel(float chunkDistSq, float sizeBlocks, float entryBlocks, float nearDensityMaxSqDist) {
    if (chunkDistSq <= nearDensityMaxSqDist)
    return clamp(sizeBlocks * NEAR_TESSELLATION_DENSITY, 1.0, MAX_TESSELLATION_LEVEL);
    return clamp(sizeBlocks / entryBlocks, 1.0, MAX_TESSELLATION_LEVEL);
}

// Snaps a tessellated position to the quarter-block lattice every tessellated vertex ideally lies on, so two
// patches evaluating the same shared vertex from different corners and extents land on bit-identical input.
vec3 snapToTessellationLattice(vec3 position) {
    return round(position * NEAR_TESSELLATION_DENSITY) / NEAR_TESSELLATION_DENSITY;
}

#endif
