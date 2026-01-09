#version 150

in vec3 v_dir;  // from vertex shader
out vec4 fragColor;

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "sky/DayNightCycle.glsl"
#include "sky/Nebula.glsl"
#include "sky/SkyColor.glsl"
#include "sky/SkyNoiseHelper.glsl"

void main() {
    // Normalized world-space direction of fragment
    vec3 dir = normalize(v_dir);

    // Altitude 0..1 horizon→zenith
    float altitude = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);

    // --- compute cycle factors ONCE ---
    CycleFactors factors = getDayNightFactors();

    // --- DEBUG OVERRIDE: force night for testing ---
    factors.night = 1.0;
    factors.day = 0.0;
    factors.sunrise = 0.0;
    factors.sunset = 0.0;
    // --- DEBUG END ---

    // --- use precomputed noise helpers ---
    float baseNoise = calculateSkyNoiseBase(dir);                                      // simple time-of-day rotation
    float dailyVariation = calculateDailyVariation();                                  // daily offset curve
    float fullNoise = calculateSkyNoiseWithDailyVariation(baseNoise, dailyVariation);  // rotation + daily variation

    // --- 1) compute base sky color ---
    vec3 baseSky = getSkyColor(altitude, dir, fullNoise, dailyVariation, factors);

    // --- 2) compute nebula independently ---
    vec3 nebula = calculateNebula(dir, altitude, factors);  // now does NOT take baseSky

    // Define a nebula intensity factor (0 = invisible, 1 = fully dominates)
    float nebulaIntensity = 0.5;

    // Blend nebula into sky
    vec3 finalColor = mix(baseSky, nebula, nebulaIntensity);

    fragColor = vec4(finalColor, 1.0);
}
