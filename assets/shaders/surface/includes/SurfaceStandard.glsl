#ifndef SURFACE_STANDARD_GLSL
#define SURFACE_STANDARD_GLSL

layout(std140) uniform SurfaceStandard {
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