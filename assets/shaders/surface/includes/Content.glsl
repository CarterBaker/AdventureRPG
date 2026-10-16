#ifndef CONTENT_GLSL
#define CONTENT_GLSL

#include "includes/ContentData.glsl"
#include "surface/includes/Coverage.glsl"

// Lays the content a block holds — an ore, a rock, a root — over every face of the block. The quad's content ID names
// an overlay tile in the same texture array as the block, sampled at exactly the offset into its tile the block's own
// texture is, so the overlay spins and turns with the face beneath it. Within the detail radius every texel whose
// alpha clears the cutoff shows the overlay, with its own normal, specular and ambient occlusion swapped in, and every
// other texel keeps the block; a covering grown over the face is drawn over both. Beyond the radius the overlay is
// approximated without a single texture read: ContentBufferSystem bakes the average color the overlay shows and the
// share of its tile it shows, and the face blends the block's albedo toward that color by that share. The alpha cutoff
// is the one coverings use, which the bake shares.

// Full draw — true where the content shows over this texel, with its own material written out
bool sampleContentDetail(
    vec2 tiledUV,
    vec2 uvOrigin,
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

    vec2 contentUV     = u_contentTiles[id].xy + (tiledUV - uvOrigin);
    vec4 contentAlbedo = sampleLayerTiled(contentUV, u_layer_albedo);

    if (contentAlbedo.a < COVERAGE_ALPHA_CUTOFF)
    return false;

    albedo     = contentAlbedo.rgb;
    normalView = sampleNormalViewSpace(contentUV, u_layer_normal, worldNormal, viewMat);
    specular   = sampleSpecular(contentUV, u_layer_specular);
    ao         = sampleAO(contentUV, u_layer_ao);
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
