#version 400 core

in vec2       vAtlas;
flat in float vLayer;

#include "trees/includes/TreeImpostor.glsl"
#include "editor/treeviewer/TreeViewerLight.glsl"

out vec4 FragColor;

// The impostor in the Tree Viewer: cut out and lit by the viewer's light exactly as its frame was baked.
void main() {
    vec3 layered = vec3(vAtlas, vLayer);
    vec4 albedo  = texture(u_impostorAlbedo, layered);

    if (albedo.a < TREE_IMPOSTOR_CUTOFF)
    discard;

    vec3 normal = texture(u_impostorNormal, layered).xyz * 2.0 - 1.0;

    FragColor = vec4(albedo.rgb * resolveTreeViewerLight(mat3(u_view) * normal, 1.0), 1.0);
}
