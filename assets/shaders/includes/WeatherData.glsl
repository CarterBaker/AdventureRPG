#ifndef WEATHER_DATA_GLSL
#define WEATHER_DATA_GLSL

layout(std140) uniform WeatherData {
    float u_cloudCoverage;
    vec3  u_cloudColor;
    float u_cloudType;
    float u_precipitationIntensity;
    float u_windSpeedScale;
    float u_fogDensityScale;
};

#endif