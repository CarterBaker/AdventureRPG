#ifndef HEIGHT_DISPLACEMENT_GLSL
#define HEIGHT_DISPLACEMENT_GLSL

#include "surface/includes/SurfaceStandard.glsl"
#include "surface/includes/Bevel.glsl"

// Height-map displacement for the near ring only, faded to zero over the
// same exposure masks the bevel uses — well before the bevel radius starts
// — so a face's own height detail always dies out before its edge starts
// curving, and any two faces meeting at that edge converge back onto the
// same flat seam line rather than two independently bumpy surfaces.

const float HEIGHT_DISPLACE_SCALE   = 0.12;
const float HEIGHT_EDGE_FADE_RADIUS = 0.75;

vec3 applyHeightDisplacement(
    vec3 worldPos, vec3 normal, vec2 tiledUV,
    float posA, float posB, float sizeA, float sizeB, int iLastA, int iLastB,
    float maskA0, float maskA1, float maskB0, float maskB1,
    float nearStrength) {
    vec2  heightEdgeAxes = computeEdgeExposure(posA, posB, sizeA, sizeB, iLastA, iLastB,
        maskA0, maskA1, maskB0, maskB1, HEIGHT_EDGE_FADE_RADIUS);
    float heightWeight   = 1.0 - max(heightEdgeAxes.x, heightEdgeAxes.y);

    float h = texture(u_textureArray, vec3(tiledUV, float(u_layer_height))).r;
    return worldPos + normal * (h - 0.5) * HEIGHT_DISPLACE_SCALE * heightWeight * nearStrength;
}

#endif