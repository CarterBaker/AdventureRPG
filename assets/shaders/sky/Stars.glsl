#ifndef STARS_GLSL
#define STARS_GLSL

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"

// dir: normalized world-space direction
// altitude: 0=horizon, 1=zenith
// nightFactor: 0=day, 1=midnight
vec3 calculateStars(vec3 dir, float altitude, float nightFactor) {
    // Star field cell (deterministic)
    vec3 starCell = floor(dir * 200.0);

    // Deterministic hash-based star seed (no time-varying noise!)
    float starSeed = hash31(starCell);

    float starThreshold = 0.992;
    float starOn = step(starThreshold, starSeed);

    // Star brightness curve
    float starBright = pow(fract(starSeed * 931.7), 25.0) * 0.8;

    // Twinkle effect (small sinusoidal oscillation)
    float twinkle = 0.7 + 0.3 * sin(u_time * 1.2 + starSeed * 6.2831);

    // Fade stars near horizon and during day
    float starFade = smoothstep(0.0, 0.3, altitude) * nightFactor;

    return vec3(starOn * starBright * twinkle * starFade);
}

#endif
