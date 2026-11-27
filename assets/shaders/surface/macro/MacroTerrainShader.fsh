#version 330 core

in vec3 vLocalPos;
in vec2 vTilePos;
in vec3 vTopColor;
in vec3 vSideColor;
flat in int vCoverChunk;

#include "includes/CameraData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MacroCoverageUtility.glsl"
#include "surface/includes/CloudShadow.glsl"
#include "surface/includes/MacroPaint.glsl"
#include "trees/includes/TreeFade.glsl"
#include "includes/GBufferSurface.glsl"
#include "includes/GBufferNormal.glsl"

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;
layout(location = 3) out vec4 gSurface;

// Distant macro terrain, written into the same G-buffer as the voxel surface so the deferred lighting and fog passes
// treat it as ordinary ground. Any fragment over a chunk the grid itself draws right now is discarded, so macro terrain
// fills every chunk the grid has not drawn — beyond its footprint, along its rim, and wherever streaming has not caught
// up — and never overlaps one. A tree stand-in yields whole once the grid has the trees of the chunk its tree roots in
// on the GPU, so a tree is never drawn twice and never missing. A tile's canopy and stand-ins are a model of their own,
// swapped whole with its land once the new ones are uploaded, never dithered (u_treeFade stays whole): two broad
// surfaces interleaved pixel by pixel would be inked as edges everywhere by the lighting pass. The facet normal comes
// from screen-space derivatives of the flat position, which gives the low poly look without any normal data. A
// heightfield never faces down, while a tree stand-in is a closed faceted shape, so its facet faces the camera and a
// crown's underside turns down into its slope color. A gentle facet shows its top color and a steep one turns to its
// slope color, the way a stepped voxel hillside shows more block sides the steeper it climbs, painted as a cartoon
// backdrop's mountains are (MacroPaint). The surface is marked painted, so the lighting pass lights it in a backdrop's
// flat tones and hazes it with distance, and its normal is stored smooth, so the outline pass inks its silhouettes but
// never the folds between its facets. Sun visibility under the cloud layers is the same the voxel surface writes, so
// cloud shadows run on across the seam. All three targets must output alpha = 1.0, since this pass draws with blending
// enabled; gMaterial packs sun visibility, specular and ao.

const float MACRO_SPECULAR           = 0.0;
const float MACRO_AO                 = 1.0;
const float SUN_SHADOW_MIN_ELEVATION = 0.05;

float resolveSunVisibility() {
    if (u_sunIntensity <= 0.0)
    return 1.0;

    vec2 sunHorizonOffset = u_sunDirection.xz / max(u_sunDirection.y, SUN_SHADOW_MIN_ELEVATION);

    return 1.0 - sampleCloudShadow(vLocalPos, sunHorizonOffset);
}

void main() {
    bool standIn = vCoverChunk >= 0;

    if (isTreeFaded())
    discard;

    if (standIn ? isTreesDrawnByGrid(vCoverChunk) : isDrawnByChunkGrid(vTilePos))
    discard;

    vec3 normal = normalize(cross(dFdx(vLocalPos), dFdy(vLocalPos)));
    bool facesAway = standIn
        ? dot(normal, u_inverseView[3].xyz - vLocalPos) < 0.0
        : normal.y < 0.0;

    if (facesAway)
    normal = -normal;

    vec3 albedo = paintMacroGround(vTopColor, vSideColor, normal, anchorMacroPaint(vLocalPos), !standIn);

    gAlbedo   = vec4(albedo, 1.0);
    gNormal   = vec4(encodeSmoothNormal(normalize(mat3(u_view) * normal)), 1.0);
    gMaterial = vec4(resolveSunVisibility(), MACRO_SPECULAR, MACRO_AO, 1.0);
    gSurface  = GBUFFER_SURFACE_PAINTED;
}
