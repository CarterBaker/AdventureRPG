#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL

#include "includes/TimeData.glsl"
#include "includes/WeatherData.glsl"
#include "includes/WeatherRegionData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/WindData.glsl"
#include "sky/util/SkyCloudUtility.glsl"

/*
* Sky-dome distant weather preview — v13 (correct noise-domain scale; see
 * below). Builds on v12's full per-direction cloud archetype data.
 *
 * v13 — noise domain scale fix
 * -------------------------------------------------------------------
 * calculateClouds() previously built its raymarch position directly from
 * SKY_LAYER_SCALE (~180 units) — dir * (180ish) + wind — and fed that
 * straight into sampleSkyCloudDensity() as the coordinate, multiplied by
 * densityNoiseScale (0.6-1.4 in every shipped archetype). gradientNoise3D/
 * worleyNoise3D both key off ONE WORLD UNIT PER LATTICE CELL, so a full
 * sweep across the visible sky (dir ranging roughly -1 to 1 on each axis)
 * crossed 300-400+ noise lattice cells — far beyond anything these
 * functions can represent as a smooth billow. The result was
 * indistinguishable from static, no matter how densityNoiseScale or
 * noiseWarpStrength were tuned, since both are small multipliers that can
 * never compensate for a base domain three orders of magnitude too large.
 * The same aliasing also made the self-shadow tap (litDensity, sampled a
 * further SKY_LIGHT_TAP_DISTANCE toward the light) statistically no more
 * correlated with the base sample than a second, unrelated random pick —
 * lightLift was effectively random too, which is why the result also read
 * as flat and dark rather than showing a coherent lit/shadowed gradient.
 *
 * SKY_LAYER_SCALE/THICKNESS/LIGHT_TAP_DISTANCE are now expressed directly
 * in this shader's own small, sane noise-space domain — a noiseScale of
 * 1.0 now spans roughly the same number of billow cycles across the
 * visible sky that VolumetricCloudUtility.glsl's own per-cloud-relative
 * domain spans across one physical cloud (see that file's matching
 * "Domain scale fix" doc comment), so the sky-dome preview and physical
 * cloud objects read as the same weather simulation, not two differently
 * tuned art styles. SKY_NOISE_WORLD_SCALE carries the real, physically-
 * driven wind drift (u_windDriftOffset, accumulated in actual world-ish
 * units by WindManager) into this same compact domain, preserving its
 * proportion of the domain size — and therefore its "extremely slow"
 * apparent speed — rather than either vanishing (left scaled down alone)
 * or swamping the field (left at its old magnitude inside the new,
 * smaller domain).
 *
 * v12 — real per-direction cloud character
 * -------------------------------------------------------------------
 * Every earlier version of this file only ever varied TWO things by
 * direction: coverage and a single flat "cloudColor" tint. Toon banding,
 * shading strength, rim light, ambient occlusion, brightness, and the
 * noise parameters that actually shape a cloud's silhouette were all
 * fixed SKY_* constants, identical in every direction and under every
 * weather. That is why a Nimbus storm to the east and a Cumulus puff to
 * the north used to read as the same generic cloud blob with a different
 * paint job — nothing about their actual SHAPE or LIGHTING ever differed.
 *
 * WeatherRegionData (and WeatherData for the zenith) now carry the full
 * set of CloudData fields for whichever cloud archetype is primary in
 * each of the 8 sampled directions — see RegionSampleBranch.writeSample()
 * and WeatherBufferBranch. CompassSample below is resolved per-fragment
 * by blending across the compass sector (_sampleHorizonWeather) and then
 * again toward the zenith/center sample (sampleWeatherForSky) — the same
 * two-stage blend this file already did for coverage/color, just carried
 * through every field now. calculateClouds() feeds the fully-resolved,
 * per-fragment CompassSample into sampleSkyCloudDensity()/
 * shadeSkyCloudLit() (sky/util/SkyCloudUtility.glsl) instead of the fixed
 * SKY_TOON_BANDS/SKY_SHADE_STRENGTH/etc constants those used to hardcode.
 * This file's own remaining SKY_* constants are now only the raymarch's
 * OWN tuning (domain scale, step count, detail jitter, tint-mix ratios) —
 * nothing that describes what a cloud looks like lives here as a
 * constant any more.
 *
 * sky/util/SkyCloudUtility.glsl replaces the old shared
 * sky/util/CloudShapeUtility.glsl include — see that new file's own doc
 * comment for why a shared sky/physical shape function never worked well.
 * CloudShapeUtility.glsl itself is now dead code — nothing includes it
 * any more — and should be deleted.
 *
 * What stayed the same
 * ---------------------
 * The 8-compass-direction + centre sampling, the fact that every value
 * read from WeatherData/WeatherRegionData has already passed through
 * RegionSampleBranch's own temporal smoothing, and the real sun/moon-lit
 * shading via shadeSkyCloudLit() are all untouched in spirit — only now
 * every shading input is real, sampled, per-direction cloud data instead
 * of a fixed constant.
 *
 * cloudAltitude is carried all the way through WeatherData/
 * WeatherRegionData and into CompassSample's own source uniforms, but
 * deliberately not consumed by calculateClouds() yet — it's provisioned
 * here for the upcoming sky-to-physical-cloud transition stage, which
 * needs to know what altitude the sky preview implies so the physical
 * cloud layer can hand off from it with no visible seam.
 */

