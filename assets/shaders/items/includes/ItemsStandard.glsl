#ifndef ITEMS_STANDARD_GLSL
#define ITEMS_STANDARD_GLSL

layout(std140) uniform ItemsStandard {
    int  u_layer_albedo;
    vec2 u_uvPerBlock;
};

#endif