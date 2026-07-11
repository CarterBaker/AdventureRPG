#ifndef WIND_DATA_GLSL
#define WIND_DATA_GLSL

// Source: WindManager. u_windDirection/u_windSpeed mirror WindHandle's own
// live local wind (see LocalWindBranch), refreshed every frame right after
// LocalWindBranch resolves it. u_windDriftOffset is a continuously
// accumulated (x, z) position drift — see WindManager.advanceSkyDrift() —
// integrated incrementally every frame exactly like RegionSampleBranch and
// OverheadManager already drift their own noise/cell positions, rather than
// a raw direction*speed*time product, so a sudden wind change never
// retroactively scrolls the whole session's worth of history in one visible
// pop. Consumed by the sky dome's distant cloud preview (see
// sky/util/Clouds.glsl) so its drift genuinely matches the same wind
// blowing the physical overhead cloud layer instead of a fixed synthetic
// scroll.
layout(std140) uniform WindData {
    vec3  u_windDirection;
    float u_windSpeed;
    vec2  u_windDriftOffset;
};

#endif