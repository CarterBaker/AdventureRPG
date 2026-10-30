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
#include "editor/treeviewer/TreeViewerLight.glsl"

// The tier's place along the viewer's row; the clusters never turn.
uniform mat4 u_model;

out vec4 FragColor;

// Leaves in the Tree Viewer: cut from their sprigs and domed exactly as the world's, then lit here.
void main() {
    vec4 albedo;
    vec4 normal;
    vec4 material;

    if (!shadeTreeLeaf(
        vWorldPos, vCard, vCenter, vRadii, vCardRadii, vUnderside, vSeed, vUVOrigin, vColor, mat3(u_model),
        albedo, normal, material))
    discard;

    FragColor = vec4(albedo.rgb * resolveTreeViewerLight(normal.xyz, material.z), 1.0);
}
