#ifndef MACRO_WATER_DATA_GLSL
#define MACRO_WATER_DATA_GLSL

#include "includes/SettingsData.glsl"

// Must match EngineSetting.MACRO_WATER_MASK_SPAN_CHUNKS and MACRO_WATER_COVERAGE_SIZE — GLSL has no visibility
// into the Java constants.
#define MACRO_WATER_MASK_SPAN_CHUNKS 1072.0
#define MACRO_WATER_COVERAGE_SIZE 112

// Source: MacroWaterRenderSystem, one set per grid.
// macroWaterMask:     the open water ring, one patch per macro tile, repeating every MACRO_WATER_MASK_SPAN_CHUNKS;
//                     red is set where the sea stands over the terrain.
// macroWaterCoverage: one texel per chunk around the active chunk, which sits at the texture's centre; red is set
//                     where the chunk grid draws that chunk itself.
// macroWaterAnchor:   the active chunk's place in the mask ring, in chunks.
uniform sampler2D u_macroWaterMask;
uniform sampler2D u_macroWaterCoverage;
uniform vec2      u_macroWaterAnchor;

// Grid positions are relative to the active chunk's origin, the frame every chunk and macro renders in.
bool isMacroWaterOpen(vec2 gridPos) {
    vec2 ringChunk = u_macroWaterAnchor + gridPos / u_chunkSize;
    return texture(u_macroWaterMask, ringChunk / MACRO_WATER_MASK_SPAN_CHUNKS).r > 0.5;
}

bool isDrawnByChunkGridAround(vec2 gridPos) {
    ivec2 texel = ivec2(floor(gridPos / u_chunkSize)) + ivec2(MACRO_WATER_COVERAGE_SIZE / 2);

    if (any(lessThan(texel, ivec2(0))) || any(greaterThanEqual(texel, ivec2(MACRO_WATER_COVERAGE_SIZE))))
    return false;

    return texelFetch(u_macroWaterCoverage, texel, 0).r > 0.5;
}

#endif
