#ifndef BEVEL_GLSL
#define BEVEL_GLSL

// Exact rounded-cube edge and corner solver. Each edge word holds two bits per unit cell for the quad's
// run plus one padding cell at either end; the solver samples cell centers and interpolates between them,
// so bevel strength is continuous along an edge, both faces meeting at a quad's end resolve the same pair
// of physical cells, and a non-natural cell adjacent to a natural one inherits exactly half the bevel at
// their shared line and tapers to flat across itself. The offset itself reconstructs the Minkowski core of
// the block and reprojects onto a sphere of the bevel radius about it, so an edge midpoint lands on
// O - R(1 - 1/sqrt(2))(N1 + N2) and a uniform triple corner on O - R(1 - 1/sqrt(3))(N1 + N2 + N3) with no
// dependence on quad size, tessellation density, or which axis a face calls A. Where one axis is convex
// and the other concave the two roundings genuinely conflict and no single sphere satisfies both faces, so
// each axis contributes its own single-axis solution damped by the other's strength; both damping factors
// are functions of the shared edge alone, which is what closes the mixed corner that a single dominant
// sign could not.

const float BEVEL_RADIUS     = 0.25;
const float EDGE_FADE_RADIUS = 0.5;

const int EDGE_STATE_INTERIOR = 0;
const int EDGE_STATE_BOUNDARY = 1;
const int EDGE_STATE_CONVEX   = 2;
const int EDGE_STATE_CONCAVE  = 3;

void decodeEdgeCell(float word, int index, out float signedState, out float fadeFlag) {
    int state = (int(word) >> (index * 2)) & 3;
    signedState = (state == EDGE_STATE_CONVEX) ? 1.0 : ((state == EDGE_STATE_CONCAVE) ? -1.0 : 0.0);
    fadeFlag    = (state == EDGE_STATE_INTERIOR) ? 0.0 : 1.0;
}

void sampleEdgeWord(
    float word, float posAlong, float sizeAlong,
    out float signedState, out float fadeFlag) {
    float center = clamp(posAlong + 0.5, 0.5, sizeAlong + 0.5);
    float lowF   = floor(center);
    int   low    = int(lowF);
    float blend  = center - lowF;

    float signedLow, signedHigh, fadeLow, fadeHigh;
    decodeEdgeCell(word, low,     signedLow,  fadeLow);
    decodeEdgeCell(word, low + 1, signedHigh, fadeHigh);

    signedState = mix(signedLow, signedHigh, blend);
    fadeFlag    = mix(fadeLow,   fadeHigh,   blend);
}

void resolveAxis(
    float pos, float size,
    float posOther, float sizeOther,
    float word0, float word1,
    vec3 tangent,
    out float dist, out float signedState, out float fadeFlag, out vec3 outward) {
    float d0 = pos;
    float d1 = size - pos;

    bool nearLow = (d0 <= d1);

    dist    = nearLow ? d0 : d1;
    outward = nearLow ? -tangent : tangent;

    sampleEdgeWord(nearLow ? word0 : word1, posOther, sizeOther, signedState, fadeFlag);
}

float edgeFade(float dist, float fadeFlag) {
    return fadeFlag * (1.0 - smoothstep(0.0, EDGE_FADE_RADIUS, dist));
}

vec3 bevelOffsetAxis(vec3 normal, vec3 outward, float weight, float sign) {
    vec3 core = outward * weight + normal * (sign * BEVEL_RADIUS);
    return BEVEL_RADIUS * normalize(core) - core;
}

vec3 applyBevel(
    vec3 worldPos, vec3 normal,
    vec3 outwardA, float distA, float signedA,
    vec3 outwardB, float distB, float signedB,
    float strength) {
    float amountA = signedA * max(BEVEL_RADIUS - distA, 0.0);
    float amountB = signedB * max(BEVEL_RADIUS - distB, 0.0);

    float weightA = abs(amountA);
    float weightB = abs(amountB);

    if (weightA <= 1e-6 && weightB <= 1e-6)
    return worldPos;

    float signA = (amountA < 0.0) ? -1.0 : 1.0;
    float signB = (amountB < 0.0) ? -1.0 : 1.0;

    vec3 offset;

    if (weightA <= 1e-6 || weightB <= 1e-6 || signA == signB) {
        float sign = (weightA > weightB) ? signA : signB;
        vec3  core = outwardA * weightA + outwardB * weightB + normal * (sign * BEVEL_RADIUS);

        offset = BEVEL_RADIUS * normalize(core) - core;
    } else {
        float dampA = max(1.0 - weightB / BEVEL_RADIUS, 0.0);
        float dampB = max(1.0 - weightA / BEVEL_RADIUS, 0.0);

        offset = dampA * bevelOffsetAxis(normal, outwardA, weightA, signA)
        + dampB * bevelOffsetAxis(normal, outwardB, weightB, signB);
    }

    return worldPos + offset * strength;
}

#endif