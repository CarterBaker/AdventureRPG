#ifndef BEVEL_GLSL
#define BEVEL_GLSL

// Corner and edge rounding for tessellated block faces. Convex edges chamfer inward;
// concave edges mirror the same tangential motion (always toward face center) but
// push the normal outward instead of inward, so both faces at any shared vertex
// always displace to the same 3D point. All displacements are snapped to a fixed
// sub-block grid so two faces of different quad sizes meeting at a shared edge
// always land on the same quantized position regardless of their respective
// tessellation densities or face orientations.

const float BEVEL_RADIUS    = 0.30;
const float BEVEL_SIZE      = 0.09;
const float BEVEL_SNAP_GRID = 1.0 / 1024.0;

float maskBit(float mask, int bit) {
    return float((int(mask) >> bit) & 1);
}

float maskBitAt(float mask, float pos, int lastIndex) {
    int idx = clamp(int(floor(pos)), 0, lastIndex);
    return maskBit(mask, idx);
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
    float bevelMax   = max(bevelA, bevelB);

    if (bevelMax <= 0.001)
        return;

    float signedBevelA  = bevelA * bevelASign;
    float signedBevelB  = bevelB * bevelBSign;
    float signWeight    = max(bevelA + bevelB, 0.0001);
    float normalSign    = (bevelA * bevelASign + bevelB * bevelBSign) / signWeight;
    float signAgreement = (bevelASign * bevelBSign >= 0.0) ? 1.0 : abs(normalSign);

    // Tangential movement is always toward the face center — unsigned magnitudes only.
    // The normal offset carries the convex/concave distinction via normalSign: positive
    // pushes into the surface (convex), negative pushes outward (concave). Using the
    // signed values here was the bug: it reversed the tangential direction for concave
    // edges, displacing both faces at a shared vertex in opposite directions and causing
    // intersection when viewed through the geometry.
    vec3 tangentialOffset = towardCenterA * BEVEL_SIZE * bevelA
                           + towardCenterB * BEVEL_SIZE * bevelB;
    vec3 normalOffset     = -normal * BEVEL_SIZE * bevelMax * normalSign;

    worldPos += snapToGrid(tangentialOffset + normalOffset, BEVEL_SNAP_GRID);

    // edgeDir uses the signed values so it points toward the correct fillet bisector:
    // away from the face center for convex (outward corner normal), toward the face
    // center for concave (inward corner normal into open space).
    vec3  edgeDir = -towardCenterA * signedBevelA - towardCenterB * signedBevelB;
    float edgeLen = length(edgeDir);
    if (edgeLen > 0.001)
        normal = normalize(mix(normal, edgeDir / edgeLen, bevelMax * signAgreement));
}

#endif