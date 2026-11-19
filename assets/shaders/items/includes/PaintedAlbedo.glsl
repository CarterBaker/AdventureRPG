#ifndef PAINTED_ALBEDO_GLSL
#define PAINTED_ALBEDO_GLSL

#include "includes/TileTextures.glsl"

// A painted item is unwrapped and painted like any mesh, so its faces carry their own UVs into the one tile its maps
// were baked and painted into, v running up as every engine UV does; its screen gradients pick the tier and mip.

// A painted face's albedo at its UV. Takes derivatives, so call it from uniform control flow.
vec4 samplePaintedAlbedo(int tile, vec2 uv) {
    vec2 dx = dFdx(uv);
    vec2 dy = dFdy(uv);

    return sampleTileAlbedo(resolveTileReadGrad(tile, dx, dy), uv, dx, dy);
}

#endif
