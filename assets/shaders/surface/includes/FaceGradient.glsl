#ifndef FACE_GRADIENT_GLSL
#define FACE_GRADIENT_GLSL

#include "surface/includes/TiledSampling.glsl"

// A face's UV with its screen gradients, for the fragment stage only. The planar coordinate runs on unbroken across a
// merged quad, so its gradients never jump; the face's spin turns them exactly as it turns the UV, so every read
// takes explicit gradients that stay smooth where neighbouring blocks spin differently.

struct FaceUV {
    vec2 uv;
    vec2 dx;
    vec2 dy;
};

// A face's UV and its screen gradients for the planar axes and quarter turns given. Takes derivatives, so call it
// from uniform control flow.
FaceUV buildFaceUV(vec3 localPos, ivec2 faceLayout) {
    vec2 planar = resolveFacePlanar(localPos, faceLayout.x);

    FaceUV face;
    face.uv = spinTileVector(planar - 0.5, faceLayout.y) + 0.5;
    face.dx = spinTileVector(dFdx(planar), faceLayout.y);
    face.dy = spinTileVector(dFdy(planar), faceLayout.y);
    return face;
}

// The UV a face reads by its encoded face
FaceUV resolveFaceUV(vec3 localPos, vec3 normal, float encodedFace) {
    return buildFaceUV(localPos, resolveFaceLayout(localPos, normal, encodedFace));
}

// The UV a natural top or bottom face reads, spun by its block, whatever face it is laid over
FaceUV resolveNaturalFaceUV(vec3 localPos, vec3 normal, int faceOrdinal) {
    return buildFaceUV(localPos, ivec2(0, resolveNaturalSpin(localPos, normal, faceOrdinal)));
}

#endif
