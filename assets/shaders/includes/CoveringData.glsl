#ifndef COVERING_DATA_GLSL
#define COVERING_DATA_GLSL

// Must match EngineSetting.COVERING_ID_COUNT, COVERING_FACE_COUNT, COVERAGE_LEVEL_MAX, COVERAGE_VERTEX_SIDE_BIT and
// COVERAGE_VERTEX_ID_SHIFT, and the counts in ubos/CoveringData — GLSL has no visibility into the Java constants.
#define COVERING_ID_COUNT 64
#define COVERING_FACE_COUNT 128
#define COVERAGE_LEVEL_MAX 15
#define COVERAGE_VERTEX_SIDE_BIT 16
#define COVERAGE_VERTEX_ID_SHIFT 5

// Source: CoveringBufferSystem, indexed by covering ID, and the reveal rows by covering face, the covering ID times
// two for its top tile and one more for its side tile.
//
// coveringTiles:          xy = the top tile's atlas origin, zw = the side tile's, negative where the covering has no
//                         sides.
// coveringStyle:          x  = how far the biome's tint colors the covering, 0 not at all to 1 fully.
// coveringRevealColor:    rgb = the average albedo of the face's tile where it shows at its full level.
// coveringRevealTintable: rgb = the same average weighted by how far each texel takes the biome's tint.
// coveringRevealShares:   the share of the face's tile shown at each level, one byte per level from level zero,
//                         four levels per component in order, x through w, low byte first.
layout(std140) uniform CoveringData {
    vec4  u_coveringTiles[COVERING_ID_COUNT];
    vec4  u_coveringStyle[COVERING_ID_COUNT];
    vec4  u_coveringRevealColor[COVERING_FACE_COUNT];
    vec4  u_coveringRevealTintable[COVERING_FACE_COUNT];
    ivec4 u_coveringRevealShares[COVERING_FACE_COUNT];
};

#endif
