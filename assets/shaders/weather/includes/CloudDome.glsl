#ifndef CLOUD_DOME_GLSL
#define CLOUD_DOME_GLSL

#include "includes/WeatherMapData.glsl"

// Bends a cloud entry's authored altitude down toward the world's fixed
// sea level as ITS OWN real distance from the reference chunk grows —
// never the angle it happens to be viewed at. A pattern sitting genuinely
// close to the player keeps its true altitude no matter which part of its
// own footprint a ray happens to graze; a pattern that has actually
// drifted far away sinks toward sea level even glanced at nearly
// overhead, matching how a real distant cloud reads low against the
// horizon regardless of the exact angle it's glimpsed from. Distance is
// normalized against u_weatherRangeBlocks (see WeatherMapData.glsl) — the
// same terrain-independent range the CPU streams patterns across — so the
// dome always spans exactly as far as weather itself is simulated,
// independent of the much smaller terrain streaming radius.

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