#ifndef WEATHER_DATA_GLSL
#define WEATHER_DATA_GLSL

layout(std140) uniform WeatherData {
    float u_cloudCoverage;
    vec3  u_cloudColor;
    vec3  u_cloudTopColor;
    vec3  u_cloudShadowColor;
    float u_cloudAltitude;
    float u_cloudDensity;
    float u_cloudShadeStrength;
    float u_cloudRimLightStrength;
    float u_cloudAmbientOcclusionStrength;
    float u_cloudBrightnessMultiplier;
    float u_cloudToonBands;
    float u_cloudDensityNoiseScale;
    float u_cloudNoiseWarpStrength;
    float u_cloudCoverageBias;
    float u_cloudSilhouetteSoftness;
    float u_precipitationIntensity;
    float u_windSpeedScale;
    float u_fogDensityScale;
};

#endif