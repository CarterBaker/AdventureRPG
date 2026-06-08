#version 330 core

in  vec3 v_dir;
out vec4 fragColor;

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "sky/DayNightCycle.glsl"
#include "sky/SeasonCycle.glsl"
#include "sky/Nebula.glsl"
#include "sky/SkyColor.glsl"
#include "sky/SkyNoise.glsl"
#include "sky/SkyRotation.glsl"
#include "sky/Stars.glsl"

void main() {
    vec3 dir        = normalize(v_dir);
    vec3 dynamicDir = getDynamicDir(dir);

    float altitude = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);

    CycleFactors  dayFactor    = getDayNightFactors();
    SeasonFactors seasonFactor = getSeasonFactors();

    float baseNoise      = calculateSkyNoiseBase(dir);
    float dailyVariation = calculateDailyVariation();
    float fullNoise      = calculateSkyNoiseWithDailyVariation(baseNoise, dailyVariation);

    // CPU computed horizon/zenith blended by altitude + noise only
    vec3 baseSky = getSkyColor(altitude, fullNoise);

    vec4 nebula = calculateNebula(dynamicDir, dailyVariation, dayFactor, seasonFactor);
    vec4 stars  = calculateStars(dynamicDir, altitude, dayFactor);

    vec3 finalColor = baseSky;
    finalColor = mix(finalColor, nebula.rgb, clamp(nebula.a, 0.0, 1.0));
    finalColor = mix(finalColor, stars.rgb,  clamp(stars.a,  0.0, 1.0));

    fragColor = vec4(finalColor, 1.0);
}