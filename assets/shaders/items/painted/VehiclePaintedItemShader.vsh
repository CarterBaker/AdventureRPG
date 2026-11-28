#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in vec3  aNormal;
layout(location = 2) in vec2  aUV;
layout(location = 3) in float aTile;

#include "includes/CameraData.glsl"
#include "includes/WorldCurvature.glsl"

// Carries the painted item onto the vehicle it stands on, its model position kept for a viewer's cutaway.
uniform mat4 u_model;

out vec2 vUV;
flat out int vTile;
out vec3 vModelPosition;
out vec3 vViewPosition;

void main() {
    vec4 worldPosition = u_model * vec4(aPos, 1.0);

    vUV           = aUV;
    vTile         = int(aTile + 0.5);
    vModelPosition = aPos;
    vViewPosition = (u_view * worldPosition).xyz;
    gl_Position   = u_viewProjection * vec4(curveToHorizon(worldPosition.xyz), worldPosition.w);
}
