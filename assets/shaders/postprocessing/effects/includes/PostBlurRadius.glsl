#ifndef POST_BLUR_RADIUS_GLSL
#define POST_BLUR_RADIUS_GLSL

#include "includes/PostProcessData.glsl"

const float DOF_RADIUS_MIN_FRACTION = 0.004;
const float DOF_RADIUS_MAX_FRACTION = 0.028;

// The depth of field's widest blur in pixels of a target this many pixels
// tall, measured against the target's height so the look holds at every
// resolution.
float resolveDofRadius(float targetHeight) {
    return mix(DOF_RADIUS_MIN_FRACTION, DOF_RADIUS_MAX_FRACTION, u_dofBlur) * targetHeight;
}

#endif
