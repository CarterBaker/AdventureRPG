#ifndef WIND_DATA_GLSL
#define WIND_DATA_GLSL

// Source: WindManager. u_windDirection/u_windSpeed mirror the grid's own
// live local wind (see LocalWindBranch), refreshed every frame right after
// LocalWindBranch resolves it.
//
// u_temperature is this grid's own current ambient temperature — resolved
// every frame from the grid's local weather and season by TemperatureSystem
// (see TemperatureInstance) and pushed here alongside wind, since both are
// "weather + season" derived, per-location atmospheric values. Held here so
// systems such as breath fog, frost overlays, or heat shimmer can read it
// without plumbing a new block.
//
// u_windFoliage is how thick the trees stand about the grid's active chunk, from none to one, so the wind tears leaves
// loose only where trees are (WindStreaks).
layout(std140) uniform WindData {
    vec3  u_windDirection;
    float u_windSpeed;
    float u_temperature;
    float u_windFoliage;
};

#endif
