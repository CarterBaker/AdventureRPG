#ifndef SSAO_DATA_GLSL
#define SSAO_DATA_GLSL

layout(std140) uniform SSAOData {
    vec4  u_samples[64];
    int   u_kernelSize;
    float u_radius;
    float u_bias;
};

uniform sampler2D u_texNoise; // 4x4 rotation vectors (RGB32F, GL_REPEAT, GL_NEAREST)

#endif