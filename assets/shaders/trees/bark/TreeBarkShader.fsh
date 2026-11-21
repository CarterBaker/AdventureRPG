#version 400 core

in vec3      vWorldPos;
in vec3      vModelPos;
in vec2      vLocal;
flat in vec2 vSize;
flat in int  vTile;
flat in vec3 vColor;
flat in int  vFace;
flat in int  vEdges;

#include "trees/includes/TreeBarkShading.glsl"
#include "trees/includes/TreeFade.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

// Wood standing in a chunk or mega keeps the world's axes, fades between looks with its tree, and hands over to the
// macro terrain's stand-ins far out (TreeFade).

void main() {
    if (isTreeFaded() || isTreeHandedOver(vWorldPos))
    discard;

    shadeTreeBark(
        vWorldPos, vModelPos, vLocal, vSize, vTile, vColor, vFace, vEdges,
        mat3(1.0), gAlbedo, gNormal, gMaterial);
}
