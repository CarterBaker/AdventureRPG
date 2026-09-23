#ifndef OCEAN_DATA_GLSL
#define OCEAN_DATA_GLSL

// Must match EngineSetting.OCEAN_WAVE_COUNT, OCEAN_TURBULENCE_UBO_MAX_ENTRIES,
// and OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR — GLSL has no visibility into the
// Java constants, so these are manually-kept mirrors, same convention
// WeatherMapData.glsl already uses for its own entry count.
#define OCEAN_WAVE_COUNT 4
#define OCEAN_TURBULENCE_MAX_ENTRIES 16
#define OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR 4

// Source: TurbulenceBufferSystem, one instance per grid.
// oceanWaves:                xy = wave vector (radians per block),
//                            z = this grid's phase (radians, reference chunk
//                            and elapsed time already folded in),
//                            w = share of the wave amplitude (shares sum to 1)
// oceanTurbulenceCells:      xy = cell center in blocks relative to this
//                            grid's reference chunk, z = radius in blocks,
//                            w = influence weight (already faded)
// oceanTurbulenceStrengths:  every cell's turbulence strength, packed four to
//                            a vector in cell order
// oceanSurface:              x = live ocean surface height in blocks,
//                            y = tide offset from sea level in blocks,
//                            z = baseline turbulence of this grid's own weather,
//                            w = wrapped clock in seconds
// oceanWaveScale:            x = wave amplitude per unit turbulence in blocks,
//                            y = wave amplitude cap in blocks
// oceanTurbulenceCount:      number of live cells
layout(std140) uniform OceanData {
    vec4  u_oceanWaves[OCEAN_WAVE_COUNT];
    vec4  u_oceanTurbulenceCells[OCEAN_TURBULENCE_MAX_ENTRIES];
    vec4  u_oceanTurbulenceStrengths[OCEAN_TURBULENCE_MAX_ENTRIES / OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR];
    vec4  u_oceanSurface;
    vec2  u_oceanWaveScale;
    int   u_oceanTurbulenceCount;
};

#endif
