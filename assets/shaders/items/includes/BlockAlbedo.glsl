#ifndef BLOCK_ALBEDO_GLSL
#define BLOCK_ALBEDO_GLSL

#include "includes/TileTextures.glsl"
#include "items/includes/BlockRepeat.glsl"

// A sub-voxel face's tile read at a model position, for the fragment stage only: its UV repeats once per block and
// its screen gradients, taken here, pick the tier and mip the read uses.

// A face's albedo at a model position. Takes derivatives, so call it from uniform control flow.
vec4 sampleBlockAlbedo(int tile, vec3 position, int face) {
    vec2 uv = resolveBlockUV(position, face);
    vec2 dx = dFdx(uv);
    vec2 dy = dFdy(uv);

    return sampleTileAlbedo(resolveTileReadGrad(tile, dx, dy), uv, dx, dy);
}

#endif
