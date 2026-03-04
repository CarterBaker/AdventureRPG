#ifndef STANDARD_ITEM_LAYOUT_DATA_GLSL
#define STANDARD_ITEM_LAYOUT_DATA_GLSL

layout(std140) uniform StandardItemLayoutData {
    int  u_layer_albedo;
    vec2 u_uvPerBlock;
};

#endif