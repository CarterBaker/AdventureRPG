#ifndef SKY_COLOR_GLSL
#define SKY_COLOR_GLSL

#include "includes/SkyColorData.glsl"

vec3 getSkyColor(float altitude, float fullNoise) {
    float altT   = pow(smoothstep(0.0, 1.0, clamp(altitude, 0.0, 1.0)), 1.2);
    float noisyT = clamp(altT + (fullNoise - 0.5) * 0.35, 0.0, 1.0);

    vec3 baseSky  = mix(u_skyHorizonColor, u_skyZenithColor, noisyT);
    baseSky += (fullNoise - 0.5) * 0.02;

    return baseSky;
}

#endif