// ── Sky-dome/local blend ────────────────────────────────────────────────
const float CLOUD_DIR_Y_HARD_LIMIT = 0.62;

// ── Horizon seam fade ────────────────────────────────────────────────────
const float SKY_HORIZON_FADE_START = -0.02;
const float SKY_HORIZON_FADE_END   = 0.06;

// ── Raymarch domain tuning — NOT archetype data. Every value that
// describes what a cloud actually looks like (noise scale, warp, coverage
// bias, edge softness, toon bands, shading strengths, colors) comes from
// the resolved per-fragment CompassSample instead of a constant here.
//
// Expressed directly in noise-space units — see this file's own "v13 —
// noise domain scale fix" doc comment above for why these are small
// (~4, not the ~180-unit shell radius an earlier revision used) and for
// SKY_NOISE_WORLD_SCALE's role converting real wind drift into this same
// domain.
const float SKY_NOISE_WORLD_SCALE  = 1.0 / 45.0;
const float SKY_LAYER_SCALE        = 180.0 * SKY_NOISE_WORLD_SCALE;
const float SKY_LAYER_THICKNESS    = 24.0  * SKY_NOISE_WORLD_SCALE;
const int   SKY_RAYMARCH_STEPS     = 8;
const float SKY_DETAIL_JITTER      = 0.35;
const float SKY_STEP_ALPHA_SCALE   = 0.65;
const float SKY_LIGHT_TAP_DISTANCE = 9.0   * SKY_NOISE_WORLD_SCALE;
const float SKY_TOP_TINT_MIX       = 0.18;
const float SKY_SHADOW_TINT_MIX    = 0.15;

// ── Weather Sampling ─────────────────────────────────────────────────────

struct CompassSample {
    float coverage;
    vec3  color;
    vec3  topColor;
    vec3  shadowColor;
    float density;
    float shadeStrength;
    float rimStrength;
    float aoStrength;
    float brightness;
    float toonBands;
    float densityNoiseScale;
    float noiseWarpStrength;
    float coverageBias;
    float silhouetteSoftness;
};

