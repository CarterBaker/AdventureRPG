#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUV;

#include "includes/CameraData.glsl"

out vec3 vNormalView;
out vec3 vViewPosition;
out vec2 vUV;

// A Material Viewer shape standing at the origin, its normal carried into view space for the viewer's light.
void main() {
    vNormalView   = normalize(mat3(u_view) * aNormal);
    vViewPosition = (u_view * vec4(aPos, 1.0)).xyz;
    vUV           = aUV;
    gl_Position   = u_viewProjection * vec4(aPos, 1.0);
}
