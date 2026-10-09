#ifndef TREE_SHADING_GLSL
#define TREE_SHADING_GLSL

#include "includes/SunLightData.glsl"
#include "items/includes/ItemsStandard.glsl"
#include "items/includes/BlockRepeat.glsl"
#include "surface/includes/CloudShadow.glsl"

// Shared by every tree shader: the item atlas every tree part is drawn from, the six faces in the order the
// sub-voxel mesher numbers them, the two axes each face runs along (u the axis after the normal's, v the one after
// that, as the mesher lays quads out), the tint every vertex carries as an exact 24-bit RGB triple, and the cloud
// shadow trees take exactly as the terrain beneath them does.

uniform sampler2DArray u_textureArray;

const vec3 TREE_FACE_NORMALS[6] = vec3[](
    vec3(0, 0, 1),
    vec3(1, 0, 0),
    vec3(0, 0,-1),
    vec3(-1, 0, 0),
    vec3(0, 1, 0),
    vec3(0,-1, 0));

const float TREE_SUN_SHADOW_MIN_ELEVATION = 0.05;

int resolveTreeFaceAxis(int face) {
    if (face == 1 || face == 3) return 0;
    if (face == 4 || face == 5) return 1;
    return 2;
}

vec3 resolveTreeAxis(int axis) {
    if (axis == 0) return vec3(1, 0, 0);
    if (axis == 1) return vec3(0, 1, 0);
    return vec3(0, 0, 1);
}

// The face most nearly matching a direction, for reading a block texture off a rounded surface
int resolveTreeFace(vec3 normal) {
    vec3 a = abs(normal);
    if (a.x >= a.y && a.x >= a.z) return normal.x > 0.0 ? 1 : 3;
    if (a.y >= a.z) return normal.y > 0.0 ? 4 : 5;
    return normal.z > 0.0 ? 0 : 2;
}

vec3 unpackTreeColor(float packedColor) {
    int col = int(packedColor);
    return vec3(float((col >> 16) & 255),
        float((col >>  8) & 255),
        float(col        & 255)) * (1.0 / 255.0);
}

float resolveTreeSunVisibility(vec3 worldPos) {
    if (u_sunIntensity <= 0.0)
    return 1.0;

    vec2 sunHorizonOffset = u_sunDirection.xz / max(u_sunDirection.y, TREE_SUN_SHADOW_MIN_ELEVATION);

    return 1.0 - sampleCloudShadow(worldPos, sunHorizonOffset);
}

#endif
