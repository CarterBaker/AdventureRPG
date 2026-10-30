#version 400 core

in vec3       vWorldPos;
in vec2       vCard;
flat in vec3  vCenter;
flat in vec2  vRadii;
flat in vec2  vCardRadii;
flat in float vUnderside;
flat in float vSeed;
flat in vec2  vUVOrigin;
flat in vec3  vColor;

#include "trees/includes/TreeLeafSprig.glsl"
#include "trees/includes/TreeFade.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

// Leaves standing in a chunk or mega keep the world's axes, and fade between looks with their tree.

void main() {
    if (isTreeFaded())
    discard;

    if (!shadeTreeLeaf(
        vWorldPos, vCard, vCenter, vRadii, vCardRadii, vUnderside, vSeed, vUVOrigin, vColor, mat3(1.0),
        gAlbedo, gNormal, gMaterial))
    discard;
}
