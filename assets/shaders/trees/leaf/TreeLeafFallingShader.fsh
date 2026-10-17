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

// The same transform the vertices fell by, so a cluster's dome of light turns with it.
uniform mat4 u_model;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

void main() {
    if (!shadeTreeLeaf(
        vWorldPos, vCard, vCenter, vRadii, vCardRadii, vUnderside, vSeed, vUVOrigin, vColor, mat3(u_model),
        gAlbedo, gNormal, gMaterial))
    discard;
}
