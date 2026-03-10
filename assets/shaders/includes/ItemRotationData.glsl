#ifndef ITEM_ROTATION_DATA_GLSL
#define ITEM_ROTATION_DATA_GLSL
layout(std140) uniform ItemRotationData {
    mat4 u_rotations[24];
};
#endif