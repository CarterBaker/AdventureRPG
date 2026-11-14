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

// The face's UV and its screen gradients. Takes derivatives, so call it from uniform control flow.
FaceUV resolveFaceUV(vec3 localPos, vec3 normal, float encodedFace) {
    ivec2 faceLayout = resolveFaceLayout(localPos, normal, encodedFace);
    vec2 planar  = resolveFacePlanar(localPos, faceLayout.x);

    FaceUV face;
    face.uv = spinTileVector(planar - 0.5, faceLayout.y) + 0.5;
    face.dx = spinTileVector(dFdx(planar), faceLayout.y);
    face.dy = spinTileVector(dFdy(planar), faceLayout.y);
    return face;
}

#endif
