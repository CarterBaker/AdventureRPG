#ifndef HEIGHT_DISPLACEMENT_GLSL
#define HEIGHT_DISPLACEMENT_GLSL

#include "surface/includes/SurfaceStandard.glsl"

// Height-map displacement for the near ring. The caller supplies a single weight already folded from
// natural-block eligibility, distance fade, and the surface-boundary gradient, so displacement is exactly
// zero on every edge cell where the surface ends and only the bevel survives at a seam. A second window
// carries it to zero at every unit block boundary as well: the map is sampled per block through fract(),
// and adjacent blocks of a natural surface draw a different spin of the tile, so without that window the
// sampled height steps discontinuously at every internal block line and cracks the surface inside a single
// merged quad. Displacement rides the flat geometric normal, never the beveled one.

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

vec3 applyHeightDisplacement(vec3 worldPos, vec3 basePos, vec3 normal, vec2 tiledUV, float weight) {
    if (weight <= 0.001)
    return worldPos;

    float window = heightBlockWindow(basePos, normal);

    if (window <= 0.001)
    return worldPos;

    float h = texture(u_textureArray, vec3(tiledUV, float(u_layer_height))).r;

    return worldPos + normal * ((h - 0.5) * HEIGHT_DISPLACE_SCALE * weight * window);
}

#endif