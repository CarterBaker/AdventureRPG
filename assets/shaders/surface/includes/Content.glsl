#ifndef CONTENT_GLSL
#define CONTENT_GLSL

#include "includes/ContentData.glsl"
#include "surface/includes/Coverage.glsl"

// Lays the content a block holds — an ore, a rock, a root — over every face of the block, as much of it as the block
// holds. The quad's content word names the content above the amount's bits; the amount shows as a covering's level
// does, at the coverage level its share of the content's most reaches, so the overlay's growth map, in the extra set,
// fills in nugget by nugget exactly as a covering's fills in blade by blade. The overlay is sampled at exactly the UV
// and gradients the block's own texture is, so it spins and turns with the face beneath it. Within the detail radius
// every grown texel whose alpha clears the cutoff shows the overlay, with its own normal, specular and ambient
// occlusion swapped in, and every other texel keeps the block; a covering grown over the face is drawn over both.
// Beyond the radius the overlay is approximated without a single texture read: ContentBufferSystem bakes the average
// color the overlay shows and the share of its tile it shows at every level, and the face blends the block's albedo
// toward that color by the share its level shows. The growth test, alpha cutoff and shares are the ones coverings
// use, which the bake shares.

int toContentID(int word) {
    return word >> CONTENT_AMOUNT_BITS;
}

// The coverage level a content word's amount shows at — ContentUtility.toRevealLevel
int toContentLevel(int word, int contentID) {
    int maxAmount = int(u_contentTiles[contentID].y + 0.5);
    return ((word & CONTENT_AMOUNT_MASK) * COVERAGE_LEVEL_MAX + maxAmount / 2) / maxAmount;
}

// Full draw — true where the content shows over this texel, with its own material written out
bool sampleContentDetail(
    FaceUV face,
    float contentWord,
    vec3 worldNormal,
    mat4 viewMat,
    out vec3 albedo,
    out vec3 normalView,
    out float specular,
    out float ao) {

    int word = int(contentWord + 0.5);
    int id   = toContentID(word);

    albedo     = vec3(0.0);
    normalView = vec3(0.0);
    specular   = 0.0;
    ao         = 1.0;

    if (id == 0)
    return false;

    TileRead tile   = resolveTileReadGrad(int(u_contentTiles[id].x + 0.5), face.dx, face.dy);
    float    growth = sampleTileExtra(tile, face.uv, face.dx, face.dy).r;

    if (!isGrownAt(growth, toContentLevel(word, id)))
    return false;

    vec4 contentAlbedo = sampleTileAlbedo(tile, face.uv, face.dx, face.dy);

    if (contentAlbedo.a < COVERAGE_ALPHA_CUTOFF)
    return false;

    vec4 material = sampleTileMaterial(tile, face.uv, face.dx, face.dy);

    albedo     = contentAlbedo.rgb;
    normalView = toViewSpaceNormal(sampleTileNormal(tile, face.uv, face.dx, face.dy), worldNormal, viewMat);
    specular   = material.g;
    ao         = material.r;
    return true;
}

// Baked approximation — the block's albedo blended toward the content's average by the share its amount shows
vec3 approximateContent(vec3 baseAlbedo, float contentWord) {

    int word = int(contentWord + 0.5);
    int id   = toContentID(word);

    if (id == 0)
    return baseAlbedo;

    float share = unpackRevealShare(u_contentRevealShares[id], toContentLevel(word, id));

    return mix(baseAlbedo, u_contentReveal[id].rgb, share);
}

#endif
