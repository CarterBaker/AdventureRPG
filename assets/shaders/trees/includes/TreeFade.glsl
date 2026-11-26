#ifndef TREE_FADE_GLSL
#define TREE_FADE_GLSL

#include "includes/CameraData.glsl"
#include "includes/SettingsData.glsl"
#include "trees/includes/TreeDither.glsl"
#include "trees/includes/TreeFar.glsl"

// A standing tree's look hands over to its next one by screen-door dither rather than at once: while a chunk or mega
// redraws its trees, the new look rises through a 4x4 ordered pattern while the old one stands whole, and only once
// the new one is whole does the old fall away through the very same pattern, so a crown is never thinner than one
// whole look, however differently the two looks' leaf cards cover it. The fade is positive for the look rising in,
// the share of the pattern it still leaves out, and negative for the look falling away, the share it already gave
// up; zero shows the whole tree. The steps match TREE_FADE_STEPS.
//
// Far out the grid's trees hand over to the macro terrain's trees chunk by chunk, as its ground does: a macro tile
// draws a tree only while the grid does not draw the chunk it roots in, so a tree never leaves before the one taking
// its place is drawn, and never shows twice (TreeFar). Near the grid the macro tile's trees are the very look the
// grid's farthest trees take, so the hand-over shows no change.

uniform float u_treeFade;

// True where this fragment's look has not risen in yet or has already fallen away
bool isTreeFaded() {
    float threshold = resolveTreeFadeThreshold();

    return u_treeFade >= 0.0 ? threshold < u_treeFade : threshold >= 1.0 + u_treeFade;
}

#endif
