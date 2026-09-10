#ifndef BEVEL_GLSL
#define BEVEL_GLSL

// Corner and edge rounding for tessellated block faces. Each axis bordering a
// fragment contributes its own tangential pull toward the face center and its
// own signed push along the normal (inward for a convex edge, outward for a
// concave one); the two axes are summed independently rather than collapsed
// into one shared sign, so a corner where one adjoining edge is convex and
// the other concave never has to cancel one direction against the other.
// Mask lookups interpolate between neighboring unit cells instead of
// stepping at the cell boundary, so a merged multi-block edge never tears
// where its exposure classification changes partway along its length. All
// displacements are snapped to a fixed sub-block grid so two faces of
// different quad sizes meeting at a shared edge always land on the same
// quantized position regardless of their respective tessellation densities
// or face orientations.

const float BEVEL_RADIUS    = 0.30;
const float BEVEL_SIZE      = 0.09;
const float BEVEL_SNAP_GRID = 1.0 / 1024.0;

float maskBit(float mask, int bit) {
    return float((int(mask) >> bit) & 1);
}

// Bits are defined at unit-cell granularity. Sampling a raw bit at a
// fractional position would step the instant the query crosses into the
// next cell; treating each bit as a sample at its cell's center and blending
// linearly between neighboring centers removes that step.
float maskBitAt(float mask, float pos, int lastIndex) {
    float c    = clamp(pos - 0.5, 0.0, float(lastIndex));
    int   idx0 = int(floor(c));
    int   idx1 = min(idx0 + 1, lastIndex);
    float t    = fract(c);
    return mix(maskBit(mask, idx0), maskBit(mask, idx1), t);
}

vec3 snapToGrid(vec3 p, float grid) {
    return round(p / grid) * grid;
}

vec2 computeEdgeExposure(
    float posA, float posB,
    float sizeA, float sizeB,
    int iLastA, int iLastB,
    float maskA0, float maskA1, float maskB0, float maskB1,
    float radius) {

    float rawA0 = 1.0 - smoothstep(0.0, radius, posA);
    float rawA1 = 1.0 - smoothstep(0.0, radius, sizeA - posA);
    float rawB0 = 1.0 - smoothstep(0.0, radius, posB);
    float rawB1 = 1.0 - smoothstep(0.0, radius, sizeB - posB);

    float edgeA0 = rawA0 * maskBitAt(maskA0, posB, iLastB);
    float edgeA1 = rawA1 * maskBitAt(maskA1, posB, iLastB);
    float edgeB0 = rawB0 * maskBitAt(maskB0, posA, iLastA);
    float edgeB1 = rawB1 * maskBitAt(maskB1, posA, iLastA);

    return vec2(max(edgeA0, edgeA1), max(edgeB0, edgeB1));
}

vec4 computeSignedEdgeExposure(
    float posA, float posB,
    float sizeA, float sizeB,
    int iLastA, int iLastB,
    float maskA0, float maskA1, float maskB0, float maskB1,
    float negMaskA0, float negMaskA1, float negMaskB0, float negMaskB1,
    float radius) {

    float rawA0 = 1.0 - smoothstep(0.0, radius, posA);
    float rawA1 = 1.0 - smoothstep(0.0, radius, sizeA - posA);
    float rawB0 = 1.0 - smoothstep(0.0, radius, posB);
    float rawB1 = 1.0 - smoothstep(0.0, radius, sizeB - posB);

    float magA0 = rawA0 * maskBitAt(maskA0, posB, iLastB);
    float magA1 = rawA1 * maskBitAt(maskA1, posB, iLastB);
    float magB0 = rawB0 * maskBitAt(maskB0, posA, iLastA);
    float magB1 = rawB1 * maskBitAt(maskB1, posA, iLastA);

    float polarityA0 = maskBitAt(negMaskA0, posB, iLastB);
    float polarityA1 = maskBitAt(negMaskA1, posB, iLastB);
    float polarityB0 = maskBitAt(negMaskB0, posA, iLastA);
    float polarityB1 = maskBitAt(negMaskB1, posA, iLastA);

    float signedA0 = magA0 * (1.0 - 2.0 * polarityA0);
    float signedA1 = magA1 * (1.0 - 2.0 * polarityA1);
    float signedB0 = magB0 * (1.0 - 2.0 * polarityB0);
    float signedB1 = magB1 * (1.0 - 2.0 * polarityB1);

    float signedA = magA0 >= magA1 ? signedA0 : signedA1;
    float signedB = magB0 >= magB1 ? signedB0 : signedB1;

    return vec4(abs(signedA), sign(signedA), abs(signedB), sign(signedB));
}

void applyBevel(
    inout vec3 worldPos, inout vec3 normal,
    vec3 towardCenterA, vec3 towardCenterB,
    float posA, float posB, float sizeA, float sizeB, int iLastA, int iLastB,
    float maskA0, float maskA1, float maskB0, float maskB1,
    float negMaskA0, float negMaskA1, float negMaskB0, float negMaskB1) {

    vec4  signedAxes  = computeSignedEdgeExposure(posA, posB, sizeA, sizeB, iLastA, iLastB,
                                                   maskA0, maskA1, maskB0, maskB1,
                                                   negMaskA0, negMaskA1, negMaskB0, negMaskB1,
                                                   BEVEL_RADIUS);
    float bevelA     = signedAxes.x;
    float bevelASign = signedAxes.y;
    float bevelB     = signedAxes.z;
    float bevelBSign = signedAxes.w;

    if (max(bevelA, bevelB) <= 0.001)
        return;

    float signedBevelA = bevelA * bevelASign;
    float signedBevelB = bevelB * bevelBSign;

    // Tangential motion is always toward the face center regardless of
    // polarity. The normal offset is where convex and concave diverge, and
    // each axis contributes its own signed push independently, clamped to
    // the same single-edge depth ceiling rather than let two axes double up
    // unbounded — a corner where both axes agree lands at that ceiling
    // exactly like a straight edge does; a corner where they disagree sums
    // to their true difference instead of being averaged away to nothing.
    vec3  tangentialOffset = towardCenterA * BEVEL_SIZE * bevelA
                            + towardCenterB * BEVEL_SIZE * bevelB;
    float normalScale       = clamp(signedBevelA + signedBevelB, -1.0, 1.0);
    vec3  normalOffset      = -normal * BEVEL_SIZE * normalScale;

    worldPos += snapToGrid(tangentialOffset + normalOffset, BEVEL_SNAP_GRID);

    vec3  edgeDir     = -towardCenterA * signedBevelA - towardCenterB * signedBevelB;
    float edgeLen     = length(edgeDir);
    float blendAmount = clamp(length(vec2(signedBevelA, signedBevelB)), 0.0, 1.0);

    if (edgeLen > 0.001)
        normal = normalize(mix(normal, edgeDir / edgeLen, blendAmount));
}

#endif