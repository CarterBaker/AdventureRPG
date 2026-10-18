#ifndef CHARACTER_HIDDEN_GLSL
#define CHARACTER_HIDDEN_GLSL

// A character drawn with a hidden bone — its head, seen from its own eyes — carries how much of each vertex that bone
// moves. Every fragment the bone moves at all is cut away, so the cut runs cleanly through the faces between hidden
// and kept vertices and leaves nothing stretched toward the eye; a weight of zero everywhere hides nothing.

const float CHARACTER_HIDDEN_WEIGHT_EPSILON = 0.001;

bool isHiddenFragment(float hiddenWeight) {
    return hiddenWeight > CHARACTER_HIDDEN_WEIGHT_EPSILON;
}

#endif
