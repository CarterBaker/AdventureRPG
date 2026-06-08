#ifndef SKY_COLOR_DATA_GLSL
#define SKY_COLOR_DATA_GLSL
layout(std140) uniform SkyColorData {
    vec3  u_skyHorizonColor;
    float u_maxDistanceFromCenter;
    vec3  u_skyZenithColor;
};
#endif