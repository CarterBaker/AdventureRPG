#ifndef SKY_BODIES_GLSL
#define SKY_BODIES_GLSL

#include "includes/CameraData.glsl"

/*
 * Shared by everything painted onto the sky. A body is a flat picture that
 * faces the viewer and stays upright to the camera, like a sticker on the
 * dome, while its centre follows the body's true direction. Pictures are
 * read with a small rotated-grid supersample so their outlines stay clean
 * however small they are drawn, and anything below the horizon is clipped by
 * a soft line rather than a hard one.
 */

const float SKY_BODY_HORIZON_SOFTNESS = 0.004;
const float SKY_BODY_OFF_PLANE        = 1.0e4;
const float SKY_BODY_BASIS_EPSILON    = 1.0e-4;

// ── Plane ──────────────────────────────────────────────────────────────────

// The camera-upright right and up of a body's picture plane. False when the
// body sits exactly off the camera's side and no upright plane exists.
bool resolveSkyBodyBasis(vec3 bodyDir, out vec3 right, out vec3 up) {
    vec3  cameraRight = u_inverseView[0].xyz;
    vec3  side        = cameraRight - bodyDir * dot(cameraRight, bodyDir);
    float sideLength  = length(side);

    right = vec3(1.0, 0.0, 0.0);
    up    = vec3(0.0, 1.0, 0.0);

    if (sideLength < SKY_BODY_BASIS_EPSILON)
        return false;

    right = side / sideLength;
    up    = cross(right, bodyDir);

    return true;
}

// Where a view direction lands on a body's picture plane, in radians from
// the body's centre along the plane's right and up. Directions facing away
// from the body land far off the plane.
vec2 resolveSkyBodyPlane(vec3 dir, vec3 bodyDir) {
    float facing = dot(dir, bodyDir);
    vec3  right;
    vec3  up;

    if (facing <= 0.0 || !resolveSkyBodyBasis(bodyDir, right, up))
        return vec2(SKY_BODY_OFF_PLANE);

    vec3 onPlane = dir / facing;

    return vec2(dot(onPlane, right), dot(onPlane, up));
}

// Turns a point on the picture plane by an angle in radians.
vec2 rotateSkyBodyPlane(vec2 point, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return vec2(c * point.x - s * point.y, s * point.x + c * point.y);
}

// ── Sampling ───────────────────────────────────────────────────────────────

// Reads a picture with four rotated-grid taps spread over one screen pixel,
// given in picture coordinates, so a large picture drawn small does not
// shimmer. Coordinates outside the picture read as empty.
vec4 sampleSkyBodyPicture(sampler2D picture, vec2 uv, float pixelSpan) {
    if (any(lessThan(uv, vec2(-pixelSpan))) || any(greaterThan(uv, vec2(1.0 + pixelSpan))))
        return vec4(0.0);

    vec4 sum = texture(picture, uv + vec2(-0.125, -0.375) * pixelSpan)
             + texture(picture, uv + vec2( 0.375, -0.125) * pixelSpan)
             + texture(picture, uv + vec2( 0.125,  0.375) * pixelSpan)
             + texture(picture, uv + vec2(-0.375,  0.125) * pixelSpan);

    return sum * 0.25;
}

// Lays a picture over a color by its own coverage.
vec3 layerSkyBodyPicture(vec3 color, vec4 picture, vec3 tint, float visibility) {
    return mix(color, picture.rgb * tint, clamp(picture.a * visibility, 0.0, 1.0));
}

// ── Horizon ────────────────────────────────────────────────────────────────

// How much of a direction still lies above the horizon.
float resolveSkyBodyHorizon(vec3 dir) {
    return smoothstep(-SKY_BODY_HORIZON_SOFTNESS, SKY_BODY_HORIZON_SOFTNESS, dir.y);
}

#endif
