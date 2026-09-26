#ifndef SURFACE_TESSELLATION_TIER_GLSL
#define SURFACE_TESSELLATION_TIER_GLSL

#include "includes/SettingsData.glsl"
#include "includes/PlayerPositionData.glsl"

// Shared tier-distance math for StandardSurfaceShader's vsh/tcs/tes/fsh. Tessellation
// density is decided from the chunk that owns the patch's origin sub-block rather than from a patch or edge
// midpoint: merged quads never cross a chunk, so every face of a given block resolves to one key, and a
// long quad whose center drifts into the neighbouring chunk can no longer pick a different tier from the
// perpendicular face it shares an edge with. The near tier places a vertex every quarter block on every
// patch, and the far tier one vertex per edge-word entry — a block on block quads, a sub-block on
// sub-block quads — so every level is a whole number, every tessellated vertex lies on the quarter-block
// lattice, and two patches of different sizes place their shared vertices at the same lattice points.
// Displacement strength fades to zero a full chunk inside the density boundary, so wherever two chunks
// disagree on density the surface there is already flat. The mid tier ends MID_TIER_MARGIN_BLOCKS short of
// the render edge, and must match EngineSetting.NATURAL_NOISE_MID_TIER_MARGIN_BLOCKS. SUB_BLOCK_SIZE must match
// EngineSetting.SUB_BLOCK_SIZE.

const float SUB_BLOCK_SIZE              = 0.5;
const float MID_TIER_MARGIN_BLOCKS      = 512.0;
const float NEAR_TESSELLATION_DENSITY   = 4.0;
const float MAX_TESSELLATION_LEVEL      = 64.0;
const float NEAR_FADE_END_CHUNKS        = 1.0;
const float NEAR_FADE_BAND_CHUNKS       = 1.0;
const float ORIGIN_SUB_BLOCK_HALF_SIZE  = SUB_BLOCK_SIZE * 0.5;

float getTier1MaxSqDist() {
    float halfD        = u_renderDistance * 0.5 - 0.5;
    float marginChunks = MID_TIER_MARGIN_BLOCKS / (u_chunkSize * sqrt(2.0));
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
float getEdgeTessLevel(
    float chunkDistSq, float sizeBlocks, float entryBlocks,
    float tier0MaxSqDist, float tier1MaxSqDist) {
    if (chunkDistSq <= tier0MaxSqDist)
    return clamp(sizeBlocks * NEAR_TESSELLATION_DENSITY, 1.0, MAX_TESSELLATION_LEVEL);
    return clamp(sizeBlocks / entryBlocks, 1.0, MAX_TESSELLATION_LEVEL);
}

// Snaps a tessellated position to the quarter-block lattice every tessellated vertex ideally lies on, so two
// patches evaluating the same shared vertex from different corners and extents land on bit-identical input.
vec3 snapToTessellationLattice(vec3 position) {
    return round(position * NEAR_TESSELLATION_DENSITY) / NEAR_TESSELLATION_DENSITY;
}

float getNearStrength(float distSq, float tier0MaxSqDist) {
    float dist      = sqrt(distSq);
    float tier0Dist = sqrt(tier0MaxSqDist);
    float fadeEnd   = max(tier0Dist - NEAR_FADE_END_CHUNKS, 0.0);
    float fadeStart = max(fadeEnd - NEAR_FADE_BAND_CHUNKS, 0.0);
    return 1.0 - smoothstep(fadeStart, fadeEnd, dist);
}

#endif
