#ifndef WEATHER_MAP_DATA_GLSL
#define WEATHER_MAP_DATA_GLSL

// Must match EngineSetting.WEATHER_MAP_UBO_MAX_ENTRIES.
#define WEATHER_MAP_MAX_ENTRIES 128

// One entry = one (weather pattern × cloud archetype) pair.
// bounds:              xy = world min corner (chunkX, chunkZ), zw = max corner
// patternState:        x = distance, y = intensity, z = spread (band purity), w = fadeAlpha
// cloudColorScale:      xyz = cloud color, w = scale (width)
// cloudMaterial:        x = saturation, y = fullness (0 = flat sheet, 1 = billowy mass), z/w = reserved
// cloudShape:           x = verticalThickness, y = altitude, z = density, w = driftSpeedScale
// cloudNoise:           x = densityNoiseScale, y = noiseWarpStrength, z = coverageBias, w = silhouetteSoftness
// cloudVariance0:       x = spreadRatio, y = sizeVarianceMin, z = sizeVarianceMax, w = elongationMin
// cloudVariance1:       x = elongationMax, y = cloudTypeIndex, z = patternSeed, w = unused
//
// u_weatherOuterRangeChunks/u_weatherNearRangeChunks mirror the same ranges
// EngineSetting.WEATHER_OUTER_RANGE_CHUNKS/WEATHER_NEAR_RANGE_CHUNKS drive
// CPU-side, written once on awake — the skybox and overhead cloud shaders
// read the ring boundary from here rather than duplicating the constants.
layout(std140) uniform WeatherMapData {
    vec4 u_weatherBounds[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherPatternState[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudColorScale[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudMaterial[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudShape[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudNoise[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudVariance0[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudVariance1[WEATHER_MAP_MAX_ENTRIES];
    int   u_weatherEntryCount;
    float u_weatherOuterRangeChunks;
    float u_weatherNearRangeChunks;
};

#endif