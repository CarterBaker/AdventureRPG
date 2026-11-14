#ifndef HEIGHT_DISPLACEMENT_GLSL
#define HEIGHT_DISPLACEMENT_GLSL

#include "includes/TileTextures.glsl"

// Height-map displacement for the near ring. The caller supplies a single weight already folded from
// natural-block eligibility, distance fade, and the surface-boundary gradient, so displacement is exactly
// zero on every edge cell where the surface ends and only the bevel survives at a seam. A second window
// carries it to zero at every unit block boundary as well: the map repeats once per block, and adjacent
// blocks of a natural surface draw a different spin of the tile, so without that window the sampled height
// steps discontinuously at every internal block line and cracks the surface inside a single merged quad. The
// height is read at the mip level the vertex spacing calls for, so a coarse patch never aliases a fine map.
// Displacement rides the flat geometric normal, never the beveled one.

const float HEIGHT_DISPLACE_SCALE    = 0.12;
const float HEIGHT_BLOCK_EDGE_MARGIN = 0.25;

float heightBlockWindow(vec3 localPos, vec3 normal) {
    vec3 axis = abs(normal);
    vec2 planar = (axis.y > 0.5) ? localPos.xz : ((axis.x > 0.5) ? localPos.zy : localPos.xy);

    vec2 cell = fract(planar);
    vec2 window = smoothstep(0.0, HEIGHT_BLOCK_EDGE_MARGIN, cell)
    * (1.0 - smoothstep(1.0 - HEIGHT_BLOCK_EDGE_MARGIN, 1.0, cell));

    return window.x * window.y;
}

vec3 applyHeightDisplacement(
    vec3 worldPos, vec3 basePos, vec3 normal, int tileID, vec2 uv, float spacing, float weight) {
    if (weight <= 0.001)
    return worldPos;

    float window = heightBlockWindow(basePos, normal);

    if (window <= 0.001)
    return worldPos;

    float    footprint = spacing * TILE_BASE_SIZE;
    TileRead tile      = resolveTileRead(tileID, footprint);
    float    h         = sampleTileMaterialLod(tile, uv, log2(max(footprint, TILE_MIN_FOOTPRINT))).a;

    return worldPos + normal * ((h - 0.5) * HEIGHT_DISPLACE_SCALE * weight * window);
}

#endif