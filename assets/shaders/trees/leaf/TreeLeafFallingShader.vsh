#version 400 core
layout (location = 0) in vec3  aPos;
layout (location = 1) in vec2  aUVOrigin;
layout (location = 2) in float aMeta;
layout (location = 3) in float aColor;
layout (location = 4) in vec4  aEdgeLow;
layout (location = 5) in vec4  aEdgeHigh;

#include "includes/CameraData.glsl"
#include "trees/includes/TreeLeaf.glsl"

// Carries the falling piece, in blocks from the centre of its tree's root, into the world.
uniform mat4 u_model;

out vec3      vBoxPos;
flat out vec3 vCenter;
flat out vec3 vModelCenter;
flat out vec2 vRadii;
flat out float vSeed;
flat out vec2 vUVOrigin;
flat out vec3 vColor;

// A falling clump keeps its own centre and radii; its box turns with the piece, so the box is grown to hold the
// clump however it turns.

void main() {
    vec3 modelCenter = aPos + aEdgeLow.xyz;
    vec3 center      = (u_model * vec4(modelCenter, 1.0)).xyz;
    float reach      = max(aEdgeLow.w, aEdgeHigh.x);

    vBoxPos      = center + mat3(u_model) * (-aEdgeLow.xyz) * (reach / max(min(aEdgeLow.w, aEdgeHigh.x), 0.001));
    vCenter      = center;
    vModelCenter = modelCenter;
    vRadii       = vec2(aEdgeLow.w, aEdgeHigh.x);
    vSeed        = aEdgeHigh.y;
    vUVOrigin    = aUVOrigin;
    vColor       = unpackTreeColor(aColor);

    gl_Position = u_viewProjection * vec4(vBoxPos, 1.0);
}
