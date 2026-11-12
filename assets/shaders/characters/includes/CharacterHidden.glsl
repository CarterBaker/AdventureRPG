#ifndef CHARACTER_HIDDEN_GLSL
#define CHARACTER_HIDDEN_GLSL

#include "characters/includes/CharacterAppearance.glsl"

// A character drawn through a bone mask carries how much of each vertex the masked bones move. Hiding — its head, seen
// from its own eyes — cuts away every fragment those bones move at all, so the cut runs cleanly through the faces
// between hidden and kept vertices and leaves nothing stretched toward the eye; showing — a part editor's part — keeps
// only the fragments those bones carry at least half of, so the part stands alone cut cleanly where it blends away.

const float CHARACTER_HIDDEN_WEIGHT_EPSILON = 0.001;
const float CHARACTER_SHOWN_WEIGHT          = 0.5;

bool isHiddenFragment(int instance, float maskWeight) {
    int mode = fetchMaskMode(instance);

    if (mode == CHARACTER_MASK_HIDE)
    return maskWeight > CHARACTER_HIDDEN_WEIGHT_EPSILON;

    if (mode == CHARACTER_MASK_SHOW)
    return maskWeight < CHARACTER_SHOWN_WEIGHT;

    return false;
}

#endif
