#ifndef WEATHER_MAP_UTILITY_GLSL
#define WEATHER_MAP_UTILITY_GLSL

#include "includes/WeatherMapData.glsl"
#include "includes/NoiseUtility.glsl"

/*
 * Shared reads of a grid's weather window and its cloud layers, so the sky
 * pass and terrain cloud shadows see exactly the same clouds. The window is
 * sampled bilinearly between cell centres, which is what lets the scrolling
 * weather image glide smoothly instead of stepping cell by cell. A layer's
 * density is its archetype's periodic shape field, cut by the local
 * coverage, shaped by a vertical envelope, and eroded toward the crown in
 * proportion to fullness, so puffy archetypes grow rounded domes while
 * sheets stay flat slabs.
 */

const float WEATHER_MAP_EPSILON           = 0.001;
const float WEATHER_MAP_EDGE_MARGIN_CELLS = 1.5;

const float CLOUD_LAYER_WARP_AMPLITUDE     = 0.6;
const float CLOUD_LAYER_FIELD_CONTRAST     = 2.2;
const float CLOUD_LAYER_BILLOW_FLOOR       = 0.45;
const float CLOUD_LAYER_LUMP_MIN           = 0.72;
const float CLOUD_LAYER_LUMP_MAX           = 1.18;
const float CLOUD_LAYER_DETAIL_STRENGTH    = 0.28;
const float CLOUD_LAYER_COVERAGE_BIAS_BASE = 0.5;
const float CLOUD_LAYER_CROWN_EROSION      = 0.85;
const float CLOUD_LAYER_MIN_SOFTNESS       = 0.02;
const float CLOUD_LAYER_SHEET_BASE_RAMP    = 0.30;
const float CLOUD_LAYER_PUFFY_BASE_RAMP    = 0.05;
const float CLOUD_LAYER_SHEET_TOP_START    = 0.35;
const float CLOUD_LAYER_PUFFY_TOP_START    = 0.60;

const uint CLOUD_LAYER_SEED_STRIDE = 7919u;
const uint CLOUD_LAYER_WARP_SEED_X = 131u;
const uint CLOUD_LAYER_WARP_SEED_Z = 257u;
const uint CLOUD_LAYER_DETAIL_SEED = 521u;

// ── Weather Window ─────────────────────────────────────────────────────────

float unpackWeatherChannel(int packed, int layer) {
    int shift = (layer % WEATHER_MAP_LAYERS_PER_COMPONENT) * 8;
    return float((packed >> shift) & 255) / WEATHER_MAP_CHANNEL_MAX;
}

// x = coverage, y = density scale of one layer in one cell.
vec2 readWeatherCell(ivec2 cell, int layer) {
    ivec4 packed = u_weatherCells[cell.y * WEATHER_MAP_RESOLUTION + cell.x];
    bool  upper  = layer >= WEATHER_MAP_LAYERS_PER_COMPONENT;

    return vec2(
        unpackWeatherChannel(upper ? packed.y : packed.x, layer),
        unpackWeatherChannel(upper ? packed.w : packed.z, layer) * WEATHER_MAP_DENSITY_SCALE_MAX);
}

// Density is weighted by coverage, so an empty neighbour never drags a
// cloud's opacity toward zero at the edge of a weather.
vec2 sampleWeatherLayer(int layer, vec2 positionXZ) {
    vec2  mapPosition = (positionXZ + u_weatherMapOrigin.xy) / u_weatherMapOrigin.z - 0.5;
    vec2  base        = floor(mapPosition);
    vec2  f           = mapPosition - base;
    ivec2 limit       = ivec2(WEATHER_MAP_RESOLUTION - 1);
    ivec2 i0          = clamp(ivec2(base), ivec2(0), limit);
    ivec2 i1          = clamp(ivec2(base) + 1, ivec2(0), limit);

    vec2 c00 = readWeatherCell(ivec2(i0.x, i0.y), layer);
    vec2 c10 = readWeatherCell(ivec2(i1.x, i0.y), layer);
    vec2 c01 = readWeatherCell(ivec2(i0.x, i1.y), layer);
    vec2 c11 = readWeatherCell(ivec2(i1.x, i1.y), layer);

    vec4 weights = vec4(
        (1.0 - f.x) * (1.0 - f.y),
        f.x * (1.0 - f.y),
        (1.0 - f.x) * f.y,
        f.x * f.y);

    vec4  coverages = vec4(c00.x, c10.x, c01.x, c11.x);
    vec4  densities = vec4(c00.y, c10.y, c01.y, c11.y);
    vec4  weighted  = weights * coverages;
    float coverage  = weighted.x + weighted.y + weighted.z + weighted.w;

    return vec2(coverage, dot(weighted, densities) / max(coverage, WEATHER_MAP_EPSILON));
}

