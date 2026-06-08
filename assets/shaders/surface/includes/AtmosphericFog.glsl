#ifndef ATMOSPHERIC_FOG_GLSL
#define ATMOSPHERIC_FOG_GLSL

#include "includes/GridCoordinateData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SettingsData.glsl"

vec3 applyAtmosphericFog(vec3 litColor) {
    float half       = u_renderDistance * 0.5 - 0.5;
    float trueMax    = half * half * 2.0;
    float fogDist    = clamp(u_distanceFromCenter / trueMax, 0.0, 1.0);
    float linearDist = sqrt(fogDist);

    float fogT = smoothstep(0.0, 0.5, linearDist) * 0.25
    + smoothstep(0.5, 1.0, linearDist) * 0.15;

    vec3 fogColor = u_skyHorizonColor + 0.04;
    return mix(litColor, fogColor, clamp(fogT, 0.0, 0.9));
}

#endif