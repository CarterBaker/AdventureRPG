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
#include "editor/treeimpostor/TreeImpostorBake.glsl"

out vec4 FragColor;

// The shared tree's wood written into one impostor map: beveled and creased exactly as the world's.
void main() {
    vec4 albedo;
    vec4 normal;
    vec4 material;

    shadeTreeBark(
        vWorldPos, vModelPos, vLocal, vSize, vTile, vColor, vFace, vEdges,
        mat3(1.0), albedo, normal, material);

    FragColor = resolveTreeImpostorBake(albedo, normal, material);
}
