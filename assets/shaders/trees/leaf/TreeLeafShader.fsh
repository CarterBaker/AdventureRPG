#version 400 core

in vec3       vWorldPos;
in vec2       vCard;
flat in vec3  vCenter;
flat in vec2  vRadii;
flat in vec2  vCardRadii;
flat in float vUnderside;
flat in float vSeed;
flat in int   vTile;
flat in vec3  vColor;
flat in int   vCover;

#include "trees/includes/TreeLeafSprig.glsl"
#include "trees/includes/TreeFade.glsl"
#include "includes/GBufferSurface.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;
layout(location = 3) out vec4 gSurface;

// Leaves standing in a chunk or mega keep the world's axes and fade between looks with their tree; a macro tile's far
// tree yields while the grid draws its chunk (TreeFade).

void main() {
    gSurface = GBUFFER_SURFACE_NONE;

    if (isTreeFaded() || isTreeYielded(vCover))
    discard;

    if (!shadeTreeLeaf(
        vWorldPos, vCard, vCenter, vRadii, vCardRadii, vUnderside, vSeed, vTile, vColor, mat3(1.0),
        gAlbedo, gNormal, gMaterial))
    discard;
}
