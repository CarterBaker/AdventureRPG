#version 330 core

in vec2 a_position;
out vec2 v_uv;

uniform vec4 u_destRect;

#include "includes/CameraData.glsl"

void main() {
    vec2 finalPos = a_position;

    if (u_destRect.z > 0.0 && u_destRect.w > 0.0) {
        vec2 minNdc;
        minNdc.x = (u_destRect.x / u_viewport.x) * 2.0 - 1.0;
        minNdc.y = 1.0 - ((u_destRect.y + u_destRect.w) / u_viewport.y) * 2.0;

        vec2 maxNdc;
        maxNdc.x = ((u_destRect.x + u_destRect.z) / u_viewport.x) * 2.0 - 1.0;
        maxNdc.y = 1.0 - (u_destRect.y / u_viewport.y) * 2.0;

        vec2 normalized = a_position * 0.5 + 0.5;
        finalPos = mix(minNdc, maxNdc, normalized);
    }

    gl_Position = vec4(finalPos, 0.0, 1.0);
    v_uv = a_position * 0.5 + 0.5;
}
