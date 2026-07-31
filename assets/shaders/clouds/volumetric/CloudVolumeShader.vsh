#version 330 core

layout (location = 0) in vec3 aPos;

#include "includes/CameraData.glsl"

out vec3 vLocalPos;

uniform vec3 u_boxCenter;
uniform vec3 u_boxHalfExtent;

void main() {
    vec3 localPos = aPos - vec3(0.5);
    vLocalPos = localPos;

    vec3 worldPos = u_boxCenter + localPos * u_boxHalfExtent * 2.0;

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}