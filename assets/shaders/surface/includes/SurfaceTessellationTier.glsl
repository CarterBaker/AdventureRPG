#ifndef SURFACE_TESSELLATION_TIER_GLSL
#define SURFACE_TESSELLATION_TIER_GLSL

#include "includes/SettingsData.glsl"
#include "includes/PlayerPositionData.glsl"

// Shared tier-distance math for StandardSurfaceShader's tcs/tes/fsh and for
// WaterShader's distant-rise gate. Every boundary is evaluated against the
// caller's own true world position, and the tessellation level is decided
// per edge from that edge's own midpoint rather than per patch, so any two
// patches sharing a physical edge always agree on both its tier and its
// density regardless of their own size or which ring their own center falls
// in — the same guarantee computeDistanceFromCenterSq already gave the
// fragment shader's material tier.

const float DISTANT_RISE_START_MARGIN_BLOCKS = 512.0;
const float NEAR_TESSELLATION_DENSITY        = 4.0;
const float MAX_TESSELLATION_LEVEL           = 64.0;
const float NEAR_FADE_BAND_CHUNKS            = 1.0;

float getTier1MaxSqDist() {
    float halfD        = u_renderDistance * 0.5 - 0.5;
    float marginChunks = DISTANT_RISE_START_MARGIN_BLOCKS / (u_chunkSize * sqrt(2.0));
    float farHalfD      = max(halfD - marginChunks, 1.0);
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

float getEdgeTessLevel(float edgeDistSq, float blockSize, float tier0MaxSqDist, float tier1MaxSqDist) {
    if (edgeDistSq <= tier0MaxSqDist)
    return clamp(blockSize * NEAR_TESSELLATION_DENSITY, 1.0, MAX_TESSELLATION_LEVEL);
    if (edgeDistSq > tier1MaxSqDist)
    return clamp(blockSize, 1.0, MAX_TESSELLATION_LEVEL);
    return 1.0;
}

float getNearStrength(float distSq, float tier0MaxSqDist) {
    float dist      = sqrt(distSq);
    float tier0Dist = sqrt(tier0MaxSqDist);
    return 1.0 - smoothstep(tier0Dist - NEAR_FADE_BAND_CHUNKS, tier0Dist, dist);
}

#endif