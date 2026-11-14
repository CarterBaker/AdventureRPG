#ifndef CONTENT_GLSL
#define CONTENT_GLSL

#include "includes/ContentData.glsl"
#include "surface/includes/Coverage.glsl"

// Lays the content a block holds — an ore, a rock, a root — over every face of the block. The quad's content ID names
// an overlay tile, sampled at exactly the UV and gradients the block's own texture is, so the overlay spins and turns
// with the face beneath it. Within the detail radius every texel whose
// alpha clears the cutoff shows the overlay, with its own normal, specular and ambient occlusion swapped in, and every
// other texel keeps the block; a covering grown over the face is drawn over both. Beyond the radius the overlay is
// approximated without a single texture read: ContentBufferSystem bakes the average color the overlay shows and the
// share of its tile it shows, and the face blends the block's albedo toward that color by that share. The alpha cutoff
// is the one coverings use, which the bake shares.

// Full draw — true where the content shows over this texel, with its own material written out
bool sampleContentDetail(
    FaceUV face,
    float contentID,
    vec3 worldNormal,
    mat4 viewMat,
    out vec3 albedo,
    out vec3 normalView,
    out float specular,
    out float ao) {

    int id = int(contentID + 0.5);

    albedo     = vec3(0.0);
    normalView = vec3(0.0);
    specular   = 0.0;
    ao         = 1.0;

    if (id == 0)
    return false;

    TileRead tile          = resolveTileReadGrad(int(u_contentTiles[id].x + 0.5), face.dx, face.dy);
    vec4     contentAlbedo = sampleTileAlbedo(tile, face.uv, face.dx, face.dy);

    if (contentAlbedo.a < COVERAGE_ALPHA_CUTOFF)
    return false;

    vec4 material = sampleTileMaterial(tile, face.uv, face.dx, face.dy);

    albedo     = contentAlbedo.rgb;
    normalView = toViewSpaceNormal(sampleTileNormal(tile, face.uv, face.dx, face.dy), worldNormal, viewMat);
    specular   = material.g;
    ao         = material.r;
    return true;
}

// Baked approximation — the block's albedo blended toward the content's average by the share it shows
vec3 approximateContent(vec3 baseAlbedo, float contentID) {

    int id = int(contentID + 0.5);

    if (id == 0)
    return baseAlbedo;

    vec4 reveal = u_contentReveal[id];

    return mix(baseAlbedo, reveal.rgb, reveal.a);
}

#endif
