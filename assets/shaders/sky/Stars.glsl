#ifndef STARS_GLSL
#define STARS_GLSL

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "sky/DayNightCycle.glsl"

// dir: normalized world-space direction
// altitude: 0=horizon, 1=zenith
// dayFactor: struct with .night controlling star visibility
vec4 calculateStars(vec3 dir, float altitude, CycleFactors dayFactor) {
    float nightFactor = dayFactor.night;

    // CRITICAL FIX: Much sharper cutoff to eliminate daytime stars
    // Power of 6-8 creates very aggressive fade - stars only visible in deep night
    nightFactor = pow(nightFactor, 6.0);
    nightFactor = smoothstep(0.02, 0.12, nightFactor);

    // Early exit if barely any night
    if (nightFactor < 0.001) {
        return vec4(0.0);
    }

    // Horizon compression
    float horizonCompression = 1.0 + (1.0 - altitude) * 0.5;
    vec3 adjustedDir = normalize(vec3(dir.x, dir.y * horizonCompression, dir.z));

    // ----------------------------
    // MAIN STAR LAYER (deterministic, jittered)
    // ----------------------------
    vec3 starCell = floor(adjustedDir * 180.0);
    float mainSeed = hash31(starCell);

    vec3 starOffset = vec3(
        fract(sin(mainSeed * 12.9898) * 43758.5453),
        fract(sin(mainSeed * 78.233) * 43758.5453),
        fract(sin(mainSeed * 39.3467) * 43758.5453));

    vec3 starPos = starCell + starOffset;
    vec3 starDir = normalize(starPos / 180.0);

    float starBaseSize = 0.2 + fract(sin(mainSeed * 43758.5453123) * 43758.5453123) * 0.8;
    float sizeBias = pow(starBaseSize, 2.5);

    float starThreshold = 0.992 - (1.0 - altitude) * 0.002 + (1.0 - sizeBias) * 0.008;
    bool starExists = mainSeed >= starThreshold;

    if (!starExists) return vec4(0.0);

    float nudge = fract(sin(mainSeed * 7.123) * 43758.5453) * 0.4 - 0.2;
    starDir.xy += nudge * (1.0 - altitude);

    // ----------------------------
    // TWINKLE MASK LAYER (SLOWED ~1000×)
    // ----------------------------

    // Slow down twinkle/flicker time
    const float TWINKLE_SLOWDOWN = 1500.0;
    float slowTime = u_time / TWINKLE_SLOWDOWN;

    float angle = slowTime * 0.5;

    vec3 twinkleDir = normalize(vec3(
            starDir.x * cos(angle) - starDir.z * sin(angle),
            starDir.y,
            starDir.x * sin(angle) + starDir.z * cos(angle)));

    vec3 twinkleCell = floor(twinkleDir * 200.0);
    float twinkleSeed = hash31(twinkleCell);

    float sizeFactor = smoothstep(0.5, 1.0, starBaseSize);

    // flicker slowed ~1000×
    float flicker = sin(slowTime * mix(0.08, 0.6, sizeFactor) + twinkleSeed * 500.0);

    float phaseWidth = mix(0.22, 0.05, sizeFactor);
    float twinkleStrength = sizeFactor * smoothstep(0.8 - phaseWidth, 0.85 + phaseWidth, flicker);

    float starRadius = starBaseSize * 0.002 + twinkleStrength * 0.004;

    // ----------------------------
    // CLOUD NOISE LAYER (soft clouds)
    // ----------------------------
    float cloudNoise = fbmNoise3D(starDir * 10.0 + vec3(u_time * 0.01));
    float cloudMask = clamp(1.0 - 0.5 * cloudNoise, 0.6, 1.0);

    // ----------------------------
    // BASE BRIGHTNESS
    // ----------------------------
    float brightness = nightFactor * (1.0 + twinkleStrength * 0.3);
    brightness *= cloudMask;

    // ----------------------------
    // DOUBLE BRIGHTNESS ON TWINKLE (clamped)
    // ----------------------------
    if (twinkleStrength > 0.0) {
        brightness = clamp(brightness * 2.0, 0.0, 1.0);
    }

    // ----------------------------
    // SUBTLE STAR COLOR VARIATION
    // ----------------------------
    vec3 colorVariation = vec3(
        fract(sin(mainSeed * 12.9898) * 43758.5453) * 0.1 - 0.05,
        fract(sin(mainSeed * 78.233) * 43758.5453) * 0.1 - 0.05,
        fract(sin(mainSeed * 39.3467) * 43758.5453) * 0.1 - 0.05);

    // ----------------------------
    // RADIAL MASK (CIRCLE -> SQUARE MORPH)
    // ----------------------------
    vec2 uv = fract(starDir.xy * 200.0) - 0.5;

    // Circle distance (default shape)
    float circularDist = length(uv);

    // Square distance (chebyshev/max distance)
    float squareDist = max(abs(uv.x), abs(uv.y));

    // Morph from circle to square based on twinkle strength
    float dist = mix(circularDist, squareDist, twinkleStrength);

    float mask = smoothstep(starRadius, starRadius * 0.8, dist);

    // ----------------------------
    // FINAL COLOR OUTPUT
    // ----------------------------
    vec3 finalColor = vec3(brightness) + colorVariation;
    finalColor = clamp(finalColor, 0.0, 1.0);

    return vec4(finalColor * (1.0 - mask), brightness * (1.0 - mask));
}

#endif
