#ifndef TREE_IMPOSTOR_BAKE_GLSL
#define TREE_IMPOSTOR_BAKE_GLSL

#include "includes/CameraData.glsl"

// The map a bake view writes: 0 the albedo darkened by its occlusion, whole where the tree is; 1 the normal in the
// tree's own frame packed into the unit cube. The shading writes the normal in the window's view space, so it is
// turned back into the tree's frame by that view's transpose.
uniform int u_bakeMap;

vec4 resolveTreeImpostorBake(vec4 albedo, vec4 normal, vec4 material) {
    if (u_bakeMap == 0)
    return vec4(albedo.rgb * material.z, 1.0);

    vec3 treeNormal = normalize(transpose(mat3(u_view)) * normal.xyz);

    return vec4(treeNormal * 0.5 + 0.5, 1.0);
}

#endif
