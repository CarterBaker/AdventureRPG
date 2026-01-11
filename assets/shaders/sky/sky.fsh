#version 150

in vec3 v_dir;  // from vertex shader
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
    // Normalized world-space direction of fragment
    vec3 dir = normalize(v_dir);

    // Factor in time of year and time of day into the look direction
    vec3 dynamicDir = getDynamicDir(dir);

    // Altitude 0..1 horizon→zenith
    float altitude = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);

    // --- compute cycle factors ONCE ---
    CycleFactors dayFactor = getDayNightFactors();

    // --- DEBUG OVERRIDE ---
    //dayFactor.night = 1.0;
    //dayFactor.day = 0.0;
    //dayFactor.sunrise = 0.0;
    //dayFactor.sunset = 0.0;
    // --- DEBUG END ---

    // --- compute cycle factors ONCE ---
    SeasonFactors seasonFactor = getSeasonFactors();

    // --- DEBUG OVERRIDE ---
    //seasonFactor.winter = 1.0;
    //seasonFactor.summer = 0.0;
    //seasonFactor.spring = 0.0;
    //seasonFactor.fall = 0.0;
    // --- DEBUG END ---

    // --- use precomputed noise helpers ---
    float baseNoise = calculateSkyNoiseBase(dir);                                      // simple time-of-day rotation
    float dailyVariation = calculateDailyVariation();                                  // daily offset curve
    float fullNoise = calculateSkyNoiseWithDailyVariation(baseNoise, dailyVariation);  // rotation + daily variation

    // --- 1) compute base sky color ---
    vec3 baseSky = getSkyColor(altitude, dir, fullNoise, dailyVariation, dayFactor, seasonFactor);

    // --- 2) compute nebula independently ---
    vec4 nebula = calculateNebula(dynamicDir, dailyVariation, dayFactor, seasonFactor);

    // --- 3) add stars ---
    vec4 stars = calculateStars(dynamicDir, altitude, dayFactor);

    vec3 finalColor = baseSky;

    // add nebula (uses nebula alpha only)
    finalColor = mix(finalColor, nebula.rgb, clamp(nebula.a, 0.0, 1.0));

    // add stars ON TOP, independently
    finalColor = mix(finalColor, stars.rgb, clamp(stars.a, 0.0, 1.0));

    fragColor = vec4(finalColor, 1.0);
}
