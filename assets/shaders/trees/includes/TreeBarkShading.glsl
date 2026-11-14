#ifndef TREE_BARK_SHADING_GLSL
#define TREE_BARK_SHADING_GLSL

#include "items/includes/BlockAlbedo.glsl"
#include "trees/includes/TreeBark.glsl"

// One bark fragment's G-buffer targets, for the fragment stage only: the rounded normal and crease TreeBark.glsl
// works out, and the wood's tile read once per block at the wood's own position.

// Every G-buffer target for one bark fragment; rotation turns the wood's own axes into the world's
void shadeTreeBark(
    vec3 worldPos, vec3 modelPos, vec2 local, vec2 size, int tile, vec3 color, int face, int edges,
    mat3 rotation, out vec4 albedoOut, out vec4 normalOut, out vec4 materialOut) {
    float crease;
    vec3  normal = bevelTreeNormal(face, edges, local, size, crease);
    vec4  albedo = sampleBlockAlbedo(tile, modelPos, face);

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    albedoOut   = vec4(albedo.rgb * color, 1.0);
    normalOut   = vec4(normalize(mat3(u_view) * (rotation * normal)), 1.0);
    materialOut = vec4(resolveTreeSunVisibility(worldPos), TREE_BARK_SPECULAR, crease, 1.0);
}

#endif
