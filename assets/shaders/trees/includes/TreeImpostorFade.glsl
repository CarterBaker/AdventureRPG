#ifndef TREE_IMPOSTOR_FADE_GLSL
#define TREE_IMPOSTOR_FADE_GLSL

#include "includes/TimeData.glsl"
#include "trees/includes/TreeDither.glsl"

// An impostor instance rises in or falls away through the trees' dither over TREE_IMPOSTOR_FADE_SECONDS from the time
// it names on the shaders' clock (TimeData's u_time). Matches EngineSetting.TREE_IMPOSTOR_FADE_SECONDS.

const float TREE_IMPOSTOR_FADE_SECONDS = 0.3;

// True where an instance's impostor has not risen in yet, or has already fallen away: rising from its start when its
// direction is positive, falling from it when negative
bool isTreeImpostorFaded(float start, float direction) {
    float share = clamp((u_time - start) / TREE_IMPOSTOR_FADE_SECONDS, 0.0, 1.0);
    float shown = direction > 0.0 ? share : 1.0 - share;

    return resolveTreeFadeThreshold() >= shown;
}

#endif