CompassSample _compassSample(int index) {
    if (index == 0) return CompassSample(
        u_cloudCoverageNorth, u_cloudColorNorth, u_cloudTopColorNorth, u_cloudShadowColorNorth,
        u_cloudDensityNorth, u_cloudShadeStrengthNorth, u_cloudRimLightStrengthNorth,
        u_cloudAmbientOcclusionStrengthNorth, u_cloudBrightnessMultiplierNorth, u_cloudToonBandsNorth,
        u_cloudDensityNoiseScaleNorth, u_cloudNoiseWarpStrengthNorth, u_cloudCoverageBiasNorth,
        u_cloudSilhouetteSoftnessNorth);
    if (index == 1) return CompassSample(
        u_cloudCoverageNortheast, u_cloudColorNortheast, u_cloudTopColorNortheast, u_cloudShadowColorNortheast,
        u_cloudDensityNortheast, u_cloudShadeStrengthNortheast, u_cloudRimLightStrengthNortheast,
        u_cloudAmbientOcclusionStrengthNortheast, u_cloudBrightnessMultiplierNortheast, u_cloudToonBandsNortheast,
        u_cloudDensityNoiseScaleNortheast, u_cloudNoiseWarpStrengthNortheast, u_cloudCoverageBiasNortheast,
        u_cloudSilhouetteSoftnessNortheast);
    if (index == 2) return CompassSample(
        u_cloudCoverageEast, u_cloudColorEast, u_cloudTopColorEast, u_cloudShadowColorEast,
        u_cloudDensityEast, u_cloudShadeStrengthEast, u_cloudRimLightStrengthEast,
        u_cloudAmbientOcclusionStrengthEast, u_cloudBrightnessMultiplierEast, u_cloudToonBandsEast,
        u_cloudDensityNoiseScaleEast, u_cloudNoiseWarpStrengthEast, u_cloudCoverageBiasEast,
        u_cloudSilhouetteSoftnessEast);
    if (index == 3) return CompassSample(
        u_cloudCoverageSoutheast, u_cloudColorSoutheast, u_cloudTopColorSoutheast, u_cloudShadowColorSoutheast,
        u_cloudDensitySoutheast, u_cloudShadeStrengthSoutheast, u_cloudRimLightStrengthSoutheast,
        u_cloudAmbientOcclusionStrengthSoutheast, u_cloudBrightnessMultiplierSoutheast, u_cloudToonBandsSoutheast,
        u_cloudDensityNoiseScaleSoutheast, u_cloudNoiseWarpStrengthSoutheast, u_cloudCoverageBiasSoutheast,
        u_cloudSilhouetteSoftnessSoutheast);
    if (index == 4) return CompassSample(
        u_cloudCoverageSouth, u_cloudColorSouth, u_cloudTopColorSouth, u_cloudShadowColorSouth,
        u_cloudDensitySouth, u_cloudShadeStrengthSouth, u_cloudRimLightStrengthSouth,
        u_cloudAmbientOcclusionStrengthSouth, u_cloudBrightnessMultiplierSouth, u_cloudToonBandsSouth,
        u_cloudDensityNoiseScaleSouth, u_cloudNoiseWarpStrengthSouth, u_cloudCoverageBiasSouth,
        u_cloudSilhouetteSoftnessSouth);
    if (index == 5) return CompassSample(
        u_cloudCoverageSouthwest, u_cloudColorSouthwest, u_cloudTopColorSouthwest, u_cloudShadowColorSouthwest,
        u_cloudDensitySouthwest, u_cloudShadeStrengthSouthwest, u_cloudRimLightStrengthSouthwest,
        u_cloudAmbientOcclusionStrengthSouthwest, u_cloudBrightnessMultiplierSouthwest, u_cloudToonBandsSouthwest,
        u_cloudDensityNoiseScaleSouthwest, u_cloudNoiseWarpStrengthSouthwest, u_cloudCoverageBiasSouthwest,
        u_cloudSilhouetteSoftnessSouthwest);
    if (index == 6) return CompassSample(
        u_cloudCoverageWest, u_cloudColorWest, u_cloudTopColorWest, u_cloudShadowColorWest,
        u_cloudDensityWest, u_cloudShadeStrengthWest, u_cloudRimLightStrengthWest,
        u_cloudAmbientOcclusionStrengthWest, u_cloudBrightnessMultiplierWest, u_cloudToonBandsWest,
        u_cloudDensityNoiseScaleWest, u_cloudNoiseWarpStrengthWest, u_cloudCoverageBiasWest,
        u_cloudSilhouetteSoftnessWest);
    return CompassSample(
        u_cloudCoverageNorthwest, u_cloudColorNorthwest, u_cloudTopColorNorthwest, u_cloudShadowColorNorthwest,
        u_cloudDensityNorthwest, u_cloudShadeStrengthNorthwest, u_cloudRimLightStrengthNorthwest,
        u_cloudAmbientOcclusionStrengthNorthwest, u_cloudBrightnessMultiplierNorthwest, u_cloudToonBandsNorthwest,
        u_cloudDensityNoiseScaleNorthwest, u_cloudNoiseWarpStrengthNorthwest, u_cloudCoverageBiasNorthwest,
        u_cloudSilhouetteSoftnessNorthwest);
}

