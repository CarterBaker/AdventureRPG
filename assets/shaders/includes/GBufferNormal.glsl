#ifndef GBUFFER_NORMAL_GLSL
#define GBUFFER_NORMAL_GLSL

/*
 * The G-buffer normal carries one flag in its length. Every pass that reads it
 * normalizes it first, so the length is free: a unit normal is an ordinary
 * surface, and a normal written at twice that length is a smooth surface —
 * a character's skin, whose low-poly facets stand for one continuous body —
 * whose folds the outline pass never inks. Silhouettes still ink as usual.
 */

const float GBUFFER_SMOOTH_NORMAL_LENGTH  = 2.0;
const float GBUFFER_SMOOTH_NORMAL_SQUARED = 2.25;

vec3 encodeSmoothNormal(vec3 normal) {
    return normal * GBUFFER_SMOOTH_NORMAL_LENGTH;
}

// True for a stored normal written by encodeSmoothNormal(); the test sits between the two lengths' squares.
bool isSmoothNormal(vec3 storedNormal) {
    return dot(storedNormal, storedNormal) > GBUFFER_SMOOTH_NORMAL_SQUARED;
}

#endif
