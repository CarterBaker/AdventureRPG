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

#include "trees/includes/TreeLeafSprig.glsl"
#include "editor/treeimpostor/TreeImpostorBake.glsl"

out vec4 FragColor;

// The shared tree's leaves written into one impostor map: cut from their sprigs and domed exactly as the world's.
void main() {
    vec4 albedo;
    vec4 normal;
    vec4 material;

    if (!shadeTreeLeaf(
        vWorldPos, vCard, vCenter, vRadii, vCardRadii, vUnderside, vSeed, vTile, vColor, mat3(1.0),
        albedo, normal, material))
    discard;

    FragColor = resolveTreeImpostorBake(albedo, normal, material);
}
