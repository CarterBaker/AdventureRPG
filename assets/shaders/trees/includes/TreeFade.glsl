#ifndef TREE_FADE_GLSL
#define TREE_FADE_GLSL

// A standing tree's look hands over to its next one by screen-door dither rather than at once: while a chunk or mega
// redraws its trees, the new look rises through a 4x4 ordered pattern as the old one falls away through the very same
// pattern, so every pixel shows exactly one of them and a tree that did not change looks the same throughout. The
// fade is positive for the look rising in, the share of the pattern it still leaves out, and negative for the look
// falling away, the share it already gave up; zero shows the whole tree. The steps match TREE_FADE_STEPS.

uniform float u_treeFade;

const float TREE_FADE_PATTERN[16] = float[16](
    0.0, 8.0, 2.0, 10.0,
    12.0, 4.0, 14.0, 6.0,
    3.0, 11.0, 1.0, 9.0,
    15.0, 7.0, 13.0, 5.0);
const float TREE_FADE_STEPS = 16.0;

// True where this fragment's look has not risen in yet or has already fallen away
bool isTreeFaded() {
    ivec2 cell      = ivec2(gl_FragCoord.xy) & 3;
    float threshold = (TREE_FADE_PATTERN[cell.y * 4 + cell.x] + 0.5) / TREE_FADE_STEPS;

    return u_treeFade >= 0.0 ? threshold < u_treeFade : threshold >= 1.0 + u_treeFade;
}

#endif
