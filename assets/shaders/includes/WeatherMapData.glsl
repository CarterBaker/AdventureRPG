// WeatherMapData.glsl
#ifndef WEATHER_MAP_DATA_GLSL
#define WEATHER_MAP_DATA_GLSL

#define WEATHER_MAP_MAX_ENTRIES 32

// bounds:          xy = footprint min corner in blocks, zw = footprint max
//                  corner in blocks — both already relative to this grid's
//                  own reference chunk and already wrap-corrected.
// patternState:    x = intensity (cloud coverage), y = fadeAlpha,
//                  z = distance from reference chunk in blocks,
//                  w = near-range edge fade (1 = fully visible, 0 = culled)
// cloudColorScale: xyz = cloud color, w = scale (width)
// cloudMaterial:   x = saturation, y = fullness, z/w = reserved
// cloudShape:      x = verticalThickness, y = altitude, z = density,
//                  w = driftSpeedScale
// cloudNoise:      x = densityNoiseScale, y = noiseWarpStrength,
//                  z = coverageBias, w = silhouetteSoftness
// cloudVariance0:  x = spreadRatio, y = sizeVarianceMin, z = sizeVarianceMax,
//                  w = elongationMin
// cloudVariance1:  x = elongationMax, y = cloudSlotIndex, z = patternSeed,
//                  w = unused
// weatherHeightVariation: x = per-pattern height offset ratio of slab
//                  thickness, y = per-step local jitter ratio of slab
//                  thickness, z = local jitter frequency (1/blocks),
//                  w = vertical wisp frequency (1/blocks). Sourced from
//                  EngineSetting so the CPU's layer-band margin and the
//                  shader's actual displacement can never disagree.
// weatherCloudLayerMinY/MaxY: this frame's actual min/max cloud altitude
//                  band across all written entries, already widened by the
//                  full height variation above — lets the fullscreen pass
//                  bound its raymarch to where cloud volume can physically
//                  exist instead of the whole atmosphere column.
// weatherRangeBlocks: the CPU-side weather pattern sampling range (see
//                  WeatherPatternManager.getRangeChunks()), converted to
//                  blocks — terrain-independent on purpose. Drives the
//                  fullscreen pass's horizon dome bend so clouds curve
//                  toward the fade altitude across the same distance the
//                  weather simulation actually spans.
layout(std140) uniform WeatherMapData {
    vec4 u_weatherBounds[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherPatternState[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudColorScale[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudMaterial[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudShape[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudNoise[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudVariance0[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudVariance1[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherHeightVariation;
    int u_weatherEntryCount;
    float u_weatherCloudLayerMinY;
    float u_weatherCloudLayerMaxY;
    float u_weatherRangeBlocks;
};

#endif