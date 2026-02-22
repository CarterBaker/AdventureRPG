#ifndef NATURAL_LIGHT_DATA_GLSL
#define NATURAL_LIGHT_DATA_GLSL

layout(std140) uniform DirectionalLightData {
    vec3  u_lightDirection;
    float u_lightIntensity;
    vec3  u_lightColor;
};

#endif