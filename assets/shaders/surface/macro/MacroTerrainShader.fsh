#version 330 core

in vec3 vLocalPos;
in vec2 vTilePos;
in vec3 vColor;

#include "includes/CameraData.glsl"
#include "includes/SettingsData.glsl"
#include "includes/MacroCoverageData.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

// Distant macro terrain, written into the same G-buffer as the voxel surface so the deferred lighting and
// fog passes treat it as ordinary ground. Any fragment over a chunk the grid itself draws right now is
// discarded, read from the tile's coverage bits, so macro terrain fills every chunk the grid has not drawn —
// beyond its footprint, along its rim, and wherever streaming has not caught up — and never overlaps one.
// The facet normal comes from screen-space derivatives of the flat position, which gives the low poly look
// without any normal data, and a heightfield never faces down. All three targets must output alpha = 1.0,
// since this pass draws with blending enabled; gMaterial packs sun visibility, specular and ao.

const float MACRO_SUN_VISIBILITY = 1.0;
const float MACRO_SPECULAR       = 0.0;
const float MACRO_AO             = 1.0;

bool isDrawnByChunkGrid(vec2 tilePos) {
    ivec2 chunk = clamp(ivec2(floor(tilePos / u_chunkSize)), ivec2(0), ivec2(MACRO_COVERAGE_CHUNKS_PER_SIDE - 1));
    int   index = chunk.y * MACRO_COVERAGE_CHUNKS_PER_SIDE + chunk.x;
    int   word  = index / MACRO_COVERAGE_BITS_PER_WORD;

    ivec4 vector = u_macroCoverage[word / MACRO_COVERAGE_WORDS_PER_VECTOR];

    return ((vector[word % MACRO_COVERAGE_WORDS_PER_VECTOR] >> (index % MACRO_COVERAGE_BITS_PER_WORD)) & 1) != 0;
}

void main() {
    if (isDrawnByChunkGrid(vTilePos))
    discard;

    vec3 normal = normalize(cross(dFdx(vLocalPos), dFdy(vLocalPos)));

    if (normal.y < 0.0)
    normal = -normal;

    gAlbedo   = vec4(vColor, 1.0);
    gNormal   = vec4(normalize(mat3(u_view) * normal), 1.0);
    gMaterial = vec4(MACRO_SUN_VISIBILITY, MACRO_SPECULAR, MACRO_AO, 1.0);
}
