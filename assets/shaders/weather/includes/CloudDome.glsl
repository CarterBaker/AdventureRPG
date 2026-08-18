#ifndef CLOUD_DOME_GLSL
#define CLOUD_DOME_GLSL

#include "includes/WeatherMapData.glsl"

// Bends an authored cloud altitude down toward the world's fixed sea
// level as the true horizontal distance passed in grows — the caller
// resolves that distance per fragment (see WeatherShader.fsh's
// resolveCloudSlab), never from a single CPU-side value for the whole
// weather pattern, so the same pattern's clouds sit at their real
// elevation near the camera and sink toward sea level at the horizon
// continuously across their own footprint instead of as one flat bent
// slab. Distance is normalized against u_weatherRangeBlocks (see
// WeatherMapData.glsl) — the same terrain-independent range the CPU
// streams patterns across — so the dome always spans exactly as far as
// weather itself is simulated, independent of the much smaller terrain
// streaming radius.

// Must mirror EngineSetting.WEATHER_SEA_LEVEL_BLOCKS — GLSL has no
// visibility into the Java constant, the same convention WaterShader.vsh
// already uses for its own LIQUID_LEVEL_MAX mirror.
const float CLOUD_DOME_SEA_LEVEL_BLOCKS = 512.0;
const float CLOUD_DOME_CURVE_POWER      = 1.6;

float resolveCloudDomeAltitude(float authoredAltitude, float distanceBlocks) {
    float distanceT = clamp(distanceBlocks / max(u_weatherRangeBlocks, 1.0), 0.0, 1.0);
    float bend       = pow(distanceT, CLOUD_DOME_CURVE_POWER);
    return mix(authoredAltitude, CLOUD_DOME_SEA_LEVEL_BLOCKS, bend);
}

#endif