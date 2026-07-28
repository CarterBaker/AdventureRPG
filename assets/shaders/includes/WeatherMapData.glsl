#ifndef WEATHER_MAP_DATA_GLSL
#define WEATHER_MAP_DATA_GLSL

// Must match EngineSetting.WEATHER_MAP_UBO_MAX_ENTRIES.
#define WEATHER_MAP_MAX_ENTRIES 128

// One entry = one (weather pattern × cloud archetype) pair.
// bounds:              xy = world min corner (chunkX, chunkZ), zw = max corner
// patternState:        x = distance, y = intensity, z = spread (band purity), w = fadeAlpha
// cloudColorScale:      xyz = cloud color, w = scale (width)
// cloudShape:           x = verticalThickness, y = altitude, z = density, w = driftSpeedScale
// cloudNoise:           x = densityNoiseScale, y = noiseWarpStrength, z = coverageBias, w = silhouetteSoftness
// cloudVariance0:       x = spreadRatio, y = sizeVarianceMin, z = sizeVarianceMax, w = elongationMin
// cloudVariance1:       x = elongationMax, y = cloudTypeIndex, z = patternSeed, w = unused
layout(std140) uniform WeatherMapData {
    vec4 u_weatherBounds[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherPatternState[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudColorScale[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudShape[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudNoise[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudVariance0[WEATHER_MAP_MAX_ENTRIES];
    vec4 u_weatherCloudVariance1[WEATHER_MAP_MAX_ENTRIES];
    int  u_weatherEntryCount;
};

#endif