CompassSample _mixCompass(CompassSample a, CompassSample b, float t) {
    CompassSample result;
    result.coverage           = mix(a.coverage, b.coverage, t);
    result.color              = mix(a.color, b.color, t);
    result.topColor           = mix(a.topColor, b.topColor, t);
    result.shadowColor        = mix(a.shadowColor, b.shadowColor, t);
    result.density            = mix(a.density, b.density, t);
    result.shadeStrength      = mix(a.shadeStrength, b.shadeStrength, t);
    result.rimStrength        = mix(a.rimStrength, b.rimStrength, t);
    result.aoStrength         = mix(a.aoStrength, b.aoStrength, t);
    result.brightness         = mix(a.brightness, b.brightness, t);
    result.toonBands          = mix(a.toonBands, b.toonBands, t);
    result.densityNoiseScale  = mix(a.densityNoiseScale, b.densityNoiseScale, t);
    result.noiseWarpStrength  = mix(a.noiseWarpStrength, b.noiseWarpStrength, t);
    result.coverageBias       = mix(a.coverageBias, b.coverageBias, t);
    result.silhouetteSoftness = mix(a.silhouetteSoftness, b.silhouetteSoftness, t);
    return result;
}

CompassSample _sampleHorizonWeather(vec3 dir) {
    float headingDeg = degrees(atan(dir.x, -dir.z));
    headingDeg = mod(headingDeg + 360.0, 360.0);

    float sector = headingDeg / 45.0;
    int i0 = int(floor(sector)) % 8;
    int i1 = (i0 + 1) % 8;
    float t = smoothstep(0.0, 1.0, fract(sector));

    return _mixCompass(_compassSample(i0), _compassSample(i1), t);
}

CompassSample sampleWeatherForSky(vec3 dir) {
    CompassSample horizon = _sampleHorizonWeather(dir);

    CompassSample zenith;
    zenith.coverage           = u_cloudCoverage;
    zenith.color              = u_cloudColor;
    zenith.topColor           = u_cloudTopColor;
    zenith.shadowColor        = u_cloudShadowColor;
    zenith.density            = u_cloudDensity;
    zenith.shadeStrength      = u_cloudShadeStrength;
    zenith.rimStrength        = u_cloudRimLightStrength;
    zenith.aoStrength         = u_cloudAmbientOcclusionStrength;
    zenith.brightness         = u_cloudBrightnessMultiplier;
    zenith.toonBands          = u_cloudToonBands;
    zenith.densityNoiseScale  = u_cloudDensityNoiseScale;
    zenith.noiseWarpStrength  = u_cloudNoiseWarpStrength;
    zenith.coverageBias       = u_cloudCoverageBias;
    zenith.silhouetteSoftness = u_cloudSilhouetteSoftness;

    float zenithT = clamp(dir.y / CLOUD_DIR_Y_HARD_LIMIT, 0.0, 1.0);

    return _mixCompass(horizon, zenith, zenithT);
}

// ── Public API ────────────────────────────────────────────────────────────

