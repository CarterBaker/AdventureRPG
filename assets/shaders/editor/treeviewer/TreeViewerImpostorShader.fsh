#version 400 core

in vec2       vQuad;
flat in vec2  vFrameA;
flat in vec2  vFrameB;
flat in vec2  vFrameC;
flat in vec3  vWeights;
flat in float vLayer;

#include "trees/includes/TreeImpostor.glsl"
#include "editor/treeviewer/TreeViewerLight.glsl"

out vec4 FragColor;

// The impostor in the Tree Viewer: blended from its three nearest frames, cut out and lit by the viewer's light.
void main() {
    vec4 albedo = sampleTreeImpostor(u_impostorAlbedo, vQuad, vFrameA, vFrameB, vFrameC, vWeights, vLayer);

    if (albedo.a < TREE_IMPOSTOR_CUTOFF)
    discard;

    vec3 normal = sampleTreeImpostor(u_impostorNormal, vQuad, vFrameA, vFrameB, vFrameC, vWeights, vLayer).xyz;

    FragColor = vec4(albedo.rgb * resolveTreeViewerLight(mat3(u_view) * (normal * 2.0 - 1.0), 1.0), 1.0);
}
