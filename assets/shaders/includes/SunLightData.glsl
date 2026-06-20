#ifndef SUN_LIGHT_DATA_GLSL
#define SUN_LIGHT_DATA_GLSL

layout(std140) uniform SunLightData {
    vec3  u_sunDirection;
    float u_sunIntensity;
    vec3  u_sunColor;
};

#endif