#ifndef SKY_WEATHER_PATTERN_DATA_GLSL
#define SKY_WEATHER_PATTERN_DATA_GLSL

#define SKY_CLOUD_MAX_COUNT 64

layout(std140) uniform SkyWeatherPatternData {
    int   u_cloudCount;
    vec3  u_cloudCenter[SKY_CLOUD_MAX_COUNT];
    vec3  u_cloudHalfExtent[SKY_CLOUD_MAX_COUNT];
    float u_cloudDomainRotation[SKY_CLOUD_MAX_COUNT];
    float u_cloudFadeAlpha[SKY_CLOUD_MAX_COUNT];
    float u_cloudIntensity[SKY_CLOUD_MAX_COUNT];
    vec3  u_cloudColor[SKY_CLOUD_MAX_COUNT];
    vec3  u_cloudTopColor[SKY_CLOUD_MAX_COUNT];
    vec3  u_cloudShadowColor[SKY_CLOUD_MAX_COUNT];
    float u_cloudDensity[SKY_CLOUD_MAX_COUNT];
    float u_cloudShadeStrength[SKY_CLOUD_MAX_COUNT];
    float u_cloudRimLightStrength[SKY_CLOUD_MAX_COUNT];
    float u_cloudAmbientOcclusionStrength[SKY_CLOUD_MAX_COUNT];
    float u_cloudBrightnessMultiplier[SKY_CLOUD_MAX_COUNT];
    float u_cloudToonBands[SKY_CLOUD_MAX_COUNT];
    float u_cloudDensityNoiseScale[SKY_CLOUD_MAX_COUNT];
    float u_cloudNoiseWarpStrength[SKY_CLOUD_MAX_COUNT];
    float u_cloudCoverageBias[SKY_CLOUD_MAX_COUNT];
    float u_cloudSilhouetteSoftness[SKY_CLOUD_MAX_COUNT];
    float u_cloudSeed[SKY_CLOUD_MAX_COUNT];
    float u_skyElevationFadeStart;
    float u_skyElevationLimit;
};

#endif