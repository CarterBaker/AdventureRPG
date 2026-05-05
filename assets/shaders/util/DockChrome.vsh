#version 330 core

layout (location = 0) in vec2 a_position;

#include "includes/CameraData.glsl"

uniform mat4 u_transform;
uniform vec4 u_color;

out vec4 v_color;

void main() {
    vec2 screenPos = (u_transform * vec4(a_position, 0.0, 1.0)).xy;

    vec2 ndc;
    ndc.x = (screenPos.x / u_viewport.x) * 2.0 - 1.0;
    ndc.y = (screenPos.y / u_viewport.y) * 2.0 - 1.0;

    gl_Position = vec4(ndc, 0.0, 1.0);
    v_color = u_color;
}
