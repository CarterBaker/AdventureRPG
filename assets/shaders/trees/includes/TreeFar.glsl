#ifndef TREE_FAR_GLSL
#define TREE_FAR_GLSL

#include "includes/MacroCoverageUtility.glsl"
#include "surface/includes/MacroGround.glsl"

// A tree drawn by a macro tile rather than the chunk grid: one of the tile's models of its own trees, in the grid's
// very bark and leaf materials, marked far by u_treeFar. Every vertex names the chunk of the tile its tree roots in,
// and the tree yields whole once the grid has that chunk's own trees on the GPU, not merely its ground, so the grid's
// own tree and this one never show together and never leave a gap (TreeFade). It stands on the macro ground, so it
// sinks with it at the rim (MacroGround).

uniform float u_treeFar;

bool isTreeFar() {
    return u_treeFar > 0.5;
}

// How far a far tree's point at a grid-relative position moves to stand on the macro ground; none for a grid tree
vec3 resolveTreeFarLift(vec3 position) {
    if (!isTreeFar())
    return vec3(0.0);

    return placeOnMacroGround(position) - position;
}

// True where the grid has a far tree's chunk's own trees on the GPU, which stand there instead; never for a grid tree
bool isTreeYielded(int cover) {
    return isTreeFar() && isTreesDrawnByGrid(cover);
}

#endif
