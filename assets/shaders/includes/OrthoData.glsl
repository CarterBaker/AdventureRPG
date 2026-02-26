// ortho_data.glsl
#ifndef ORTHO_DATA_GLSL
#define ORTHO_DATA_GLSL

layout(std140) uniform OrthoData {
    mat4 u_orthoProjection;
    vec2 u_screenSize;
};

#endif