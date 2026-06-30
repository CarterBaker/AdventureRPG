#ifndef SURFACE_TIER_FLAT_GLSL
#define SURFACE_TIER_FLAT_GLSL

/*
* Flat pass — albedo only, raw per-face vertex normal (no normal-map
 * sample), no AO/specular sample. Reused twice: as the hole-filling
 * underlay beneath the Detail pass on the innermost ring, and as the
 * cheapest tier beyond the Mid ring.
 *
 * Returns false if the fragment should be discarded — see SurfaceTierFull.glsl
 * for why the actual 'discard' call lives in main() instead of here.
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