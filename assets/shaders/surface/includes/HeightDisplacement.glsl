#ifndef HEIGHT_DISPLACEMENT_GLSL
#define HEIGHT_DISPLACEMENT_GLSL

#include "surface/includes/SurfaceStandard.glsl"

// Height-map displacement for the near ring. The caller supplies a single weight already folded from
// natural-block eligibility, distance fade, and the surface-boundary gradient, so displacement is
// guaranteed to be exactly zero on every edge cell where the surface ends. That is what lets two faces
// meeting at such an edge land on one point: at the seam only the bevel contributes, and the bevel is
// symmetric by construction. Displacement rides the flat geometric normal, never the beveled one.

const float HEIGHT_DISPLACE_SCALE = 0.12;

vec3 applyHeightDisplacement(vec3 worldPos, vec3 normal, vec2 tiledUV, float weight) {
    if (weight <= 0.001)
    return worldPos;

    float h = texture(u_textureArray, vec3(tiledUV, float(u_layer_height))).r;
    return worldPos + normal * (h - 0.5) * HEIGHT_DISPLACE_SCALE * weight;
}

#endif