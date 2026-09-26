#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in float aNorIndex;
layout(location = 2) in vec2  aUV;

#include "includes/CameraData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/WorldCurvature.glsl"

// Carries the item's one-block cube onto its anchor on the posed character.
uniform mat4 u_model;

out vec2 vUV;
out vec3 vViewPosition;

void main() {
    vec4 worldPosition = u_model * vec4(aPos, 1.0);
    worldPosition.xyz  = applyWorldCurvature(worldPosition.xyz);

    vUV           = aUV;
    vViewPosition = (u_view * worldPosition).xyz;
    gl_Position   = u_viewProjection * worldPosition;
}
