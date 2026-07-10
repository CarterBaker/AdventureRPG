#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL

#include "includes/TimeData.glsl"
#include "includes/WeatherData.glsl"
#include "includes/WeatherRegionData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "sky/util/CloudShapeUtility.glsl"

/*
* Sky-dome distant weather preview — v10 (relit + height-shaped raymarch;
 * see the Stage 1 note below). Builds on v9's procedural volumetric toon
 * raymarch (which itself replaced the old 2D circular-puff painter).
 *
 * What stayed the same
 * ---------------------
 * Everything about WHERE this file gets its weather data from is
 * untouched: the 8 compass-direction + centre sampling
 * (CompassSample / _compassSample / _sampleHorizonWeather /
 * sampleWeatherForSky), and the fact that every value read from
 * WeatherData/WeatherRegionData has already passed through
 * RegionSampleBranch's own temporal smoothing before it ever reaches
 * these uniforms — so a storm still visibly builds on the horizon in its
 * own direction, in the same colour and coverage RegionSampleBranch
 * already resolved for it, well before it reaches the player.
 *
 * Stage 1 — relit, height-shaped, and no longer artificially banded
 * --------------------------------------------------------------------
 * Three things changed here:
 *
 * 1. Real lighting. This file previously had no access to sun/moon data
 *    at all and shaded every cloud sample with a fixed toon ramp — no
 *    light direction, no light color, nothing but the resolved weather's
 *    own flat color. It now includes SunLightData/MoonLightData, blends
 *    them into one direction/color/intensity exactly like
 *    CloudVolumeShader.fsh already did for physical clouds, takes a
 *    self-shadow density tap toward that direction each step, and shades
 *    with shadeCloudLit() (CloudShapeUtility.glsl) instead of the old
 *    shadeCloudToon(). The lit/shadow tints are now blended toward the
 *    sky's own zenith/horizon color (SkyColorData) rather than flat
 *    white/black, so the sky dome's clouds read as lit BY this sky
 *    rather than floating in front of it.
 *
 * 2. Height-shaped density. sampleCloudDensity() (CloudShapeUtility.glsl)
 *    now takes a heightT parameter and uses it to both stretch the
 *    sampling domain (thin/streaky higher up, compact/puffy lower down)
 *    and apply a vertical density gradient (a comparatively sharp flat
 *    base, a softer eroding top). Here, heightT is derived from the view
 *    ray's own elevation (dir.y) against a coverage-dependent ceiling, so
 *    an overcast/stormy sky can genuinely read as a deck reaching toward
 *    the zenith while a fair-weather sky stays thin and wispy up there —
 *    a property of the weather, not a fixed constant.
 *
 * 3. No more artificial vertical band. The old version multiplied every
 *    result by a fixed "verticalEnvelope" that hard-capped cloud
 *    existence to a narrow slice of sky (roughly 6-35 degrees above the
 *    horizon) regardless of what the weather actually resolved to. That
 *    read as clouds being physically restricted to one band, which isn't
 *    how clouds work. The only fade left is a thin sliver right at true
 *    horizon level (SKY_HORIZON_FADE_START/END below), there purely to
 *    hide the seam where the sky dome meets the rendered terrain
 *    skyline — everything above that is free to show cloud, and how much
 *    actually appears is left entirely to sampleCloudDensity()'s own
 *    height gradient and the resolved weather's coverage. A tall storm
 *    deck can now genuinely reach toward the zenith; a clear day reads as
 *    clear all the way up. This is the "just how the math works" behavior
 *    — convergence toward the horizon falls out of the density function,
 *    not a manual cutoff.
 *
 * The view-ray raymarch samples a pseudo-world position derived purely
 * from the view direction (dir * distance-along-ray + a slow wind-driven
 * scroll) — the sky dome has no true depth or world position of its own,
 * so this mirrors the old system's own approach of treating `dir` itself
 * as the noise domain coordinate, just marched through in 3D now instead
 * of sampled once on a flat blob grid.
 */

