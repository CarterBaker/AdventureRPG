#version 150
in vec2 a_position;
out vec3 v_dir;
#include "includes/CameraData.glsl"

void main() {
    // Full-screen quad in clip space
    gl_Position = vec4(a_position, 0.9999, 1.0);

    // Unproject from clip space to view space
    vec4 viewPos = u_inverseProjection * vec4(a_position, 1.0, 1.0);
    viewPos.xyz /= viewPos.w;

    // Transform view direction to world space
    v_dir = normalize((u_inverseView * vec4(viewPos.xyz, 0.0)).xyz);
}