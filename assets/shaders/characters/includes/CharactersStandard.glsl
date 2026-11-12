#ifndef CHARACTERS_STANDARD_GLSL
#define CHARACTERS_STANDARD_GLSL

// The character atlas and its layers, one per texture alias, each its own image (TextureLoader seeds every one)
uniform sampler2DArray u_texture;

layout(std140) uniform CharactersStandard {
    int  u_layer_albedo;
    int  u_layer_ao;
    int  u_layer_height;
    int  u_layer_metallic;
    int  u_layer_normal;
    int  u_layer_roughness;
    int  u_layer_tint;
    vec2 u_uvPerBlock;
};

#endif
