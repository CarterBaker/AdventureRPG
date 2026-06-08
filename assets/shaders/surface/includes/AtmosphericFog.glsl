#ifndef ATMOSPHERIC_FOG_GLSL
#define ATMOSPHERIC_FOG_GLSL

#include "includes/GridCoordinateData.glsl"
#include "includes/SkyColorData.glsl"

vec3 applyAtmosphericFog(vec3 litColor) {
    float fogDist = clamp(u_distanceFromCenter / u_maxDistanceFromCenter, 0.0, 1.0);
    float fogT    = 1.0 - exp(-fogDist * fogDist * 6.0);
    vec3  fogColor = u_skyHorizonColor + 0.04;
    return mix(litColor, fogColor, clamp(fogT, 0.0, 1.0));
}

#endif