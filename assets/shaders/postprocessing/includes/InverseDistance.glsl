#ifndef INVERSE_DISTANCE_GLSL
#define INVERSE_DISTANCE_GLSL

#include "includes/CameraData.glsl"

// One over the view distance of a depth buffer value, through the projection's
// two depth terms. Inverse distance runs linear across the screen over any
// plane, and the far plane and the sky beyond it read as zero, so it is safe
// for an infinite far plane.
float resolveInverseDistance(float rawDepth) {
    return max(((rawDepth * 2.0 - 1.0) + u_projection[2][2]) / u_projection[3][2], 0.0);
}

#endif
