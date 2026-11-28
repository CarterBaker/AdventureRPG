#ifndef WORLD_CURVATURE_GLSL
#define WORLD_CURVATURE_GLSL

#include "includes/CameraData.glsl"
#include "includes/WorldShapeData.glsl"

// Where a grid-relative position on a globe is drawn: lowered by the square of its level distance from the camera
// times the horizon curvature, the drop a ball's surface makes below the plane level under the eye, so the land and
// the sea fall away to a true horizon. Only where a vertex is drawn moves; every shading position stays flat, as the
// blocks are. A torus has no curvature and is drawn where it stands.
vec3 curveToHorizon(vec3 position) {
    vec2 offset = position.xz - u_cameraPosition.xz;

    return vec3(position.x, position.y - dot(offset, offset) * u_horizonCurvature, position.z);
}

#endif
