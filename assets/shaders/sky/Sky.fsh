#version 330 core

in  vec3 v_dir;
out vec4 fragColor;

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "sky/SkyColor.glsl"
#include "sky/SkyNoise.glsl"
#include "sky/Clouds.glsl"

void main() {
    vec3 dir = normalize(v_dir);

    float altitude = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);

    float baseNoise      = calculateSkyNoiseBase(dir);
    float dailyVariation = calculateDailyVariation();
    float fullNoise       = calculateSkyNoiseWithDailyVariation(baseNoise, dailyVariation);

    vec3 baseSky = getSkyColor(altitude, fullNoise);
    vec4 clouds  = calculateClouds(dir, u_randomNoiseFromDay);

    vec3 finalColor = mix(baseSky, clouds.rgb, clamp(clouds.a, 0.0, 1.0));

    fragColor = vec4(finalColor, 1.0);
}