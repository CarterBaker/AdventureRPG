#ifndef DAY_NIGHT_CYCLE_GLSL
#define DAY_NIGHT_CYCLE_GLSL

#include "includes/TimeData.glsl"

// Returns factors for day/night/sunrise/sunset
struct CycleFactors {
    float night;    // 1 at midnight, 0 at noon
    float day;      // 1 at noon, 0 at midnight
    float sunrise;  // peaks around 0.25
    float sunset;   // peaks around 0.75
};

CycleFactors getDayNightFactors() {
    CycleFactors factors;

    // Night/day exponential curve
    factors.night = pow(abs(1.0 - 2.0 * u_timeOfDay), 2.0);
    factors.day = 1.0 - factors.night;

    // Sunrise exponential peak (around 0.25)
    float sunrisePeak = 0.25;
    factors.sunrise = exp(-pow((u_timeOfDay - sunrisePeak) / 0.05, 2.0));

    // Sunset exponential peak (around 0.75)
    float sunsetPeak = 0.75;
    factors.sunset = exp(-pow((u_timeOfDay - sunsetPeak) / 0.05, 2.0));

    return factors;
}

#endif