#version 400 core

in vec3       vBoxPos;
flat in vec3  vCenter;
flat in vec3  vModelCenter;
flat in vec2  vRadii;
flat in float vSeed;
flat in vec2  vUVOrigin;
flat in vec3  vColor;

#include "trees/includes/TreeLeaf.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

// Leaves standing in a chunk keep the world's axes.

void main() {
    float depth;

    if (!shadeTreeLeaf(
        vBoxPos, vCenter, vModelCenter, vRadii, vSeed, vUVOrigin, vColor, mat3(1.0),
        gAlbedo, gNormal, gMaterial, depth))
    discard;

    gl_FragDepth = depth;
}
