#version 400 core
layout (location = 0) in vec3 aPos;

#include "includes/CameraData.glsl"
#include "trees/includes/TreeImpostor.glsl"

// Where the impostor's sphere stands over the root, its radius, and its species' layer.
uniform float u_impostorCenterY;
uniform float u_impostorRadius;
uniform float u_impostorLayer;

out vec2       vAtlas;
flat out float vLayer;

// The impostor in the Tree Viewer: its quad faces the camera and shows the frame nearest the way it is seen, as the
// world's instances do, never turned.
void main() {
    vec3 center = vec3(0.0, u_impostorCenterY, 0.0);
    vec3 facing = normalize(u_cameraPosition - center);
    vec3 right  = resolveTreeImpostorRight(facing);
    vec3 up     = cross(facing, right);
    vec2 frame  = resolveTreeImpostorFrame(facing);

    vAtlas = (frame + aPos.xy * 0.5 + 0.5) / TREE_IMPOSTOR_FRAMES;
    vLayer = u_impostorLayer;

    gl_Position = u_viewProjection * vec4(center + (right * aPos.x + up * aPos.y) * u_impostorRadius, 1.0);
}
