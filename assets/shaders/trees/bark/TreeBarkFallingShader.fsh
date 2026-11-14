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

// The same transform the vertices fell by, so the rounded wood turns with them.
uniform mat4 u_model;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

void main() {
    shadeTreeBark(
        vWorldPos, vModelPos, vLocal, vSize, vTile, vColor, vFace, vEdges,
        mat3(u_model), gAlbedo, gNormal, gMaterial);
}
