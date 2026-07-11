#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL

#include "includes/TimeData.glsl"
#include "includes/WeatherData.glsl"
#include "includes/WeatherRegionData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/WindData.glsl"
#include "sky/util/CloudShapeUtility.glsl"

/*
* Sky-dome distant weather preview — v11 (corrected raymarch domain scale +
 * real wind drift; see the notes below). Builds on v10's relit,
 * height-shaped raymarch.
 *
 * What stayed the same
 * ---------------------
 * The 8-compass-direction + centre sampling (CompassSample /
 * _compassSample / _sampleHorizonWeather / sampleWeatherForSky), the fact
 * that every value read from WeatherData/WeatherRegionData has already
 * passed through RegionSampleBranch's own temporal smoothing, and the real
 * sun/moon-lit shading via shadeCloudLit() are all untouched.
 *
 * v11 — domain scale fix + real wind
 * -------------------------------------------------------------------
 * Two problems, both responsible for the sky dome reading as a warped
 * smear instead of distinct clouds:
 *
 * 1. Domain scale. The raymarch samples density at `dir * radius`, where
 *    `dir` sweeps the full unit sphere of view directions — so the entire
 *    visible sky dome's noise-space footprint is bounded by
 *    `radius * SKY_NOISE_SCALE`. The old radius (~22) gave a footprint of
 *    only ~3.5 noise-space units for the WHOLE SKY — a couple of noise
 *    cells, total — while SKY_WARP_STRENGTH (0.9) was then ~25% of that
 *    tiny span, meaning the domain warp dominated the base shape rather
 *    than subtly perturbing it. This is what read as "warped, not
 *    clouds." SKY_LAYER_SCALE (and proportionally SKY_LAYER_THICKNESS and
 *    SKY_LIGHT_TAP_DISTANCE) are raised ~8x here so the sky's noise
 *    footprint is comparable, in proportion, to how CloudVolumeShader.fsh
 *    samples its own physical box — the warp-to-base ratio is now the
 *    same on both layers.
 *
 * 2. Wind. This file previously faked its own scroll with a fixed
 *    time-driven diagonal vector, with a comment flagging it as a
 *    placeholder. It now reads the real WindData UBO — the exact same
 *    live wind driving OverheadManager's physical cloud drift — via a
 *    CPU-accumulated drift offset (see WindManager.advanceSkyDrift()), so
 *    the sky preview and the physical layer always agree on which way
 *    weather is moving.
 */

// ── Sky-dome/local blend ────────────────────────────────────────────────
const float CLOUD_DIR_Y_HARD_LIMIT = 0.62;

// ── Horizon seam fade ────────────────────────────────────────────────────
const float SKY_HORIZON_FADE_START = -0.02;
const float SKY_HORIZON_FADE_END   = 0.06;

// ── Raymarch tuning ──────────────────────────────────────────────────────
// SKY_LAYER_SCALE/THICKNESS/LIGHT_TAP_DISTANCE were all raised ~8x from
// their original (22.0 / 3.0 / 1.1) values, keeping their ratios to each
// other and to SKY_NOISE_SCALE fixed — see the class comment above.
const float SKY_LAYER_SCALE        = 180.0; // maps view direction into the density field's coordinate space
const float SKY_LAYER_THICKNESS    = 24.0;  // how far the raymarch travels through that space, in the same units as SKY_LAYER_SCALE
const int   SKY_RAYMARCH_STEPS     = 8;
const float SKY_NOISE_SCALE        = 0.16;
const float SKY_WARP_STRENGTH      = 0.9;
const float SKY_DETAIL_JITTER      = 0.35;
const float SKY_EDGE_SOFTNESS      = 0.12;
const float SKY_STEP_ALPHA_SCALE   = 0.65;
const float SKY_LIGHT_TAP_DISTANCE = 9.0;
const int   SKY_TOON_BANDS         = 3;
const float SKY_SHADE_STRENGTH     = 0.55;
const float SKY_RIM_STRENGTH       = 0.4;
const float SKY_AO_STRENGTH        = 0.22;
const float SKY_BRIGHTNESS         = 1.0;
const float SKY_TOP_TINT_MIX       = 0.35;
const float SKY_SHADOW_TINT_MIX    = 0.30;

// ── Weather Sampling ─────────────────────────────────────────────────────

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
    if (dir.y < SKY_HORIZON_FADE_START)
    return vec4(0.0);

    float horizonFade = smoothstep(SKY_HORIZON_FADE_START, SKY_HORIZON_FADE_END, dir.y);

    if (horizonFade <= 0.001)
    return vec4(0.0);

    float weatherCoverage;
    vec3  weatherColor;
    sampleWeatherForSky(dir, weatherCoverage, weatherColor);

    float sunWeight  = clamp(u_sunIntensity / 0.3, 0.0, 1.0);
    vec3  lightDir   = normalize(mix(u_moonDirection, u_sunDirection, sunWeight));
    vec3  lightColor = mix(u_moonColor, u_sunColor, sunWeight);
    float lightPower = mix(u_moonIntensity, u_sunIntensity, sunWeight);

    // Real wind drift — see the class comment. dailySeed still decorrelates
    // each day's cloud placement from the next even under identical wind.
    vec3 wind = vec3(
        u_windDriftOffset.x + dailySeed,
        0.0,
        u_windDriftOffset.y + dailySeed * 1.3);

    float stepDepth = SKY_LAYER_THICKNESS / float(SKY_RAYMARCH_STEPS);

    vec3 topTint    = mix(weatherColor, u_skyZenithColor, SKY_TOP_TINT_MIX);
    vec3 shadowTint = mix(weatherColor, u_skyHorizonColor * 0.4, SKY_SHADOW_TINT_MIX);

    float heightT = clamp(dir.y / mix(0.45, 1.0, clamp(weatherCoverage, 0.0, 1.0)), 0.0, 1.0);

    vec4 accum = vec4(0.0);

    for (int i = 0; i < SKY_RAYMARCH_STEPS; i++) {
        if (accum.a > 0.95)
        break;

        vec3 p = dir * (SKY_LAYER_SCALE + float(i) * stepDepth) + wind;

        float density = sampleCloudDensity(
            p, heightT, SKY_NOISE_SCALE, SKY_WARP_STRENGTH, SKY_DETAIL_JITTER,
            weatherCoverage, SKY_EDGE_SOFTNESS, dailySeed, u_time);

        if (density > 0.01) {
            float litDensity = sampleCloudDensity(
                p + lightDir * SKY_LIGHT_TAP_DISTANCE, heightT, SKY_NOISE_SCALE,
                SKY_WARP_STRENGTH, SKY_DETAIL_JITTER, weatherCoverage,
                SKY_EDGE_SOFTNESS, dailySeed, u_time);
            float lightLift = clamp((density - litDensity) * 2.0 + 0.5, 0.0, 1.0);

            vec3 shaded = shadeCloudLit(
                weatherColor, topTint, shadowTint, lightColor, lightPower,
                heightT, lightLift, density, SKY_TOON_BANDS,
                SKY_SHADE_STRENGTH, SKY_RIM_STRENGTH, SKY_AO_STRENGTH, SKY_BRIGHTNESS);

            float stepAlpha = clamp(density * SKY_STEP_ALPHA_SCALE, 0.0, 1.0);
            accum.rgb += (1.0 - accum.a) * stepAlpha * shaded;
            accum.a   += (1.0 - accum.a) * stepAlpha;
        }
    }

    accum.a *= horizonFade;

    return accum;
}

#endif