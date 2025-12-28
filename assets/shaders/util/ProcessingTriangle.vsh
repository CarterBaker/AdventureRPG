#version 150

in vec2 a_position;
out vec3 v_dir;

#include "includes/CameraData.glsl"

void main() {
    // Near plane point in clip space
    vec4 clip = vec4(a_position, -1.0, 1.0);

    // Unproject to view space
    vec4 view = u_inverseProjection * clip;
    view /= view.w;

    // Convert point → direction
    view = vec4(view.xyz, 0.0);

    // Rotate into world space
    v_dir = normalize((u_inverseView * view).xyz);

    gl_Position = vec4(a_position, 0.0, 1.0);
}