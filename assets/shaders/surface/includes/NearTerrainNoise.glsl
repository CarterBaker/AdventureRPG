#ifndef NEAR_TERRAIN_NOISE_GLSL
#define NEAR_TERRAIN_NOISE_GLSL

#include "includes/NaturalNoiseData.glsl"

// Near-ring per-vertex jitter, sampled from the baked noise lattice so it
// can never disagree with physics sampling the same table. Amplitude fades
// with nearStrength rather than switching off abruptly, so a patch that
// straddles the near-ring boundary fades continuously across itself instead
// of stepping to zero mid-surface.

const float NEAR_JITTER_HORIZONTAL = 0.4;
const float NEAR_JITTER_VERTICAL   = 0.08;

vec3 applyNearTerrainJitter(vec3 worldPos, float nearStrength) {
    if (nearStrength <= 0.001)
    return worldPos;

    vec2  seed = worldPos.xz * NATURAL_NOISE_SEED_SCALE;
    float nX   = sampleNaturalNoiseSmooth(seed + vec2(17.3,  0.0)) - 0.5;
    float nZ   = sampleNaturalNoiseSmooth(seed + vec2(0.0, 31.7)) - 0.5;
    float nY   = sampleNaturalNoiseSmooth(seed + vec2(53.1, 83.2)) - 0.5;

    worldPos.x += nX * NEAR_JITTER_HORIZONTAL * nearStrength;
    worldPos.z += nZ * NEAR_JITTER_HORIZONTAL * nearStrength;
    worldPos.y += nY * NEAR_JITTER_VERTICAL   * nearStrength;

    return worldPos;
}

#endif