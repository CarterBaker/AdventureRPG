#version 330 core
in vec2 a_position;
out vec3 v_dir;
out vec2 v_screenPos;
#include "includes/CameraData.glsl"

void main() {
    // a_position arrives as the shared unit quad in [0,1] — remap to NDC.
    vec2 ndc = a_position * 2.0 - 1.0;

    gl_Position = vec4(ndc, 0.9999, 1.0);
    v_screenPos = ndc;

    // Unproject from clip space to view space
    // Use far plane (z = 1.0 in clip space maps to far plane)
    vec4 viewPos = u_inverseProjection * vec4(ndc, 1.0, 1.0);
    viewPos.xyz /= viewPos.w;

    // Transform view direction to world space
    // Use w=0.0 because we only want the direction, not position
    v_dir = (u_inverseView * vec4(viewPos.xyz, 0.0)).xyz;
}