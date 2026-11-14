#ifndef SURFACE_TIER_FULL_GLSL
#define SURFACE_TIER_FULL_GLSL

/*
 * Detail tier — the material maps sampled within the detail radius: the
 * normal map (rebuilt and TBN'd into view space), and the material map's
 * ambient occlusion and specular. Albedo comes from the flat tier, which
 * every fragment runs first, so a texel a covering takes over skips these
 * reads entirely. StandardSurfaceShader.fsh blends the result toward the
 * flat tier across the radius' fade band (see SurfaceTessellationTier.glsl).
 */
void sampleSurfaceDetail(
    TileRead tile,
    FaceUV face,
    vec3 worldNormal,
    mat4 viewMat,
    out vec3 outNormalView,
    out float outSpecular,
    out float outAO) {
    vec4 material = sampleTileMaterial(tile, face.uv, face.dx, face.dy);

    outNormalView = toViewSpaceNormal(sampleTileNormal(tile, face.uv, face.dx, face.dy), worldNormal, viewMat);
    outSpecular   = material.g;
    outAO         = material.r;
}

#endif
