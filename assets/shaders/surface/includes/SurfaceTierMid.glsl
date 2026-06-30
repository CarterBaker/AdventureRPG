#ifndef SURFACE_TIER_MID_GLSL
#define SURFACE_TIER_MID_GLSL

/*
* Tier 1 — Mid-range pass. Albedo + normal-map only, no AO/specular sample.
 * Returns false if the fragment should be discarded — see SurfaceTierFull.glsl
 * for why the actual 'discard' call lives in main() instead of here.
 */

const float MID_TIER_SPECULAR = 0.0; // matte fallback, no specular sample at this tier
const float MID_TIER_AO       = 1.0; // fully exposed fallback, no AO sample at this tier

bool shadeSurfaceMid(
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
    outSpecular   = MID_TIER_SPECULAR;
    outAO         = MID_TIER_AO;
    return true;
}

#endif