#version 330 core

layout (location = 0) in vec3 aPos;

#include "includes/CameraData.glsl"

// Placeholder — the per-instance overhead cloud box this shader used to
// draw has been removed in favor of a single stretched-cube overhead mesh.
// Kept minimal and valid so the shader pipeline still loads it cleanly.

void main() {
    gl_Position = u_viewProjection * vec4(aPos, 1.0);
}