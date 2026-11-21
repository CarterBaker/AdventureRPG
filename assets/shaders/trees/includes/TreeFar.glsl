#ifndef TREE_FAR_GLSL
#define TREE_FAR_GLSL

#include "surface/includes/MacroGround.glsl"

// A tree drawn by a macro tile rather than the chunk grid: one of the tile's models of its own trees, in the grid's
// very bark and leaf materials, marked far by u_treeFar. It stands on the macro ground, so it moves with it, crinkled
// and sunk beneath the rim alike (MacroGround), and it rises across the grid's hand-over band where the grid's own
// trees fall away (TreeFade).

uniform float u_treeFar;

bool isTreeFar() {
    return u_treeFar > 0.5;
}

// How far a far tree's point at a grid-relative position moves to stand on the macro ground; none for a grid tree
vec3 resolveTreeFarLift(vec3 position) {
    if (!isTreeFar())
    return vec3(0.0);

    return placeOnMacroGround(position, false) - position;
}

#endif
