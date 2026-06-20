#ifndef MOON_LIGHT_DATA_GLSL
#define MOON_LIGHT_DATA_GLSL

layout(std140) uniform MoonLightData {
    vec3  u_moonDirection;
    float u_moonIntensity;
    vec3  u_moonColor;
};

#endif