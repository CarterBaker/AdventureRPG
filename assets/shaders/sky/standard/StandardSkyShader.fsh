#version 330 core

in  vec3 v_dir;
out vec4 fragColor;

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "sky/util/SkyColor.glsl"
#include "sky/util/SkyNoise.glsl"
#include "sky/util/Clouds.glsl"

void main() {
    vec3 dir = normalize(v_dir);

    float altitude = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);

    float baseNoise      = calculateSkyNoiseBase(dir);
    float dailyVariation = calculateDailyVariation();
    float fullNoise       = calculateSkyNoiseWithDailyVariation(baseNoise, dailyVariation);

    vec3 baseSky = getSkyColor(altitude, fullNoise);
    vec4 clouds  = calculateClouds(dir);

    // clouds.rgb/clouds.a are premultiplied by calculateClouds — add rgb
    // directly rather than re-blending it against alpha a second time, or
    // partially-transparent cloud edges read far darker/harder than their
    // true coverage.
    float cloudCoverage = clamp(clouds.a, 0.0, 1.0);
    vec3 finalColor = baseSky * (1.0 - cloudCoverage) + clouds.rgb;

    fragColor = vec4(finalColor, 1.0);
}