#ifndef SURFACE_TIER_FLAT_GLSL
#define SURFACE_TIER_FLAT_GLSL

/*
 * Flat tier — albedo only, raw per-face vertex normal (no normal-map
 * sample), no AO/specular sample. Every fragment runs it first: it is the
 * whole material beyond the detail radius, and the albedo and fallback the
 * detail tier blends from within it.
 *
 * Returns false if the fragment should be discarded. The actual 'discard'
 * call has to happen in main(), not here — NVIDIA's compiler (error C7608)
 * rejects 'discard' inside any function that has 'out' parameters.
 */

const float FLAT_TIER_SPECULAR = 0.0;
const float FLAT_TIER_AO       = 1.0;

bool shadeSurfaceFlat(
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
    outNormalView = normalize(mat3(viewMat) * worldNormal);
    outSpecular   = FLAT_TIER_SPECULAR;
    outAO         = FLAT_TIER_AO;
    return true;
}

#endif