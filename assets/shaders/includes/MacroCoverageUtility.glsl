#ifndef MACRO_COVERAGE_UTILITY_GLSL
#define MACRO_COVERAGE_UTILITY_GLSL

#include "includes/SettingsData.glsl"
#include "includes/MacroCoverageData.glsl"

// True where the chunk grid draws the chunk of this macro tile that a tile-local position lies over, read from
// the tile's coverage bits, so distant terrain fills every chunk the grid has not drawn and never overlaps one.

bool isDrawnByChunkGrid(vec2 tilePos) {
    ivec2 chunk = clamp(ivec2(floor(tilePos / u_chunkSize)), ivec2(0), ivec2(MACRO_COVERAGE_CHUNKS_PER_SIDE - 1));
    int   index = chunk.y * MACRO_COVERAGE_CHUNKS_PER_SIDE + chunk.x;
    int   word  = index / MACRO_COVERAGE_BITS_PER_WORD;

    ivec4 vector = u_macroCoverage[word / MACRO_COVERAGE_WORDS_PER_VECTOR];

    return ((vector[word % MACRO_COVERAGE_WORDS_PER_VECTOR] >> (index % MACRO_COVERAGE_BITS_PER_WORD)) & 1) != 0;
}

#endif
