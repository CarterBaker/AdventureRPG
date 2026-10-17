#ifndef TREE_LEAF_GLSL
#define TREE_LEAF_GLSL

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/WindData.glsl"
#include "trees/includes/TreeShading.glsl"

// A leaf cluster is a sheaf of flat leaf cards through its centre: upright cards turned evenly about it and one level
// card across it, each cut out of a transparent sprig in the fragment stage (see TreeLeafSprig.glsl). Each card
// draws its own sprigs from the cluster's seed and its index, an upright card reaching as wide and as tall as the
// cluster with its downward fronds falling short, so the sheaf stands as a half sphere, and the level one as deep as
// it is wide. The whole cluster sways with the wind as one piece, so a sheaf
// never tears. The vertex layout must match the one TreeMeshUtility writes: the meta slot carries the card's index,
// the low edge slot the way from the vertex to the centre and the horizontal radius, and the high edge slot the
// vertical radius, the seed and the vertex's corner on its card.

const int   TREE_LEAF_LEVEL_CARD      = 3;
const float TREE_LEAF_CARD_SEED_STEP  = 0.37;
const float TREE_LEAF_UNDERSIDE_REACH = 0.6;
const float TREE_LEAF_SWAY_BLOCKS     = 0.05;
const float TREE_LEAF_SWAY_SPEED      = 1.7;
const float TREE_LEAF_SWAY_WIND       = 0.15;
const float TREE_LEAF_SWAY_CALM       = 0.2;
const float TREE_LEAF_SWAY_SPREAD     = 0.3;
const float TREE_LEAF_TWO_PI          = 6.28318530718;

// How far the wind carries a cluster this moment, the same for every vertex of it
vec3 resolveTreeLeafSway(vec3 center, float seed) {
    float strength = clamp(u_windSpeed * TREE_LEAF_SWAY_WIND, TREE_LEAF_SWAY_CALM, 1.0);
    float phase    = u_time * TREE_LEAF_SWAY_SPEED + seed * TREE_LEAF_TWO_PI
                   + (center.x + center.z) * TREE_LEAF_SWAY_SPREAD;
    vec3  heading  = vec3(u_windDirection.x, 0.0, u_windDirection.z);

    if (dot(heading, heading) < 0.0001)
    heading = vec3(1.0, 0.0, 0.0);

    return normalize(heading) * sin(phase) * TREE_LEAF_SWAY_BLOCKS * strength;
}

// A card's half-extents in blocks — the level card is as deep as it is wide, an upright one as tall as the cluster
vec2 resolveTreeLeafCardRadii(int card, vec2 radii) {
    return card == TREE_LEAF_LEVEL_CARD ? vec2(radii.x) : radii;
}

// The share of its rim an upright card's downward fronds reach — a level card's reach it all round
float resolveTreeLeafCardUnderside(int card) {
    return card == TREE_LEAF_LEVEL_CARD ? 1.0 : TREE_LEAF_UNDERSIDE_REACH;
}

// Every card of a cluster draws its own sprigs, so crossing cards never mirror each other
float resolveTreeLeafCardSeed(float seed, int card) {
    return fract(seed + float(card) * TREE_LEAF_CARD_SEED_STEP);
}

#endif
