#version 400 core

in vec3      vWorldPos;
in vec3      vModelPos;
in vec2      vLocal;
flat in vec2 vSize;
flat in vec2 vUVOrigin;
flat in vec3 vColor;
flat in int  vFace;
flat in int  vEdges;

#include "trees/includes/TreeBark.glsl"
#include "trees/includes/TreeFade.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

// Wood standing in a chunk or mega keeps the world's axes, and fades between looks with its tree.

void main() {
    if (isTreeFaded())
    discard;

    shadeTreeBark(
        vWorldPos, vModelPos, vLocal, vSize, vUVOrigin, vColor, vFace, vEdges,
        mat3(1.0), gAlbedo, gNormal, gMaterial);
}
