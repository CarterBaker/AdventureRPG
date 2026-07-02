#ifndef WEATHER_REGION_DATA_GLSL
#define WEATHER_REGION_DATA_GLSL

layout(std140) uniform WeatherRegionData {
    float u_cloudCoverageNorth;
    vec3  u_cloudColorNorth;
    float u_cloudTypeNorth;

    float u_cloudCoverageEast;
    vec3  u_cloudColorEast;
    float u_cloudTypeEast;

    float u_cloudCoverageSouth;
    vec3  u_cloudColorSouth;
    float u_cloudTypeSouth;

    float u_cloudCoverageWest;
    vec3  u_cloudColorWest;
    float u_cloudTypeWest;
};

#endif