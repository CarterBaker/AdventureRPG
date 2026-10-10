#ifndef COVERING_DATA_GLSL
#define COVERING_DATA_GLSL

// Must match EngineSetting.COVERING_ID_COUNT, COVERAGE_LEVEL_MAX, COVERAGE_VERTEX_SIDE_BIT and
// COVERAGE_VERTEX_ID_SHIFT, and the counts in ubos/CoveringData — GLSL has no visibility into the Java constants.
#define COVERING_ID_COUNT 64
#define COVERAGE_LEVEL_MAX 15
#define COVERAGE_VERTEX_SIDE_BIT 16
#define COVERAGE_VERTEX_ID_SHIFT 5

// Source: CoveringBufferSystem, indexed by covering ID.
//
// coveringTiles: xy = the top tile's atlas origin, zw = the side tile's, negative where the covering has no sides.
// coveringStyle: x  = how far the biome's tint colors the covering, 0 not at all to 1 fully.
layout(std140) uniform CoveringData {
    vec4 u_coveringTiles[COVERING_ID_COUNT];
    vec4 u_coveringStyle[COVERING_ID_COUNT];
};

#endif