// ── Sky-dome/local blend ────────────────────────────────────────────────
// Still used only by sampleWeatherForSky() below, to blend the 8-compass
// horizon read toward the player's own local weather sample as the view
// ray tilts up toward the zenith — nothing to do with cloud existence.
const float CLOUD_DIR_Y_HARD_LIMIT = 0.62;

// ── Horizon seam fade ────────────────────────────────────────────────────
// Purely cosmetic — hides the seam where the sky dome meets the rendered
// terrain skyline. NOT a cloud-existence cutoff; see the class comment.
const float SKY_HORIZON_FADE_START = -0.02;
const float SKY_HORIZON_FADE_END   = 0.06;

// ── Raymarch tuning ──────────────────────────────────────────────────────
const float SKY_LAYER_SCALE        = 22.0;  // maps view direction into the density field's coordinate space
const float SKY_LAYER_THICKNESS    = 3.0;   // how far the raymarch travels through that space, in the same units as SKY_LAYER_SCALE
const int   SKY_RAYMARCH_STEPS     = 8;
const float SKY_WIND_SPEED         = 0.0035;
const float SKY_NOISE_SCALE        = 0.16;
const float SKY_WARP_STRENGTH      = 0.9;
const float SKY_DETAIL_JITTER      = 0.35;
const float SKY_EDGE_SOFTNESS      = 0.12;
const float SKY_STEP_ALPHA_SCALE   = 0.65;
const float SKY_LIGHT_TAP_DISTANCE = 1.1;
const int   SKY_TOON_BANDS         = 3;
const float SKY_SHADE_STRENGTH     = 0.55;
const float SKY_RIM_STRENGTH       = 0.4;
const float SKY_AO_STRENGTH        = 0.22;
const float SKY_BRIGHTNESS         = 1.0;
const float SKY_TOP_TINT_MIX       = 0.35;
const float SKY_SHADOW_TINT_MIX    = 0.30;

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
    if (dir.y < SKY_HORIZON_FADE_START)
    return vec4(0.0);

    float horizonFade = smoothstep(SKY_HORIZON_FADE_START, SKY_HORIZON_FADE_END, dir.y);

    if (horizonFade <= 0.001)
    return vec4(0.0);

    float weatherCoverage;
    vec3  weatherColor;
    sampleWeatherForSky(dir, weatherCoverage, weatherColor);

    // Real light direction/color, blended sun->moon exactly like the
    // physical cloud objects (CloudVolumeShader.fsh) so the two stay
    // lit consistently with each other.
    float sunWeight  = clamp(u_sunIntensity / 0.3, 0.0, 1.0);
    vec3  lightDir   = normalize(mix(u_moonDirection, u_sunDirection, sunWeight));
    vec3  lightColor = mix(u_moonColor, u_sunColor, sunWeight);
    float lightPower = mix(u_moonIntensity, u_sunIntensity, sunWeight);

    // Slow scroll standing in for wind — a flat time-driven drift for
    // now; wiring this to the real WindHandle direction/speed (the same
    // vector OverheadManager/RegionSampleBranch already read) is a
    // natural follow-up once this pass is confirmed looking right.
    vec3 wind = vec3(
        u_time * SKY_WIND_SPEED + dailySeed,
        0.0,
        u_time * SKY_WIND_SPEED * 0.7 + dailySeed * 1.3);

    float stepDepth = SKY_LAYER_THICKNESS / float(SKY_RAYMARCH_STEPS);

    // Tinted toward this sky's own colors rather than flat white/black —
    // see the class comment's Stage 1 note.
    vec3 topTint    = mix(weatherColor, u_skyZenithColor, SKY_TOP_TINT_MIX);
    vec3 shadowTint = mix(weatherColor, u_skyHorizonColor * 0.4, SKY_SHADOW_TINT_MIX);

    // How high up the visible dome this ray looks, remapped against a
    // coverage-dependent ceiling — see heightGradient()'s own doc comment
    // in CloudShapeUtility.glsl. Higher coverage (a storm) pushes the
    // ceiling up, letting overcast weather genuinely reach toward the
    // zenith; clear/light weather stays thin well before it.
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