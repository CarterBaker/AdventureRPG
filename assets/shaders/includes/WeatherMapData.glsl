#ifndef WEATHER_MAP_DATA_GLSL
#define WEATHER_MAP_DATA_GLSL

// Must match EngineSetting.WEATHER_MAP_UBO_MAX_ENTRIES. Kept well under the
// 16KB GL_MAX_UNIFORM_BLOCK_SIZE floor guaranteed by GL 3.3 core — the
// previous 256-entry layout (~32.75KB) exceeded that floor on plenty of
// real drivers, which is why nothing ever rendered regardless of shader
// changes: the block simply never bound correctly.
#define WEATHER_MAP_MAX_ENTRIES 96

// bounds:              xy = world min corner (chunkX, chunkZ), zw = max corner
// patternState:        x = distance, y = intensity, z = reserved, w = fadeAlpha
// cloudColorScale:      xyz = cloud color, w = scale (width)
// cloudMaterial:        x = saturation, y = fullness, z/w = reserved
// cloudShape:           x = verticalThickness, y = altitude, z = density, w = driftSpeedScale
// cloudNoise:           x = densityNoiseScale, y = noiseWarpStrength, z = coverageBias, w = silhouetteSoftness
// cloudVariance0:       x = spreadRatio, y = sizeVarianceMin, z = sizeVarianceMax, w = elongationMin
// cloudVariance1:       x = elongationMax, y = cloudTypeIndex, z = patternSeed, w = unused
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