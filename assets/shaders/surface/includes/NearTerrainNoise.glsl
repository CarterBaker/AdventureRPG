#ifndef NEAR_TERRAIN_NOISE_GLSL
#define NEAR_TERRAIN_NOISE_GLSL

#include "includes/NaturalNoiseData.glsl"

// Near-ring natural surface noise, sampled from the baked lattice so it can never disagree with physics sampling
// the same table (NaturalNoiseUtility.sampleFields() is the formula-for-formula CPU copy). Three projections of
// the world position are read once and feed two fields, both pure functions of position and therefore seam-free
// by construction. The detail vector is split against the face normal so the dominant component rides the
// normal and is visible on a horizontal surface; it fades at every fold and seam like the height relief. The
// edge warp is not split and never fades at a fold: every patch sharing a vertex moves it by the same offset, so
// natural edges and corners bend out of their straight lines without ever opening a crack, and only artificial
// blocks hold it back. A second, finer octave of the warp breaks each edge up within a single block. The
// amplitudes must match EngineSetting.NATURAL_DETAIL_NORMAL_AMPLITUDE_BLOCKS, NATURAL_EDGE_WARP_* and
// NATURAL_NOISE_PLANE_OFFSET_CELLS.

const float NATURAL_NOISE_NORMAL_AMPLITUDE    = 0.22;
const float NATURAL_NOISE_TANGENT_AMPLITUDE   = 0.16;
const float NATURAL_EDGE_WARP_HORIZONTAL      = 0.35;
const float NATURAL_EDGE_WARP_VERTICAL        = 0.18;
const float NATURAL_EDGE_WARP_DETAIL_SHARE    = 0.5;
const float NATURAL_NOISE_PLANE_OFFSET        = 11.37;

void sampleNaturalFields(vec3 worldPos, out vec3 detail, out vec3 warp) {
    vec3 lattice = toNaturalNoiseLattice(worldPos);

    vec2 planeXZ = lattice.xz;
    vec2 planeZY = vec2(lattice.z + NATURAL_NOISE_PLANE_OFFSET, lattice.y);
    vec2 planeXY = vec2(lattice.x, lattice.y + NATURAL_NOISE_PLANE_OFFSET);

    vec4 xz = sampleNaturalNoise(planeXZ);
    vec4 zy = sampleNaturalNoise(planeZY);
    vec4 xy = sampleNaturalNoise(planeXY);

    detail = vec3(xz.x + zy.x, xz.y + xy.x, zy.y + xy.y) * 0.5;

    vec3 primary = vec3(xz.z + zy.z, xz.w + xy.z, zy.w + xy.w) * 0.5;

    vec4 fineXZ = sampleNaturalNoise(planeXZ * 2.0);
    vec4 fineZY = sampleNaturalNoise(planeZY * 2.0);
    vec4 fineXY = sampleNaturalNoise(planeXY * 2.0);

    vec3 fine = vec3(fineXZ.z + fineZY.z, fineXZ.w + fineXY.z, fineZY.w + fineXY.w) * 0.5;

    warp = (primary + fine * NATURAL_EDGE_WARP_DETAIL_SHARE) / (1.0 + NATURAL_EDGE_WARP_DETAIL_SHARE)
        * vec3(NATURAL_EDGE_WARP_HORIZONTAL, NATURAL_EDGE_WARP_VERTICAL, NATURAL_EDGE_WARP_HORIZONTAL);
}

vec3 applyNaturalSurfaceNoise(vec3 worldPos, vec3 detail, vec3 normal, float weight) {
    if (weight <= 0.001)
    return worldPos;

    float along      = dot(detail, normal);
    vec3  tangential = detail - normal * along;

    return worldPos + (normal * (along * NATURAL_NOISE_NORMAL_AMPLITUDE)
        + tangential * NATURAL_NOISE_TANGENT_AMPLITUDE) * weight;
}

#endif
