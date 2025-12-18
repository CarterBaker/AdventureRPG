#version 150

in vec2 a_position;
out vec3 v_dir;

#include "includes/CameraData.glsl"

void main() {
    // Convert from clip space to view space ray
    vec4 clip = vec4(a_position, 0.0, 1.0);
    vec4 view = u_inverseProjection * clip;
    view.z = -1.0;
    view.w = 0.0;

    // Transform into world space direction
    v_dir = normalize((u_inverseView * view).xyz);
    gl_Position = vec4(a_position, 0.0, 1.0);
}