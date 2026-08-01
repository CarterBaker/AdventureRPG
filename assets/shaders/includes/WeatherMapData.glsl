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
// cloudVariance1:  x = elongationMax, y = cloudTypeIndex, z = patternSeed,
//                  w = unused
layout(std140) uniform WeatherMapData {
    vec4 u_weatherBounds[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherPatternState[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudColorScale[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudMaterial[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudShape[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudNoise[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudVariance0[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudVariance1[WEATHER_MAP_MAX_ENTRIES];
    int u_weatherEntryCount;
};

#endif