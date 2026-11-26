#version 400 core

in vec3       vWorldPos;
in vec2       vAtlas;
flat in float vLayer;
flat in float vTurn;

#include "includes/CameraData.glsl"
#include "trees/includes/TreeShading.glsl"
#include "trees/includes/TreeImpostor.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

// A far tree's impostor written into the G-buffer as its tree would be: the albedo, darkened by the occlusion it was
// baked with, cut out where its frame shows no tree, the baked normal turned with the tree into view space, and the
// sky's shade at its own place.

void main() {
    vec3  layered = vec3(vAtlas, vLayer);
    vec4  albedo  = texture(u_impostorAlbedo, layered);

    if (albedo.a < TREE_IMPOSTOR_CUTOFF)
    discard;

    vec3 normal = turnTreeImpostor(texture(u_impostorNormal, layered).xyz * 2.0 - 1.0, vTurn);

    gAlbedo   = vec4(albedo.rgb, 1.0);
    gNormal   = vec4(normalize(mat3(u_view) * normal), 1.0);
    gMaterial = vec4(resolveTreeSunVisibility(vWorldPos), TREE_IMPOSTOR_SPECULAR, 1.0, 1.0);
}
