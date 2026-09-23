#ifndef OCEAN_SURFACE_GLSL
#define OCEAN_SURFACE_GLSL

#include "includes/OceanData.glsl"
#include "includes/NoiseUtility.glsl"

/*
 * The GPU side of TurbulenceManager. sampleOceanTurbulence() and
 * sampleOceanSwell() are the exact field and wave sum
 * TurbulenceManager.sampleSurfaceHeightBlocks() evaluates on the CPU, so a
 * vertex and a gameplay query at the same position agree on where the sea
 * stands. Positions are in blocks relative to the grid's reference chunk,
 * the same space u_gridPosition places every chunk in. Ripples are a purely
 * visual layer on top — wind-driven gradient noise whose strength follows
 * the same turbulence — that only ever bends the normal.
 */

const float OCEAN_RIPPLE_FREQUENCY      = 0.35;
const float OCEAN_RIPPLE_DRIFT_SPEED    = 0.6;
const float OCEAN_RIPPLE_STRENGTH       = 0.12;
const float OCEAN_RIPPLE_PROBE_BLOCKS   = 0.25;
const float OCEAN_FOAM_TURBULENCE_START = 1.2;
const float OCEAN_FOAM_TURBULENCE_FULL  = 3.2;

float sampleOceanTurbulence(vec2 pos) {
    float weightedStrength = u_oceanSurface.z;
    float totalWeight      = 1.0;
    int   cellCount        = min(u_oceanTurbulenceCount, OCEAN_TURBULENCE_MAX_ENTRIES);

    for (int i = 0; i < cellCount; i++) {
        vec4  cell = u_oceanTurbulenceCells[i];
        float t    = 1.0 - length(pos - cell.xy) / cell.z;

        if (t <= 0.0)
        continue;

        float influence = t * t * (3.0 - 2.0 * t) * cell.w;
        float strength  = u_oceanTurbulenceStrengths[i / OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR]
                                                    [i % OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR];

        weightedStrength += strength * influence;
        totalWeight      += influence;
    }

    return weightedStrength / totalWeight;
}

float oceanWaveAmplitude(float turbulence) {
    return min(turbulence * u_oceanWaveScale.x, u_oceanWaveScale.y);
}

float sampleOceanSwell(vec2 pos) {
    float swell = 0.0;

    for (int i = 0; i < OCEAN_WAVE_COUNT; i++) {
        vec4 wave = u_oceanWaves[i];
        swell += wave.w * sin(dot(wave.xy, pos) + wave.z);
    }

    return swell;
}

vec2 sampleOceanSwellSlope(vec2 pos, float amplitude) {
    vec2 slope = vec2(0.0);

    for (int i = 0; i < OCEAN_WAVE_COUNT; i++) {
        vec4 wave = u_oceanWaves[i];
        slope += wave.xy * (wave.w * cos(dot(wave.xy, pos) + wave.z));
    }

    return slope * amplitude;
}

float sampleOceanRipple(vec2 pos, vec2 drift) {
    return gradientNoise2D(pos * OCEAN_RIPPLE_FREQUENCY + drift);
}

vec3 computeOceanNormal(vec2 pos, float turbulence) {
    vec2  slope    = sampleOceanSwellSlope(pos, oceanWaveAmplitude(turbulence));
    vec2  drift    = u_oceanWaves[0].xy * (u_oceanSurface.w * OCEAN_RIPPLE_DRIFT_SPEED);
    float ripple   = sampleOceanRipple(pos, drift);
    float rippleX  = sampleOceanRipple(pos + vec2(OCEAN_RIPPLE_PROBE_BLOCKS, 0.0), drift) - ripple;
    float rippleZ  = sampleOceanRipple(pos + vec2(0.0, OCEAN_RIPPLE_PROBE_BLOCKS), drift) - ripple;
    float strength = OCEAN_RIPPLE_STRENGTH * (1.0 + turbulence) / OCEAN_RIPPLE_PROBE_BLOCKS;

    slope += vec2(rippleX, rippleZ) * strength;

    return normalize(vec3(-slope.x, 1.0, -slope.y));
}

float computeOceanFoam(vec2 pos, float turbulence) {
    float roughness = smoothstep(OCEAN_FOAM_TURBULENCE_START, OCEAN_FOAM_TURBULENCE_FULL, turbulence);
    float crest     = clamp(sampleOceanSwell(pos), 0.0, 1.0);

    return roughness * crest;
}

#endif
