#ifndef BEVEL_GLSL
#define BEVEL_GLSL

// Exact rounded-cube edge and corner solver. Rather than pushing a vertex by a face-local blend of
// tangent and normal, this reconstructs the Minkowski core of the block (the cube shrunk by the bevel
// radius) and re-projects the vertex onto a sphere of that radius around the core point. Both faces
// meeting at any physical edge build an identical core point and an identical projection direction from
// their own frames, so they converge on exactly the same world position — at an edge midpoint that is
// O - R(1 - 1/sqrt(2))(N1 + N2), and at a triple corner O - R(1 - 1/sqrt(3))(N1 + N2 + N3), with no
// dependence on quad size, tessellation density, or which axis a given face calls A. Edge state is read
// per unit cell with a hard clamped index and never interpolated along the edge, because any smoothing
// window would span a different cell range on a quad of a different length and the two faces would
// disagree exactly where a merged edge changes classification.

const float BEVEL_RADIUS     = 0.25;
const float EDGE_FADE_RADIUS = 0.5;

const int EDGE_STATE_INTERIOR = 0;
const int EDGE_STATE_BOUNDARY = 1;
const int EDGE_STATE_CONVEX   = 2;
const int EDGE_STATE_CONCAVE  = 3;

int edgeStateAt(float word, float pos, int lastIndex) {
    int idx = clamp(int(floor(pos)), 0, lastIndex);
    return (int(word) >> (idx * 2)) & 3;
}

void resolveAxis(
    float pos, float size,
    float posOther, int lastIndexOther,
    float word0, float word1,
    vec3 tangent,
    out float dist, out int state, out vec3 outward) {
    float d0 = pos;
    float d1 = size - pos;

    if (d0 <= d1) {
        dist    = d0;
        state   = edgeStateAt(word0, posOther, lastIndexOther);
        outward = -tangent;
    } else {
        dist    = d1;
        state   = edgeStateAt(word1, posOther, lastIndexOther);
        outward = tangent;
    }
}

float edgeFade(float dist, int state) {
    if (state == EDGE_STATE_INTERIOR)
    return 0.0;
    return 1.0 - smoothstep(0.0, EDGE_FADE_RADIUS, dist);
}

vec3 applyBevel(
    vec3 worldPos, vec3 normal,
    vec3 outwardA, float distA, int stateA,
    vec3 outwardB, float distB, int stateB,
    float strength) {
    float wA = (stateA >= EDGE_STATE_CONVEX) ? max(BEVEL_RADIUS - distA, 0.0) : 0.0;
    float wB = (stateB >= EDGE_STATE_CONVEX) ? max(BEVEL_RADIUS - distB, 0.0) : 0.0;

    if (wA <= 0.0 && wB <= 0.0)
    return worldPos;

    float signA = (stateA == EDGE_STATE_CONCAVE) ? -1.0 : 1.0;
    float signB = (stateB == EDGE_STATE_CONCAVE) ? -1.0 : 1.0;

    // A mixed corner is resolved by the stronger axis; on an exact tie both faces see the same pair of
    // magnitudes in some order, so the tie must be broken by sign alone or the two would diverge.
    float dominantSign;
    if (abs(wA - wB) < 1e-5)
    dominantSign = max(signA, signB);
    else
    dominantSign = (wA > wB) ? signA : signB;

    vec3 core = normal * BEVEL_RADIUS + dominantSign * (wA * outwardA + wB * outwardB);
    vec3 offset = -dominantSign * (core - BEVEL_RADIUS * normalize(core));

    return worldPos + offset * strength;
}

#endif