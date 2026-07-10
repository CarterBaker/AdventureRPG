#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL

#include "includes/TimeData.glsl"
#include "includes/WeatherData.glsl"
#include "includes/WeatherRegionData.glsl"
#include "sky/util/CloudShapeUtility.glsl"

/*
* Sky-dome distant weather preview — v9 (procedural volumetric toon
 * raymarch, replaces the old 2D circular-puff painter entirely).
 *
 * What stayed the same
 * ---------------------
 * Everything about WHERE this file gets its weather data from is
 * untouched: the 8 compass-direction + centre sampling
 * (CompassSample / _compassSample / _sampleHorizonWeather /
 * sampleWeatherForSky), and the fact that every value read from
 * WeatherData/WeatherRegionData has already passed through
 * RegionSampleBranch's own temporal smoothing before it ever reaches
 * these uniforms (see EngineSetting.WEATHER_SAMPLE_SMOOTHING_TIME_SECONDS's
 * own doc comment) — so a storm still visibly builds on the horizon in
 * its own direction, in the same colour and coverage RegionSampleBranch
 * already resolved for it, well before it reaches the player. None of
 * that sourcing logic needed to change; only what a "cloud" actually
 * looks like once resolved did.
 *
 * What changed
 * -------------
 * The old system placed discrete circular "puffs" on a coarse blob grid
 * and painted them with a fixed ring/core two-tone gradient — cheap, but
 * it reads as exactly what it is: flat, tiled, unmistakably geometric
 * circles. This version instead raymarches the view ray through a
 * shared, genuinely 3D procedural density field (see
 * sky/util/CloudShapeUtility.glsl) and shades it with height-driven toon
 * bands, front-to-back alpha compositing every step like a real
 * volumetric pass. Since sampleCloudDensity()/shadeCloudToon() are the
 * exact same functions the physical cloud objects
 * (clouds/CloudVolumeShader.fsh) will raymarch with once that shader is
 * reworked too, the sky preview and the real cloud objects a player
 * eventually flies into are guaranteed to look like the same weather,
 * not two independently-tuned systems that happen to sit near each
 * other.
 *
 * There is also no more discrete per-cell "existence" boundary to cross
 * — density is a continuous function of the live, already-smoothed
 * coverage value at every single sample, so the old flicker-avoidance
 * mechanism (BLOB_PRESENCE_FADE_RANGE) is no longer needed at all; there
 * is nothing discrete left to fade the existence of.
 *
 * The view-ray raymarch samples a pseudo-world position derived purely
 * from the view direction (dir * distance-along-ray + a slow wind-driven
 * scroll) — the sky dome has no true depth or world position of its own,
 * so this mirrors the old system's own approach of treating `dir` itself
 * as the noise domain coordinate, just marched through in 3D now instead
 * of sampled once on a flat blob grid.
 */

// ── Vertical placement ──────────────────────────────────────────────────
// The sky-dome layer occupies a soft vertical band rather than the whole
// hemisphere — clouds fade in a little above the horizon and fade back
// out well before the zenith. CLOUD_DIR_Y_HARD_LIMIT is also the point
// sampleWeatherForSky() blends fully into the player's own local weather
// sample (see that function) — kept identical to the original value so
// that blend behaviour is untouched.
const float CLOUD_DIR_Y_HARD_LIMIT = 0.62;

// ── Raymarch tuning ──────────────────────────────────────────────────────
const float SKY_LAYER_SCALE      = 22.0;  // maps view direction into the density field's coordinate space
const float SKY_LAYER_THICKNESS  = 3.0;   // how far the raymarch travels through that space, in the same units as SKY_LAYER_SCALE
const int   SKY_RAYMARCH_STEPS   = 8;
const float SKY_WIND_SPEED       = 0.0035;
const float SKY_NOISE_SCALE      = 0.16;
const float SKY_WARP_STRENGTH    = 0.9;
const float SKY_DETAIL_JITTER    = 0.35;
const float SKY_EDGE_SOFTNESS    = 0.12;
const float SKY_STEP_ALPHA_SCALE = 0.65;
const int   SKY_TOON_BANDS       = 3;
const float SKY_SHADE_STRENGTH   = 0.55;
const float SKY_AO_STRENGTH      = 0.22;
const float SKY_BRIGHTNESS       = 1.0;
const float SKY_TOP_TINT_MIX     = 0.35;
const float SKY_SHADOW_TINT_MIX  = 0.30;

// ── Weather Sampling ─────────────────────────────────────────────────────
// Unchanged from the original file — see the header comment above.

struct CompassSample {
    float coverage;
    vec3  color;
};

