#ifndef BLOCK_ORIENTATION_MAP_DATA_GLSL
#define BLOCK_ORIENTATION_MAP_DATA_GLSL

layout(std140) uniform BlockOrientationMapData {
    vec2 u_faceOrientations[24];  // x = axisMode (0=XZ, 1=ZY, 2=XY), y = spin (0-3)
};

#endif