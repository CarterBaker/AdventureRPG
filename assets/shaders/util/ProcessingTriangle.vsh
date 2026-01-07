#version 150
in vec2 a_position;
out vec3 v_dir;
#include "includes/CameraData.glsl"

void main() {
    // Full-screen quad in clip space
    gl_Position = vec4(a_position, 0.9999, 1.0);

    // Unproject to get view ray direction
    vec4 viewRay = u_inverseProjection * vec4(a_position, 1.0, 1.0);
    viewRay /= viewRay.w;

    // Transform to world space (as direction vector, w=0)
    v_dir = (u_inverseView * vec4(viewRay.xyz, 0.0)).xyz;
}