// Horizontal distance from the reference chunk the window can still be
// sampled at with all four surrounding cells inside it.
float resolveWeatherMapReach() {
    return (float(WEATHER_MAP_RESOLUTION) * 0.5 - WEATHER_MAP_EDGE_MARGIN_CELLS) * u_weatherMapOrigin.z;
}

float resolveWeatherMapEdgeFade(vec2 positionXZ) {
    return 1.0 - smoothstep(u_weatherMapOrigin.w, resolveWeatherMapReach(), length(positionXZ));
}

// ── Cloud Layer Shape ──────────────────────────────────────────────────────

float resolveCloudLayerFeatureSize(int layer) {
    return u_weatherShapePeriod / max(u_weatherLayerNoise[layer].y, 1.0);
}

float sampleCloudLayerShape(int layer, vec2 positionXZ, int octaves, float detailFade) {
    vec4  noiseParams = u_weatherLayerNoise[layer];
    vec4  surface     = u_weatherLayerSurface[layer];
    float fullness    = u_weatherLayerShape[layer].w;

    vec2 lattice = max(noiseParams.xy, vec2(1.0));
    vec2 p       = (surface.xy + positionXZ) / u_weatherShapePeriod * lattice;
    uint seed    = uint(layer) * CLOUD_LAYER_SEED_STRIDE;

    vec2 warp = vec2(
        periodicGradientNoise2D(p, lattice, seed + CLOUD_LAYER_WARP_SEED_X),
        periodicGradientNoise2D(p, lattice, seed + CLOUD_LAYER_WARP_SEED_Z));
    p += warp * noiseParams.w * CLOUD_LAYER_WARP_AMPLITUDE;

    vec2  field = periodicFbmBillow2D(p, lattice, octaves, seed);
    float sheet = clamp((field.x - 0.5) * CLOUD_LAYER_FIELD_CONTRAST + 0.5, 0.0, 1.0);
    float lumps = clamp((field.y - CLOUD_LAYER_BILLOW_FLOOR) / (1.0 - CLOUD_LAYER_BILLOW_FLOOR), 0.0, 1.0);
    float shape = sheet * mix(1.0, mix(CLOUD_LAYER_LUMP_MIN, CLOUD_LAYER_LUMP_MAX, lumps), fullness);

    if (detailFade > WEATHER_MAP_EPSILON) {
        float detailMultiplier = max(noiseParams.z, 1.0);
        float detail = periodicGradientNoise2D(
            p * detailMultiplier, lattice * detailMultiplier, seed + CLOUD_LAYER_DETAIL_SEED);
        shape = clamp(shape + detail * CLOUD_LAYER_DETAIL_STRENGTH * detailFade, 0.0, 1.0);
    }

    return shape;
}

// Density of one layer at a point inside it. heightFraction runs from 0 at
// the layer's base to 1 at its top.
float resolveCloudLayerDensity(int layer, vec2 positionXZ, float heightFraction, int octaves, float detailFade) {
    vec2  weather  = sampleWeatherLayer(layer, positionXZ);
    float coverage = weather.x * resolveWeatherMapEdgeFade(positionXZ);

    if (coverage <= WEATHER_MAP_EPSILON)
    return 0.0;

    vec4  shape    = u_weatherLayerShape[layer];
    vec4  surface  = u_weatherLayerSurface[layer];
    float fullness = shape.w;
    float h        = clamp(heightFraction, 0.0, 1.0);

    float baseRamp = mix(CLOUD_LAYER_SHEET_BASE_RAMP, CLOUD_LAYER_PUFFY_BASE_RAMP, fullness);
    float topStart = mix(CLOUD_LAYER_SHEET_TOP_START, CLOUD_LAYER_PUFFY_TOP_START, fullness);
    float envelope = smoothstep(0.0, baseRamp, h) * (1.0 - smoothstep(topStart, 1.0, h));

    if (envelope <= WEATHER_MAP_EPSILON)
    return 0.0;

    float effectiveCoverage = clamp(coverage * (CLOUD_LAYER_COVERAGE_BIAS_BASE + surface.z), 0.0, 1.0);
    float threshold = 1.0 - effectiveCoverage
    + effectiveCoverage * h * h * fullness * CLOUD_LAYER_CROWN_EROSION;
    float softness  = max(surface.w, CLOUD_LAYER_MIN_SOFTNESS);

    float field = sampleCloudLayerShape(layer, positionXZ, octaves, detailFade);
    float body  = smoothstep(threshold - softness, threshold + softness, field);

    return body * envelope * shape.z * weather.y;
}

#endif
