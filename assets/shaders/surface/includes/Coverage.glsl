#ifndef COVERAGE_GLSL
#define COVERAGE_GLSL

#include "includes/CoveringData.glsl"

// Grows a covering over the face it was laid on. The face's coverage word names the covering, whether the face shows
// its top or its side tile, and the level it has reached. The covering's growth map holds, per texel, the share of the
// full level at which that texel is overgrown, so as the level rises the covering fills in blade by blade, or clump by
// clump, exactly where its growth map says. A top tile always takes the spin a natural top face takes from its block,
// whatever the host's own rotation, so a covering never repeats block to block; a side tile follows the host's face, so
// its fringe always hangs from the upper edge. Texels the covering leaves transparent keep the block beneath. The
// biome's tint, carried in the vertex color, colors the covering as far as its tint strength allows, and only its
// unsaturated texels: grey blades take the biome's color while a flower's own hue survives. Near tiers swap in the
// covering's normal, specular and ambient occlusion as well; the far tier swaps its albedo alone.

const float COVERAGE_TOP_ENCODED_FACE = 28.0; // EngineSetting.ENCODED_FACE_NATURAL_FULL_OFFSET + UP
const float COVERAGE_ALPHA_CUTOFF     = 0.5;
const float COVERAGE_TINT_CHROMA_LOW  = 0.08;
const float COVERAGE_TINT_CHROMA_HIGH = 0.3;

vec3 tintCovering(vec3 coverAlbedo, int coveringID, vec3 biomeTint) {
    float chroma     = max(coverAlbedo.r, max(coverAlbedo.g, coverAlbedo.b))
                     - min(coverAlbedo.r, min(coverAlbedo.g, coverAlbedo.b));
    float tintWeight = u_coveringStyle[coveringID].x
                     * (1.0 - smoothstep(COVERAGE_TINT_CHROMA_LOW, COVERAGE_TINT_CHROMA_HIGH, chroma));
    return coverAlbedo * mix(vec3(1.0), biomeTint, tintWeight);
}

void applyCoverage(
    vec3 uvLocalPos,
    vec2 tiledUV,
    vec2 uvOrigin,
    float coverageWord,
    vec3 biomeTint,
    vec3 worldNormal,
    mat4 viewMat,
    bool detailed,
    inout vec3 albedo,
    inout vec3 normalView,
    inout float specular,
    inout float ao) {

    int word       = int(coverageWord + 0.5);
    int coveringID = word >> COVERAGE_VERTEX_ID_SHIFT;

    if (coveringID == 0)
    return;

    int  level = word & COVERAGE_LEVEL_MAX;
    bool side  = (word & COVERAGE_VERTEX_SIDE_BIT) != 0;

    vec4 tiles   = u_coveringTiles[coveringID];
    vec2 coverUV = side
        ? tiles.zw + (tiledUV - uvOrigin)
        : tileUV(uvLocalPos, tiles.xy, worldNormal, COVERAGE_TOP_ENCODED_FACE);

    float growth = texture(u_textureArray, vec3(coverUV, float(u_layer_growth))).r;

    if (growth > float(level) / float(COVERAGE_LEVEL_MAX))
    return;

    vec4 coverAlbedo = sampleLayerTiled(coverUV, u_layer_albedo);

    if (coverAlbedo.a < COVERAGE_ALPHA_CUTOFF)
    return;

    albedo = tintCovering(coverAlbedo.rgb, coveringID, biomeTint);

    if (!detailed)
    return;

    normalView = sampleNormalViewSpace(coverUV, u_layer_normal, worldNormal, viewMat);
    specular   = sampleSpecular(coverUV, u_layer_specular);
    ao         = sampleAO(coverUV, u_layer_ao);
}

#endif
