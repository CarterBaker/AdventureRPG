#ifndef NEAR_TERRAIN_NOISE_GLSL
#define NEAR_TERRAIN_NOISE_GLSL

#include "includes/NaturalNoiseData.glsl"

// Near-ring per-vertex jitter, sampled from the baked noise lattice so it can never disagree with
// physics sampling the same table. The seed is the undisplaced world position, so the lookup itself is
// seam-free by construction; the weight the caller passes carries the same boundary fade the height map
// uses, which keeps a natural surface from dragging a non-natural neighbor apart at their shared edge.

const float NEAR_JITTER_HORIZONTAL = 0.4;
const float NEAR_JITTER_VERTICAL   = 0.08;

vec3 applyNearTerrainJitter(vec3 worldPos, vec3 seedPos, float weight) {
    if (weight <= 0.001)
    return worldPos;

    vec2  seed = seedPos.xz * NATURAL_NOISE_SEED_SCALE;
    float nX   = sampleNaturalNoiseSmooth(seed + vec2(17.3,  0.0)) - 0.5;
    float nZ   = sampleNaturalNoiseSmooth(seed + vec2(0.0, 31.7)) - 0.5;
    float nY   = sampleNaturalNoiseSmooth(seed + vec2(53.1, 83.2)) - 0.5;

    worldPos.x += nX * NEAR_JITTER_HORIZONTAL * weight;
    worldPos.z += nZ * NEAR_JITTER_HORIZONTAL * weight;
    worldPos.y += nY * NEAR_JITTER_VERTICAL   * weight;

    return worldPos;
}

#endif