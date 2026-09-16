#ifndef NEAR_TERRAIN_NOISE_GLSL
#define NEAR_TERRAIN_NOISE_GLSL

#include "includes/NaturalNoiseData.glsl"

// Near-ring natural surface distortion, sampled from the baked noise lattice so it can never disagree with
// physics sampling the same table. Three lattice taps on three orthogonal projections of the world position
// form a vector field that is a pure function of position, which keeps the lookup seam-free by construction;
// that vector is then split against the face normal so the dominant component rides the normal and is
// actually visible on a horizontal surface, with a smaller in-plane component warping the silhouette. The
// old axis-aligned form put its large amplitudes on X and Z, which on a top face are both in-plane and
// therefore invisible, leaving only a few hundredths of a block of real relief.

const float NATURAL_NOISE_NORMAL_AMPLITUDE  = 0.22;
const float NATURAL_NOISE_TANGENT_AMPLITUDE = 0.16;

vec3 naturalNoiseVector(vec3 worldPos) {
    vec2 planeXZ = worldPos.xz * NATURAL_NOISE_SEED_SCALE;
    vec2 planeZY = worldPos.zy * NATURAL_NOISE_SEED_SCALE;
    vec2 planeXY = worldPos.xy * NATURAL_NOISE_SEED_SCALE;

    return vec3(
        sampleNaturalNoiseSmooth(planeXZ + vec2(17.3,  5.1)) +
        sampleNaturalNoiseSmooth(planeZY + vec2(61.7, 23.9)),
        sampleNaturalNoiseSmooth(planeXZ + vec2(91.2, 44.6)) +
        sampleNaturalNoiseSmooth(planeXY + vec2(13.8, 77.4)),
        sampleNaturalNoiseSmooth(planeZY + vec2(7.5, 68.2)) +
        sampleNaturalNoiseSmooth(planeXY + vec2(55.1, 31.6))) - 1.0;
}

vec3 applyNaturalSurfaceNoise(vec3 worldPos, vec3 seedPos, vec3 normal, float weight) {
    if (weight <= 0.001)
    return worldPos;

    vec3  noise      = naturalNoiseVector(seedPos);
    float along      = dot(noise, normal);
    vec3  tangential = noise - normal * along;

    return worldPos + (normal * (along * NATURAL_NOISE_NORMAL_AMPLITUDE)
        + tangential * NATURAL_NOISE_TANGENT_AMPLITUDE) * weight;
}

#endif