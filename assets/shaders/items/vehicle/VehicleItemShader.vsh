#version 330 core
layout(location = 0) in vec3  aPos;
layout(location = 1) in float aNorIndex;
layout(location = 2) in vec2  aUV;

#include "includes/CameraData.glsl"

// Carries the vehicle's model grid, or one moving part of it, into the world.
uniform mat4 u_model;

out vec2 vUVOrigin;
out vec3 vModelPosition;
out vec3 vViewPosition;
flat out int vFace;

void main() {
    vec4 worldPosition = u_model * vec4(aPos, 1.0);

    vUVOrigin      = aUV;
    vModelPosition = aPos;
    vViewPosition  = (u_view * worldPosition).xyz;
    vFace          = int(aNorIndex);
    gl_Position    = u_viewProjection * worldPosition;
}
