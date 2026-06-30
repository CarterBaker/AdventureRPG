#ifndef SURFACE_TIER_FULL_GLSL
#define SURFACE_TIER_FULL_GLSL

/*
* Tier 0 — Detail pass. Full per-fragment material: tiled albedo, normal-map
 * (decoded + TBN'd into view space), AO, and specular. Most expensive tier —
 * only the innermost 3x3 chunk ring — drawn on top of the Flat underlay pass
 * so tessellation cracks fall back to the solid base color underneath
 * instead of showing the void through a hole.
 *
 * Returns false if the fragment should be discarded. The actual 'discard'
 * call has to happen in main(), not here — NVIDIA's compiler (error C7608)
 * rejects 'discard' inside any function that has 'out' parameters.
 */
bool shadeSurfaceFull(
    vec2 tiledUV,
    vec3 worldNormal,
    mat4 viewMat,
    out vec3 outAlbedo,
    out vec3 outNormalView,
    out float outSpecular,
    out float outAO) {
    vec4 albedo = sampleLayerTiled(tiledUV, u_layer_albedo);

    if (albedo.a < 0.01)
    return false;

    outAlbedo     = albedo.rgb;
    outNormalView = sampleNormalViewSpace(tiledUV, u_layer_normal, worldNormal, viewMat);
    outSpecular   = sampleSpecular(tiledUV, u_layer_specular);
    outAO         = sampleAO(tiledUV, u_layer_ao);
    return true;
}

#endif