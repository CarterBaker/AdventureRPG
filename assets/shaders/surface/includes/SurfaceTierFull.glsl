#ifndef SURFACE_TIER_FULL_GLSL
#define SURFACE_TIER_FULL_GLSL

/*
 * Detail tier — the material maps sampled within the detail radius: the
 * normal map (decoded and TBN'd into view space), specular and AO. Albedo
 * comes from the flat tier, which every fragment runs first, so a texel a
 * covering takes over skips these reads entirely. StandardSurfaceShader.fsh
 * blends the result toward the flat tier across the radius' fade band (see
 * SurfaceTessellationTier.glsl).
 */
void sampleSurfaceDetail(
    vec2 tiledUV,
    vec3 worldNormal,
    mat4 viewMat,
    out vec3 outNormalView,
    out float outSpecular,
    out float outAO) {
    outNormalView = sampleNormalViewSpace(tiledUV, u_layer_normal, worldNormal, viewMat);
    outSpecular   = sampleSpecular(tiledUV, u_layer_specular);
    outAO         = sampleAO(tiledUV, u_layer_ao);
}

#endif
