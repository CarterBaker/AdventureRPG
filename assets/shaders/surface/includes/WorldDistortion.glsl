#ifndef WORLD_DISTORTION_GLSL
#define WORLD_DISTORTION_GLSL

#include "includes/WorldDistortionData.glsl"

/*
* Bilinearly samples the precomputed world-warp lattice (see
 * WorldDistortionManager) at a world-space XZ position, wrapping every
 * WORLD_DISTORTION_GRID_SIZE units so the field tiles seamlessly across
 * chunk borders. BlockCollisionBranch reads the same underlying lattice
 * cell at whole-block granularity, so a natural block's rendered wobble
 * and its collision volume always agree on direction and magnitude.
 */
vec3 sampleWorldDistortion(vec3 localPos) {
    vec2 cell = vec2(
        mod(localPos.x, float(WORLD_DISTORTION_GRID_SIZE)),
        mod(localPos.z, float(WORLD_DISTORTION_GRID_SIZE)));

    ivec2 i0 = ivec2(floor(cell));
    vec2  f  = fract(cell);

    ivec2 i1 = (i0 + ivec2(1)) & ivec2(WORLD_DISTORTION_GRID_SIZE - 1);
    i0 = i0 & ivec2(WORLD_DISTORTION_GRID_SIZE - 1);

    vec3 c00 = u_worldDistortion[i0.y * WORLD_DISTORTION_GRID_SIZE + i0.x].xyz;
    vec3 c10 = u_worldDistortion[i0.y * WORLD_DISTORTION_GRID_SIZE + i1.x].xyz;
    vec3 c01 = u_worldDistortion[i1.y * WORLD_DISTORTION_GRID_SIZE + i0.x].xyz;
    vec3 c11 = u_worldDistortion[i1.y * WORLD_DISTORTION_GRID_SIZE + i1.x].xyz;

    vec3 x0 = mix(c00, c10, f.x);
    vec3 x1 = mix(c01, c11, f.x);

    return mix(x0, x1, f.y);
}

#endif