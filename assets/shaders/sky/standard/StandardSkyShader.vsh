#version 330 core

in vec3 a_position;
out vec3 v_dir;

#include "includes/CameraData.glsl"

void main() {
    vec2 ndc = a_position.xy;

    gl_Position = vec4(ndc, 1.0, 1.0);

    vec4 farPoint = u_inverseProjection * vec4(ndc, 1.0, 1.0);
    vec3 viewDir = normalize(farPoint.xyz / farPoint.w);

    v_dir = mat3(u_inverseView) * viewDir;
}