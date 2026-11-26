#ifndef TREE_DITHER_GLSL
#define TREE_DITHER_GLSL

// The 4x4 ordered pattern every tree look rises in and falls away through, screen-door, so two looks of one tree
// hand over without blending. The steps match TREE_FADE_STEPS.

const float TREE_FADE_PATTERN[16] = float[16](
    0.0, 8.0, 2.0, 10.0,
    12.0, 4.0, 14.0, 6.0,
    3.0, 11.0, 1.0, 9.0,
    15.0, 7.0, 13.0, 5.0);
const float TREE_FADE_STEPS = 16.0;

// This fragment's place in the dither pattern, from 0 to 1
float resolveTreeFadeThreshold() {
    ivec2 cell = ivec2(gl_FragCoord.xy) & 3;

    return (TREE_FADE_PATTERN[cell.y * 4 + cell.x] + 0.5) / TREE_FADE_STEPS;
}

#endif
