#ifndef TEXTURE_LAYER_DATA_GLSL
#define TEXTURE_LAYER_DATA_GLSL

layout(std140) uniform StandardTextureLayoutData {
    int u_layer_albedo;
    int u_layer_ao;
    int u_layer_emission;
    int u_layer_height;
    int u_layer_metallic;
    int u_layer_normal;
    int u_layer_specular;
    vec2 u_uvPerBlock;
};

#endif