#version 330 core

in vec3 vLocalPos;
in vec3 vColor;

#include "includes/CameraData.glsl"
#include "includes/SettingsData.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

// Distant macro terrain, written into the same G-buffer as the voxel surface so the deferred lighting and
// fog passes treat it as ordinary ground. Any fragment over a chunk the grid itself streams is discarded,
// mirroring GridBuildSystem's footprint exactly — a chunk index inside [-half, half) on both axes and within
// the render radius — so macro terrain only ever fills the view beyond the chunk grid and never overlaps it.
// The facet normal comes from screen-space derivatives of the flat position, which gives the low poly look
// without any normal data, and a heightfield never faces down. All three targets must output alpha = 1.0,
// since this pass draws with blending enabled; gMaterial packs sun visibility, specular and ao.

const float MACRO_SUN_VISIBILITY = 1.0;
const float MACRO_SPECULAR       = 0.0;
const float MACRO_AO             = 1.0;

bool isInsideChunkGrid(vec3 localPos) {
    vec2  cell       = floor(localPos.xz / u_chunkSize);
    float gridHalf   = floor(u_renderDistance * 0.5);
    float gridRadius = u_renderDistance * 0.5;

    if (any(lessThan(cell, vec2(-gridHalf))) || any(greaterThanEqual(cell, vec2(gridHalf))))
    return false;

    return dot(cell, cell) <= gridRadius * gridRadius;
}

void main() {
    if (isInsideChunkGrid(vLocalPos))
    discard;

    vec3 normal = normalize(cross(dFdx(vLocalPos), dFdy(vLocalPos)));

    if (normal.y < 0.0)
    normal = -normal;

    gAlbedo   = vec4(vColor, 1.0);
    gNormal   = vec4(normalize(mat3(u_view) * normal), 1.0);
    gMaterial = vec4(MACRO_SUN_VISIBILITY, MACRO_SPECULAR, MACRO_AO, 1.0);
}
