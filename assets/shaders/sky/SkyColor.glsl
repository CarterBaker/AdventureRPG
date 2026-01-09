#ifndef SKY_COLOR_GLSL
#define SKY_COLOR_GLSL

#include "includes/TimeData.glsl"
#include "sky/DayNightCycle.glsl"

// altitude: 0 = horizon, 1 = zenith
// noise: precomputed coherent FBM
// factors: day/night/sunrise/sunset weights (fully trusted)
vec3 getSkyColor(float altitude, vec3 worldDir, float fullNoise, float dailyVariation, CycleFactors factors) {
    // --------------------------------------
    // BASE PALETTE
    // --------------------------------------
    vec3 nightTop = vec3(0.02, 0.02, 0.08);
    vec3 nightBottom = vec3(0.005, 0.005, 0.02);

    vec3 dayTop = vec3(0.60, 0.82, 1.00);
    vec3 dayBottom = vec3(0.32, 0.52, 0.80);

    // Base sunrise/sunset colors (before daily variation)
    vec3 sunriseBase = vec3(1.00, 0.50, 0.20);
    vec3 sunsetBase = vec3(0.90, 0.30, 0.15);

    // Apply daily variation
    vec3 sunriseColor = sunriseBase + dailyVariation;
    vec3 sunsetColor = sunsetBase + dailyVariation;

    // --------------------------------------
    // BLEND SKY BY FACTORS (FULLY TRUSTED)
    // --------------------------------------
    vec3 topColor = nightTop * factors.night + dayTop * factors.day;
    topColor = mix(topColor, sunriseColor, factors.sunrise);
    topColor = mix(topColor, sunsetColor, factors.sunset);

    vec3 bottomColor = nightBottom * factors.night + dayBottom * factors.day;
    bottomColor = mix(bottomColor, sunriseColor, factors.sunrise);
    bottomColor = mix(bottomColor, sunsetColor, factors.sunset);

    // --------------------------------------
    // ALTITUDE CURVE
    // --------------------------------------
    float altT = clamp(altitude, 0.0, 1.0);
    altT = smoothstep(0.0, 1.0, altT);
    altT = pow(altT, 1.2);

    // --------------------------------------
    // NOISE-DRIVEN SMOOTH BLEND
    // Creates organic, non-linear gradient
    // --------------------------------------
    float noisyT = altT + (fullNoise - 0.5) * 0.35;
    noisyT = clamp(noisyT, 0.0, 1.0);

    vec3 base = mix(bottomColor, topColor, noisyT);

    // Subtle noise texture overlay
    base += (fullNoise - 0.5) * 0.02;

    // --------------------------------------
    // HORIZON DESATURATION
    // --------------------------------------
    float g = dot(base, vec3(0.333));
    base = mix(base, vec3(g), 0.12 * (1.0 - altT));

    return base;
}

#endif