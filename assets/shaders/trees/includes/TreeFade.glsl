#ifndef TREE_FADE_GLSL
#define TREE_FADE_GLSL

#include "includes/CameraData.glsl"
#include "includes/SettingsData.glsl"

// A standing tree's look hands over to its next one by screen-door dither rather than at once: while a chunk or mega
// redraws its trees, the new look rises through a 4x4 ordered pattern as the old one falls away through the very same
// pattern, so every pixel shows exactly one of them and a tree that did not change looks the same throughout. The
// fade is positive for the look rising in, the share of the pattern it still leaves out, and negative for the look
// falling away, the share it already gave up; zero shows the whole tree. The steps match TREE_FADE_STEPS.
//
// Far out the grid's trees hand over to the macro terrain's stand-ins by distance alone, whichever chunks have
// loaded: across a band just inside the rim the grid draws, the stand-ins rise through the same pattern as the
// grid's trees fall away through it, measured from the camera to each fragment, so every pixel shows exactly one of
// them and the forest never changes its look at a chunk's edge. With macro terrain off nothing stands in, so the
// grid's trees keep the whole way out.

uniform float u_treeFade;

const float TREE_FADE_PATTERN[16] = float[16](
    0.0, 8.0, 2.0, 10.0,
    12.0, 4.0, 14.0, 6.0,
    3.0, 11.0, 1.0, 9.0,
    15.0, 7.0, 13.0, 5.0);
const float TREE_FADE_STEPS = 16.0;

const float TREE_HANDOVER_INSET_CHUNKS = 1.5;
const float TREE_HANDOVER_BAND_CHUNKS  = 2.0;

// This fragment's place in the dither pattern, from 0 to 1
float resolveTreeFadeThreshold() {
    ivec2 cell = ivec2(gl_FragCoord.xy) & 3;

    return (TREE_FADE_PATTERN[cell.y * 4 + cell.x] + 0.5) / TREE_FADE_STEPS;
}

// True where this fragment's look has not risen in yet or has already fallen away
bool isTreeFaded() {
    float threshold = resolveTreeFadeThreshold();

    return u_treeFade >= 0.0 ? threshold < u_treeFade : threshold >= 1.0 + u_treeFade;
}

// How far across the hand-over band a grid-relative position lies: 0 where the grid's own trees show, 1 where the
// macro terrain's stand-ins do
float resolveTreeHandover(vec3 position) {
    if (u_treeHandover <= 0.0)
    return 0.0;

    float end   = (u_renderDistance * 0.5 - TREE_HANDOVER_INSET_CHUNKS) * u_chunkSize;
    float start = end - TREE_HANDOVER_BAND_CHUNKS * u_chunkSize;

    return clamp((length(position.xz - u_inverseView[3].xz) - start) / (end - start), 0.0, 1.0);
}

// True where a grid tree's fragment has already handed over to the stand-in
bool isTreeHandedOver(vec3 position) {
    return resolveTreeFadeThreshold() < resolveTreeHandover(position);
}

// True where a stand-in's fragment over a chunk the grid draws has not taken over yet
bool isStandInHeldBack(vec3 position) {
    return resolveTreeFadeThreshold() >= resolveTreeHandover(position);
}

#endif
