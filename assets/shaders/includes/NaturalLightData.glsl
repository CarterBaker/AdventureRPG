#ifndef NATURAL_LIGHT_DATA_GLSL
#define NATURAL_LIGHT_DATA_GLSL

layout(std140) uniform NaturalLightData {
    vec3  u_sunDirection;
    float u_sunIntensity;
    vec3  u_sunColor;
    float _sunPad;
    vec3  u_moonDirection;
    float u_moonIntensity;
    vec3  u_moonColor;
    float _moonPad;
};

#endif