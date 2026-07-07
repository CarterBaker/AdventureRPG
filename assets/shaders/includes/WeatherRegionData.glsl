#ifndef WEATHER_REGION_DATA_GLSL
#define WEATHER_REGION_DATA_GLSL

layout(std140) uniform WeatherRegionData {
    float u_cloudCoverageNorth;
    vec3  u_cloudColorNorth;
    float u_cloudAltitudeNorth;

    float u_cloudCoverageNortheast;
    vec3  u_cloudColorNortheast;
    float u_cloudAltitudeNortheast;

    float u_cloudCoverageEast;
    vec3  u_cloudColorEast;
    float u_cloudAltitudeEast;

    float u_cloudCoverageSoutheast;
    vec3  u_cloudColorSoutheast;
    float u_cloudAltitudeSoutheast;

    float u_cloudCoverageSouth;
    vec3  u_cloudColorSouth;
    float u_cloudAltitudeSouth;

    float u_cloudCoverageSouthwest;
    vec3  u_cloudColorSouthwest;
    float u_cloudAltitudeSouthwest;

    float u_cloudCoverageWest;
    vec3  u_cloudColorWest;
    float u_cloudAltitudeWest;

    float u_cloudCoverageNorthwest;
    vec3  u_cloudColorNorthwest;
    float u_cloudAltitudeNorthwest;
};

#endif