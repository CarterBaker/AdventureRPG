#ifndef SURFACE_TIER_MID_GLSL
#define SURFACE_TIER_MID_GLSL

/*
* Tier 1 — Mid-range pass. Every material map is sampled here, same as Tier 0 —
 * only geometry (height displacement, near-terrain jitter, adaptive
 * tessellation boost) is skipped at this range. That split lives in
 * StandardSurface.tes/.tcs, gated on the same TIER0_MAX_SQ_DIST ring used
 * for tier selection in StandardSurface.fsh — not here.
 *
 * Previously this tier faked AO/specular as constants instead of sampling,
 * which fed fabricated "fully exposed, non-reflective" material data into
 * the deferred Lighting.fsh pass and made mid-range chunks look flatter and
 * brighter than they should. Lighting.fsh has no concept of tiers — it just
 * trusts whatever's in gMaterial, so any tier-dependent lighting difference
 * has to be caught here, not there.
 *
 * Returns false if the fragment should be discarded — see SurfaceTierFull.glsl
 * for why the actual 'discard' call lives in main() instead of here.
 */

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
    outSpecular   = sampleSpecular(tiledUV, u_layer_specular);
    outAO         = sampleAO(tiledUV, u_layer_ao);
    return true;
}

#endif