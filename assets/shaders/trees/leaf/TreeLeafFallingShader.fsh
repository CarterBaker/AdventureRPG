#version 400 core

in vec3       vBoxPos;
flat in vec3  vCenter;
flat in vec3  vModelCenter;
flat in vec2  vRadii;
flat in float vSeed;
flat in vec2  vUVOrigin;
flat in vec3  vColor;

#include "trees/includes/TreeLeaf.glsl"

// The same transform the vertices fell by, so a clump's leaves stay on it as it turns.
uniform mat4 u_model;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

void main() {
    float depth;

    if (!shadeTreeLeaf(
        vBoxPos, vCenter, vModelCenter, vRadii, vSeed, vUVOrigin, vColor, mat3(u_model),
        gAlbedo, gNormal, gMaterial, depth))
    discard;

    gl_FragDepth = depth;
}
