#ifndef OCEAN_DATA_GLSL
#define OCEAN_DATA_GLSL

// Must match EngineSetting.OCEAN_WAVE_COUNT, OCEAN_WAVE_SWELL_COUNT, OCEAN_TURBULENCE_UBO_MAX_ENTRIES,
// OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR, OCEAN_EXPOSURE_GRID_SIZE, OCEAN_EXPOSURE_VALUES_PER_VECTOR,
// OCEAN_SEA_NOISE_SEED and OCEAN_NOISE_OCTAVE_SEED_STEP — GLSL has no visibility into the Java constants, so
// these are manually-kept mirrors, same convention WeatherMapData.glsl already uses for its own entry count.
// Every tuning value the CPU sampler also reads arrives in the block itself.
#define OCEAN_WAVE_COUNT 6
#define OCEAN_WAVE_SWELL_COUNT 2
#define OCEAN_TURBULENCE_MAX_ENTRIES 16
#define OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR 4
#define OCEAN_EXPOSURE_GRID_SIZE 32
#define OCEAN_EXPOSURE_VALUES_PER_VECTOR 4
#define OCEAN_SEA_NOISE_SEED 7919u
#define OCEAN_NOISE_OCTAVE_SEED_STEP 1013u

// Source: WaveBufferSystem, one instance per grid. Positions are in blocks relative to the grid's reference
// chunk, the same space u_gridPosition places every chunk in.
// oceanWaves:                xy = wave vector (radians per block), z = this grid's phase (radians, reference
//                            chunk and elapsed time already folded in), w = share of its band's amplitude
//                            (swell waves first, then chop; each band's shares sum to 1)
// oceanTurbulenceCells:      xy = cell center, z = radius in blocks, w = influence weight (already faded)
// oceanTurbulenceStrengths:  every cell's turbulence strength, packed four to a vector in cell order
// oceanExposure:             the exposure window, row-major from its origin cell, packed four to a vector
// oceanSurface:              x = live tide surface height in blocks, y = tide offset from sea level in blocks,
//                            z = baseline turbulence of this grid's own weather, w = wrapped clock in seconds
// oceanWaveScale:            x = chop amplitude per sea state, y = chop amplitude cap,
//                            z = swell amplitude per sea state past its start, w = swell amplitude cap
// oceanSeaState:             x = calm sea state, y = sea state swell starts at, z = wave crest sharpness,
//                            w = mean of the sharpened wave shape
// oceanWhitecap:             x = sea state whitecaps start at, y = sea state at full whitecaps,
//                            z = crest height whitecaps start at, w = amplitude epsilon
// oceanNoise:                xy = sea noise lattice offset, zw = sea noise lattice cells per block
// oceanNoiseShape:           x = multiplier at the noise minimum, y = at the maximum, z = contrast,
//                            w = detail octave weight
// oceanExposureGrid:         xy = exposure window origin, z = cell size in blocks, w = cells per side
// oceanCamera:               x = 1 while this grid's camera is under water, y = surface height above it,
//                            z = camera depth below that surface in blocks
// oceanTessellation:         x = near, y = mid, z = far tessellation radius in chunks, w = fade band in chunks
// oceanNoisePeriod:          sea noise lattice period in cells
// oceanTurbulenceCount:      number of live cells
layout(std140) uniform OceanData {
    vec4  u_oceanWaves[OCEAN_WAVE_COUNT];
    vec4  u_oceanTurbulenceCells[OCEAN_TURBULENCE_MAX_ENTRIES];
    vec4  u_oceanTurbulenceStrengths[OCEAN_TURBULENCE_MAX_ENTRIES / OCEAN_TURBULENCE_STRENGTHS_PER_VECTOR];
    vec4  u_oceanExposure[OCEAN_EXPOSURE_GRID_SIZE * OCEAN_EXPOSURE_GRID_SIZE / OCEAN_EXPOSURE_VALUES_PER_VECTOR];
    vec4  u_oceanSurface;
    vec4  u_oceanWaveScale;
    vec4  u_oceanSeaState;
    vec4  u_oceanWhitecap;
    vec4  u_oceanNoise;
    vec4  u_oceanNoiseShape;
    vec4  u_oceanExposureGrid;
    vec4  u_oceanCamera;
    vec4  u_oceanTessellation;
    vec2  u_oceanNoisePeriod;
    int   u_oceanTurbulenceCount;
};

#endif
