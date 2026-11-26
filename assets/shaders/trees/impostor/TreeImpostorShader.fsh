#version 400 core

in vec3       vWorldPos;
in vec2       vQuad;
flat in vec2  vFrameA;
flat in vec2  vFrameB;
flat in vec2  vFrameC;
flat in vec3  vWeights;
flat in float vLayer;
flat in float vTurn;
flat in vec2  vFade;

#include "includes/CameraData.glsl"
#include "trees/includes/TreeShading.glsl"
#include "trees/includes/TreeImpostor.glsl"
#include "trees/includes/TreeImpostorFade.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

// A far tree's impostor written into the G-buffer as its tree would be: the albedo, darkened by the occlusion it was
// baked with, blended from the three nearest frames and cut out where they show no tree, the baked normal turned with
// the tree into view space, and the sky's shade at its own place; it rises in or falls away through the trees'
// dither while its fade runs.

void main() {
    if (isTreeImpostorFaded(vFade.x, vFade.y))
    discard;

    vec4 albedo = sampleTreeImpostor(u_impostorAlbedo, vQuad, vFrameA, vFrameB, vFrameC, vWeights, vLayer);

    if (albedo.a < TREE_IMPOSTOR_CUTOFF)
    discard;

    vec3 baked  = sampleTreeImpostor(u_impostorNormal, vQuad, vFrameA, vFrameB, vFrameC, vWeights, vLayer).xyz;
    vec3 normal = turnTreeImpostor(baked * 2.0 - 1.0, vTurn);

    gAlbedo   = vec4(albedo.rgb, 1.0);
    gNormal   = vec4(normalize(mat3(u_view) * normal), 1.0);
    gMaterial = vec4(resolveTreeSunVisibility(vWorldPos), TREE_IMPOSTOR_SPECULAR, 1.0, 1.0);
}