vec4 calculateClouds(vec3 dir, float dailySeed) {
    if (dir.y < SKY_HORIZON_FADE_START)
    return vec4(0.0);

    float horizonFade = smoothstep(SKY_HORIZON_FADE_START, SKY_HORIZON_FADE_END, dir.y);

    if (horizonFade <= 0.001)
    return vec4(0.0);

    CompassSample weather = sampleWeatherForSky(dir);

    float sunWeight  = clamp(u_sunIntensity / 0.3, 0.0, 1.0);
    vec3  lightDir   = normalize(mix(u_moonDirection, u_sunDirection, sunWeight));
    vec3  lightColor = mix(u_moonColor, u_sunColor, sunWeight);
    float lightPower = mix(u_moonIntensity, u_sunIntensity, sunWeight);

    // Real wind drift, brought into this shader's own compact noise domain
    // by SKY_NOISE_WORLD_SCALE — see the class comment's "v13" section.
    // dailySeed still decorrelates each day's cloud placement from the
    // next even under identical wind — its own, much larger contribution
    // inside sampleSkyCloudDensity's seedOffset (seed * 173.13 etc.) does
    // that regardless of this vector's magnitude, so no separate scale is
    // needed for it here.
    vec3 wind = vec3(
        u_windDriftOffset.x + dailySeed,
        0.0,
        u_windDriftOffset.y + dailySeed * 1.3) * SKY_NOISE_WORLD_SCALE;

    float stepDepth = SKY_LAYER_THICKNESS / float(SKY_RAYMARCH_STEPS);

    // Real per-direction archetype tints — topColor/shadowColor already
    // ARE this direction's actual resolved cloud archetype colors (see the
    // class comment). The small zenith-color nudge keeps distant cloud
    // tint agreeing with the sky's own atmospheric colour instead of
    // reading as pasted on top of it.
    vec3 topTint    = mix(weather.topColor, u_skyZenithColor, SKY_TOP_TINT_MIX);
    vec3 shadowTint = mix(weather.shadowColor, u_skyHorizonColor * 0.4, SKY_SHADOW_TINT_MIX);

    // Blends the live simulated weather coverage (how much sky this
    // direction's storm currently occupies, changes continuously) with
    // this direction's own resolved cloud archetype's coverageBias (how
    // dense THAT TYPE of cloud inherently reads at a given coverage) — see
    // RegionSampleBranch/CloudData's own coverageBias field. Neither alone
    // is enough: coverage alone made every archetype look equally solid at
    // the same reading; coverageBias alone never responded to the weather
    // actually changing.
    float effectiveCoverageBias = clamp(mix(weather.coverageBias, weather.coverage, 0.5), 0.0, 1.0);

    float heightT = clamp(dir.y / mix(0.45, 1.0, clamp(weather.coverage, 0.0, 1.0)), 0.0, 1.0);

    int toonBands = max(int(weather.toonBands + 0.5), 1);

    vec4 accum = vec4(0.0);

    for (int i = 0; i < SKY_RAYMARCH_STEPS; i++) {
        if (accum.a > 0.95)
        break;

        vec3 p = dir * (SKY_LAYER_SCALE + float(i) * stepDepth) + wind;

        float density = sampleSkyCloudDensity(
            p, heightT, weather.densityNoiseScale, weather.noiseWarpStrength, SKY_DETAIL_JITTER,
            effectiveCoverageBias, weather.silhouetteSoftness, dailySeed, u_time) * weather.density;

        if (density > 0.01) {
            float litDensity = sampleSkyCloudDensity(
                p + lightDir * SKY_LIGHT_TAP_DISTANCE, heightT, weather.densityNoiseScale,
                weather.noiseWarpStrength, SKY_DETAIL_JITTER, effectiveCoverageBias,
                weather.silhouetteSoftness, dailySeed, u_time) * weather.density;
            float lightLift = clamp((density - litDensity) * 2.0 + 0.5, 0.0, 1.0);

            vec3 shaded = shadeSkyCloudLit(
                weather.color, topTint, shadowTint, lightColor, lightPower,
                heightT, lightLift, density, toonBands,
                weather.shadeStrength, weather.rimStrength, weather.aoStrength, weather.brightness);

            float stepAlpha = clamp(density * SKY_STEP_ALPHA_SCALE, 0.0, 1.0);
            accum.rgb += (1.0 - accum.a) * stepAlpha * shaded;
            accum.a   += (1.0 - accum.a) * stepAlpha;
        }
    }

    accum.a *= horizonFade;

    return accum;
}

#endif