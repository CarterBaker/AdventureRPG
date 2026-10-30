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
#include "editor/treeviewer/TreeViewerLight.glsl"

// The tier's place along the viewer's row; the wood never turns.
uniform mat4 u_model;

out vec4 FragColor;

// Wood in the Tree Viewer: beveled and creased exactly as the world's, then lit here.
void main() {
    vec4 albedo;
    vec4 normal;
    vec4 material;

    shadeTreeBark(
        vWorldPos, vModelPos, vLocal, vSize, vUVOrigin, vColor, vFace, vEdges,
        mat3(u_model), albedo, normal, material);

    FragColor = vec4(albedo.rgb * resolveTreeViewerLight(normal.xyz, material.z), 1.0);
}
