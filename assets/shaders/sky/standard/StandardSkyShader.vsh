#version 330 core

in vec2 a_position;

out vec3 v_dir;

#include "includes/CameraData.glsl"

void main() {
    gl_Position = vec4(a_position, 0.9999, 1.0);

    vec4 viewPos = u_inverseProjection * vec4(a_position, 1.0, 1.0);
    viewPos.xyz /= viewPos.w;

    v_dir = (u_inverseView * vec4(viewPos.xyz, 0.0)).xyz;
}