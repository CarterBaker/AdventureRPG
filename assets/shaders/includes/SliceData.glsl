#ifndef SLICE_DATA_GLSL
#define SLICE_DATA_GLSL

layout(std140) uniform SliceData {
    vec4 u_border;   // left, bottom, right, top in texture pixels
    vec2 u_texSize;  // texture dimensions in pixels
};

#endif