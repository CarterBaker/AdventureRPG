#ifndef MACRO_COVERAGE_DATA_GLSL
#define MACRO_COVERAGE_DATA_GLSL

// Must match EngineSetting.MACRO_CHUNK_SIZE, MACRO_COVERAGE_BITS_PER_WORD,
// VECTOR4_COMPONENT_COUNT and MACRO_COVERAGE_VECTOR_COUNT, and the count in
// ubos/MacroCoverageData.arpg — GLSL has no visibility into the Java constants.
#define MACRO_COVERAGE_CHUNKS_PER_SIDE 16
#define MACRO_COVERAGE_BITS_PER_WORD 32
#define MACRO_COVERAGE_WORDS_PER_VECTOR 4
#define MACRO_COVERAGE_VECTOR_COUNT 2

// Source: MacroRenderSystem, one instance per macro chunk.
// macroCoverage:  one bit per chunk of the tile, set where the chunk grid
//                 draws that chunk itself. Chunk (x, z) is bit index
//                 z * MACRO_COVERAGE_CHUNKS_PER_SIDE + x, counted through
//                 the components in order, x through w, then the next vector.
layout(std140) uniform MacroCoverageData {
    ivec4 u_macroCoverage[MACRO_COVERAGE_VECTOR_COUNT];
};

#endif
