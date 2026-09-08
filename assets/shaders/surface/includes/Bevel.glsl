#ifndef BEVEL_GLSL
#define BEVEL_GLSL

// Corner/edge rounding for tessellated block faces. Every mask bit comes
// from FullGeometryBranch's own per-cell exposure classification, so the
// same physical edge always resolves the same way regardless of which face
// sharing it is evaluating. The tangential push on each axis is applied at
// that axis' own full magnitude and sign independently, so up to three
// mutually perpendicular faces sharing a true corner converge on the same
// point; the normal push blends the two axes' signs by their own relative
// magnitude instead of switching discretely between them, so a vertex
// sliding through a corner — or along an edge where a convex run meets a
// concave run — never creases at the point where one axis overtakes the
// other.
//
// Mask lookups are deliberately hard, per-cell reads (no blending across
// cell boundaries): floor(pos) always maps to the true world-block cell
// regardless of how big the merged quad happens to be, so two quads of
// different sizes that both touch the same physical cell always agree on
// its exposure bit. Blending that value toward a neighboring cell would
// only be safe if that neighbor stayed inside the same patch — at a
// T-junction (a large merged quad sharing an edge with several smaller
// ones, which happens routinely at chunk borders and wherever
// differently-merged faces meet) the small quads see that same boundary as
// their own hard edge with nothing to blend toward, so a blended value on
// one side and a raw value on the other would crack the seam.

const float BEVEL_RADIUS = 0.30;
const float BEVEL_SIZE   = 0.09;

float maskBit(float mask, int bit) {
    return float((int(mask) >> bit) & 1);
}

float maskBitAt(float mask, float pos, int lastIndex) {
    int idx = clamp(int(floor(pos)), 0, lastIndex);
    return maskBit(mask, idx);
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
    float bevelMax   = max(bevelA, bevelB);

    if (bevelMax <= 0.001)
        return;

    float signWeight = max(bevelA + bevelB, 0.0001);
    float normalSign = (bevelA * bevelASign + bevelB * bevelBSign) / signWeight;

    worldPos += towardCenterA * (BEVEL_SIZE * bevelASign) * bevelA;
    worldPos += towardCenterB * (BEVEL_SIZE * bevelBSign) * bevelB;
    worldPos -= normal * BEVEL_SIZE * bevelMax * normalSign;

    vec3  edgeDir = -towardCenterA * bevelA * bevelASign - towardCenterB * bevelB * bevelBSign;
    float edgeLen = length(edgeDir);
    if (edgeLen > 0.001)
        normal = normalize(mix(normal, edgeDir / edgeLen, bevelMax));
}

#endif