CompassSample _compassSample(int index) {
    if (index == 0) return CompassSample(u_cloudCoverageNorth,     u_cloudColorNorth);
    if (index == 1) return CompassSample(u_cloudCoverageNortheast, u_cloudColorNortheast);
    if (index == 2) return CompassSample(u_cloudCoverageEast,      u_cloudColorEast);
    if (index == 3) return CompassSample(u_cloudCoverageSoutheast, u_cloudColorSoutheast);
    if (index == 4) return CompassSample(u_cloudCoverageSouth,     u_cloudColorSouth);
    if (index == 5) return CompassSample(u_cloudCoverageSouthwest, u_cloudColorSouthwest);
    if (index == 6) return CompassSample(u_cloudCoverageWest,      u_cloudColorWest);
    return CompassSample(u_cloudCoverageNorthwest, u_cloudColorNorthwest);
}

// Blends the two nearest of the 8 compass samples by the view ray's
// horizontal heading. North = -Z, east = +X, south = +Z, west = -X —
// matches Direction2Vector / RegionSampleBranch's own sampling axes.
void _sampleHorizonWeather(vec3 dir, out float coverage, out vec3 color) {
    float headingDeg = degrees(atan(dir.x, -dir.z));
    headingDeg = mod(headingDeg + 360.0, 360.0);

    float sector = headingDeg / 45.0;
    int i0 = int(floor(sector)) % 8;
    int i1 = (i0 + 1) % 8;
    float t = smoothstep(0.0, 1.0, fract(sector));

    CompassSample a = _compassSample(i0);
    CompassSample b = _compassSample(i1);

    coverage = mix(a.coverage, b.coverage, t);
    color    = mix(a.color, b.color, t);
}

// Public: resolves the coverage and colour the sky's cloud layer should
// use for a given view ray, fading from the horizon blend above toward
// the player's own local WeatherData sample as dir.y rises toward
// CLOUD_DIR_Y_HARD_LIMIT.
void sampleWeatherForSky(vec3 dir, out float coverage, out vec3 color) {
    float horizonCoverage;
    vec3  horizonColor;
    _sampleHorizonWeather(dir, horizonCoverage, horizonColor);

    float zenithT = clamp(dir.y / CLOUD_DIR_Y_HARD_LIMIT, 0.0, 1.0);

    coverage = mix(horizonCoverage, u_cloudCoverage, zenithT);
    color    = mix(horizonColor, u_cloudColor, zenithT);
}

// ── Public API ────────────────────────────────────────────────────────────

vec4 calculateClouds(vec3 dir, float dailySeed) {
    if (dir.y < 0.0 || dir.y > CLOUD_DIR_Y_HARD_LIMIT)
    return vec4(0.0);

    float weatherCoverage;
    vec3  weatherColor;
    sampleWeatherForSky(dir, weatherCoverage, weatherColor);

    // Soft vertical window the layer is visible within — fades in just
    // above the horizon, fades back out well before the zenith. Never a
    // hard cutoff, so nothing pops as the player looks up or down.
    float verticalEnvelope = smoothstep(-0.02, 0.10, dir.y)
    * (1.0 - smoothstep(0.30, CLOUD_DIR_Y_HARD_LIMIT, dir.y));

    if (verticalEnvelope <= 0.001)
    return vec4(0.0);

    // Slow scroll standing in for wind — a flat time-driven drift for
    // now; wiring this to the real WindHandle direction/speed (the same
    // vector OverheadManager/RegionSampleBranch already read) is a
    // natural follow-up once this pass is confirmed looking right.
    vec3 wind = vec3(
        u_time * SKY_WIND_SPEED + dailySeed,
        0.0,
        u_time * SKY_WIND_SPEED * 0.7 + dailySeed * 1.3);

    float stepDepth  = SKY_LAYER_THICKNESS / float(SKY_RAYMARCH_STEPS);
    vec3  topTint    = mix(weatherColor, vec3(1.0), SKY_TOP_TINT_MIX);
    vec3  shadowTint = mix(weatherColor, vec3(0.0), SKY_SHADOW_TINT_MIX);

    vec4 accum = vec4(0.0);

    for (int i = 0; i < SKY_RAYMARCH_STEPS; i++) {
        if (accum.a > 0.95)
        break;

        float layerT = float(i) / float(max(SKY_RAYMARCH_STEPS - 1, 1));
        vec3  p       = dir * (SKY_LAYER_SCALE + float(i) * stepDepth) + wind;

        float density = sampleCloudDensity(
            p, SKY_NOISE_SCALE, SKY_WARP_STRENGTH, SKY_DETAIL_JITTER,
            weatherCoverage, SKY_EDGE_SOFTNESS, dailySeed, u_time);

        if (density > 0.01) {
            vec3 shaded = shadeCloudToon(
                weatherColor, topTint, shadowTint,
                layerT, density, SKY_TOON_BANDS,
                SKY_SHADE_STRENGTH, SKY_AO_STRENGTH, SKY_BRIGHTNESS);

            float stepAlpha = clamp(density * SKY_STEP_ALPHA_SCALE, 0.0, 1.0);
            accum.rgb += (1.0 - accum.a) * stepAlpha * shaded;
            accum.a   += (1.0 - accum.a) * stepAlpha;
        }
    }

    accum.a *= verticalEnvelope;

    return accum;
}

#endif