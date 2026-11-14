#ifndef COVERAGE_GLSL
#define COVERAGE_GLSL

#include "includes/CoveringData.glsl"

// Grows a covering over the face it was laid on. The face's coverage word names the covering, whether the face shows
// its top or its side tile, and the level it has reached. Within the detail radius the covering is drawn in full: its
// growth map, in the extra set, holds, per texel, the share of the full level at which that texel is overgrown, so as
// the level rises the covering fills in blade by blade, or clump by clump, exactly where its growth map says. A top
// tile always takes the spin a natural top face takes from its block, whatever the host's own rotation, so a covering
// never repeats block to block; a side tile follows the host's face, so its fringe always hangs from the upper edge.
// Texels the covering leaves transparent keep the block beneath, and the covering swaps in its own normal, specular and
// ambient occlusion where it shows. The biome's tint, carried in the vertex color, colors the covering as far as its
// tint strength allows, and only its unsaturated texels: grey blades take the biome's color while a flower's own hue
// survives. Beyond the radius the covering is approximated without a single texture read: CoveringBufferSystem bakes,
// per covering face, the share of its tile shown at every level and the tile's average color split into the part that
// keeps its own hue and the part that takes the tint, so the face blends the block's albedo toward the covering's
// tinted average by exactly the share a full draw would show. The alpha cutoff and chroma band must match
// EngineSetting.COVERAGE_ALPHA_CUTOFF, COVERAGE_TINT_CHROMA_LOW and COVERAGE_TINT_CHROMA_HIGH, which the bake uses.

const int   COVERAGE_TOP_FACE_ORDINAL = 4;    // Direction3Vector.UP, the ordinal a natural top face spins by
const float COVERAGE_ALPHA_CUTOFF     = 0.5;
const float COVERAGE_TINT_CHROMA_LOW  = 0.08;
const float COVERAGE_TINT_CHROMA_HIGH = 0.3;
const int   COVERAGE_SHARE_BITS       = 8;
const int   COVERAGE_SHARE_MASK       = 255;
const int   COVERAGE_SHARES_PER_WORD  = 4;

int toCoveringID(int word) {
    return word >> COVERAGE_VERTEX_ID_SHIFT;
}

bool isSideCoverage(int word) {
    return (word & COVERAGE_VERTEX_SIDE_BIT) != 0;
}

vec3 tintCovering(vec3 coverAlbedo, int coveringID, vec3 biomeTint) {
    float chroma     = max(coverAlbedo.r, max(coverAlbedo.g, coverAlbedo.b))
                     - min(coverAlbedo.r, min(coverAlbedo.g, coverAlbedo.b));
    float tintWeight = u_coveringStyle[coveringID].x
                     * (1.0 - smoothstep(COVERAGE_TINT_CHROMA_LOW, COVERAGE_TINT_CHROMA_HIGH, chroma));
    return coverAlbedo * mix(vec3(1.0), biomeTint, tintWeight);
}

// Full draw — true where the covering shows over this texel, with its own material written out
bool sampleCoverageDetail(
    FaceUV face,
    FaceUV topFace,
    float coverageWord,
    vec3 biomeTint,
    vec3 worldNormal,
    mat4 viewMat,
    out vec3 albedo,
    out vec3 normalView,
    out float specular,
    out float ao) {

    int word       = int(coverageWord + 0.5);
    int coveringID = toCoveringID(word);

    albedo     = vec3(0.0);
    normalView = vec3(0.0);
    specular   = 0.0;
    ao         = 1.0;

    if (coveringID == 0)
    return false;

    int  level = word & COVERAGE_LEVEL_MAX;
    vec4 tiles = u_coveringTiles[coveringID];

    bool     side  = isSideCoverage(word);
    FaceUV   cover = side ? face : topFace;
    TileRead tile  = resolveTileReadGrad(int((side ? tiles.z : tiles.x) + 0.5), cover.dx, cover.dy);

    float growth = sampleTileExtra(tile, cover.uv, cover.dx, cover.dy).r;

    if (growth > float(level) / float(COVERAGE_LEVEL_MAX))
    return false;

    vec4 coverAlbedo = sampleTileAlbedo(tile, cover.uv, cover.dx, cover.dy);

    if (coverAlbedo.a < COVERAGE_ALPHA_CUTOFF)
    return false;

    vec4 material = sampleTileMaterial(tile, cover.uv, cover.dx, cover.dy);

    albedo     = tintCovering(coverAlbedo.rgb, coveringID, biomeTint);
    normalView = toViewSpaceNormal(sampleTileNormal(tile, cover.uv, cover.dx, cover.dy), worldNormal, viewMat);
    specular   = material.g;
    ao         = material.r;
    return true;
}

// Baked approximation — the block's albedo blended toward the covering's tinted average by the share it shows
vec3 approximateCoverage(vec3 baseAlbedo, float coverageWord, vec3 biomeTint) {

    int word       = int(coverageWord + 0.5);
    int coveringID = toCoveringID(word);

    if (coveringID == 0)
    return baseAlbedo;

    int   level     = word & COVERAGE_LEVEL_MAX;
    int   face      = coveringID * 2 + (isSideCoverage(word) ? 1 : 0);
    int   shareWord = u_coveringRevealShares[face][level / COVERAGE_SHARES_PER_WORD];
    float share     = float((shareWord >> ((level % COVERAGE_SHARES_PER_WORD) * COVERAGE_SHARE_BITS))
                    & COVERAGE_SHARE_MASK) / float(COVERAGE_SHARE_MASK);

    vec3 cover = u_coveringRevealColor[face].rgb
               + u_coveringStyle[coveringID].x * u_coveringRevealTintable[face].rgb * (biomeTint - 1.0);

    return mix(baseAlbedo, cover, share);
}

#endif
