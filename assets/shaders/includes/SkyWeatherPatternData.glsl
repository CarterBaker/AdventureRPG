#ifndef SKY_WEATHER_PATTERN_DATA_GLSL
#define SKY_WEATHER_PATTERN_DATA_GLSL

#define SKY_WEATHER_PATTERN_MAX_COUNT 64

layout(std140) uniform SkyWeatherPatternData {
    int   u_patternCount;
    float u_patternBearing[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternElevation[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternAngularWidth[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternAngularHeight[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternFadeAlpha[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternIntensity[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternCoverage[SKY_WEATHER_PATTERN_MAX_COUNT];
    vec3  u_patternColor[SKY_WEATHER_PATTERN_MAX_COUNT];
    vec3  u_patternTopColor[SKY_WEATHER_PATTERN_MAX_COUNT];
    vec3  u_patternShadowColor[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternDensity[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternShadeStrength[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternRimLightStrength[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternAmbientOcclusionStrength[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternBrightnessMultiplier[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternToonBands[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternDensityNoiseScale[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternNoiseWarpStrength[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternCoverageBias[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_patternSilhouetteSoftness[SKY_WEATHER_PATTERN_MAX_COUNT];
    float u_skyElevationLimit;
};

#endif