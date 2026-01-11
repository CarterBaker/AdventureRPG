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

    // --- Sunrise / Sunset peaks ---
    float sunrisePeak = 0.25;
    float sunsetPeak  = 0.75;
    float transitionWidth = 0.04;

    // Bell-shaped curves for sunrise/sunset
    factors.sunrise = exp(-pow((u_timeOfDay - sunrisePeak) / transitionWidth, 2.0));
    factors.sunset  = exp(-pow((u_timeOfDay - sunsetPeak) / transitionWidth, 2.0));

    // --- IMPROVED Day/Night curves with gradual transitions ---

    // Define when transitions actually happen
    float nightEnd     = 0.20;  // Night ends (sunrise begins)
    float dayStart     = 0.30;  // Full day begins (sunrise ends)
    float dayEnd       = 0.70;  // Full day ends (sunset begins)
    float nightStart   = 0.80;  // Night begins (sunset ends)

    float dayFactor = 0.0;
    float nightFactor = 0.0;

    if (u_timeOfDay < nightEnd) {
        // Deep night (midnight to pre-dawn)
        nightFactor = 1.0;
        dayFactor = 0.0;
    } else if (u_timeOfDay < dayStart) {
        // Sunrise transition (night fades out, day fades in)
        float t = (u_timeOfDay - nightEnd) / (dayStart - nightEnd);
        t = smoothstep(0.0, 1.0, t); // Smooth S-curve
        nightFactor = 1.0 - t;
        dayFactor = t;
    } else if (u_timeOfDay < dayEnd) {
        // Full day (morning to afternoon)
        nightFactor = 0.0;
        dayFactor = 1.0;
    } else if (u_timeOfDay < nightStart) {
        // Sunset transition (day fades out, night fades in)
        float t = (u_timeOfDay - dayEnd) / (nightStart - dayEnd);
        t = smoothstep(0.0, 1.0, t); // Smooth S-curve
        dayFactor = 1.0 - t;
        nightFactor = t;
    } else {
        // Deep night (dusk to midnight)
        nightFactor = 1.0;
        dayFactor = 0.0;
    }

    // CRITICAL: Reduce night/day during sunrise/sunset peaks
    // This prevents stars/nebula from overpowering the sunrise/sunset colors
    float transitionPenalty = factors.sunrise + factors.sunset;
    nightFactor *= (1.0 - transitionPenalty * 0.8); // Heavily suppress night during transitions
    dayFactor *= (1.0 - transitionPenalty * 0.5);   // Moderately suppress day during transitions

    factors.night = nightFactor;
    factors.day = dayFactor;

    return factors;
}

#endif