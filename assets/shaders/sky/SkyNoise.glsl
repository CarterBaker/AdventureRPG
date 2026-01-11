#ifndef SKY_NOISE_GLSL
#define SKY_NOISE_GLSL

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"

// Rotate a 2D vector by angle (radians)
vec2 rotate2D(vec2 v, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

// Calculate variation curve that's 0 at midnight (0.0 and 1.0), peaks at noon (0.5)
// Used to smoothly fade daily variations to avoid popping at midnight
float getVariationCurve() {
    float t = u_timeOfDay;
    if (t < 0.5) {
        // 0.0 -> 0.5: goes from 0 to 1
        return smoothstep(0.0, 0.5, t);
    } else {
        // 0.5 -> 1.0: goes from 1 back to 0
        return smoothstep(1.0, 0.5, t);
    }
}

// Calculate daily offset variation amount
// Returns the daily random offset scaled by variation curve to avoid midnight pop
// Use this when you need the SAME daily variation value across your shader
float calculateDailyVariation() {
    float variationCurve = getVariationCurve();
    return (u_randomNoiseFromDay - 0.5) * 0.30 * variationCurve;
}

// Calculate precomputed noise for sky based on direction and time only
// Does NOT include daily variation - useful for base rotation
float calculateSkyNoiseBase(vec3 dir) {
    float rotationAngle = u_timeOfDay * 2.0 * 3.1415926;  // 0..2π over full day
    vec2 rotatedCoords = rotate2D(dir.xz * 2.3, rotationAngle);
    return fbmNoise2D(rotatedCoords);
}

// Calculate precomputed noise WITH daily variation that doesn't pop at midnight
// Combines time-of-day rotation with daily random offset, smoothly faded
// Use this when you want DIFFERENT noise values that vary by direction each day
float calculateSkyNoiseWithDailyVariation(float baseNoise, float dailyVariation) {
    return fbmNoise2D(vec2(baseNoise) + dailyVariation * 0.4);
}

#endif