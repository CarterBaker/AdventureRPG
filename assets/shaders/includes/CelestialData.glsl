#ifndef CELESTIAL_DATA_GLSL
#define CELESTIAL_DATA_GLSL

layout(std140) uniform CelestialData {
    mat4  u_starRotation;  // world space into the star sphere, +Z the north celestial pole
    float u_sunScale;      // the sun's apparent size from its calendar star's distance
    float u_moonScale;     // the moon's apparent size from its orbit, 0.0 with no moon
};

#endif