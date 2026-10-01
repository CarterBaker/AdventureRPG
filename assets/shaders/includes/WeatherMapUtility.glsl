#ifndef WEATHER_MAP_UTILITY_GLSL
#define WEATHER_MAP_UTILITY_GLSL

#include "includes/WeatherMapData.glsl"
#include "includes/NoiseUtility.glsl"

/*
 * Shared reads of a grid's weather window and its cloud layers, so the sky
 * pass, the lighting pass and terrain cloud shadows see exactly the same
 * clouds. The window is sampled bilinearly between cell centres, which is what
 * lets the scrolling weather image glide smoothly instead of stepping cell by
 * cell. A cloud layer is not a volume of density but a column of cloud raised
 * over the ground plan of its shape field: the archetype's periodic field, cut
 * by the local coverage, tells how deep into a cloud a point lies, and that
 * depth lifts a column between a bottom and a top height. Puffy archetypes
 * stand on a flat base under a low, round-shouldered dome, its profile a
 * quarter circle over the cloud's depth; sheets are slabs centred in the layer. Round
 * sphere bumps carve the column's crown and rim, so a cloud seen from the side
 * or from above has the scalloped, cauliflower outline of a painted cumulus.
 * Bumps only ever carve, so a column read without them always contains the
 * bumped one — a march can step on the cheap column and pay for bumps only
 * where it touches cloud. A column's opacity comes from its own thickness, so
 * rims and thin sheets stay translucent while cloud bodies read solid. A
 * sample is rejected before any noise is drawn wherever the answer is already
 * known to be empty: beyond the window's faded edge and wherever the weather
 * holds no coverage.
 */

const float WEATHER_MAP_EPSILON           = 0.001;
const float WEATHER_MAP_EDGE_MARGIN_CELLS = 1.5;
const float WEATHER_MAP_EDGE_FADE_CELLS   = 2.0;

const float CLOUD_LAYER_WARP_AMPLITUDE       = 0.6;
const float CLOUD_LAYER_FIELD_CONTRAST       = 2.2;
const float CLOUD_LAYER_BILLOW_MEAN          = 0.86;
const float CLOUD_LAYER_BILLOW_SCALE         = 0.83;
const float CLOUD_LAYER_BILLOW_SHARE         = 0.5;
const float CLOUD_LAYER_COVERAGE_BIAS_BASE   = 0.4;
const float CLOUD_LAYER_BASE_ROUNDING        = 0.12;
const float CLOUD_LAYER_SHEET_CENTER         = 0.5;
const float CLOUD_LAYER_BUMP_DEPTH           = 0.5;
const float CLOUD_LAYER_BUMP_LATTICE_SCALE   = 0.7;
const float CLOUD_LAYER_PUFFY_HEIGHT         = 0.55;
const float CLOUD_LAYER_BUMP_RIM_SHARE       = 0.6;
const float CLOUD_LAYER_SHEET_BUMP_SHARE     = 0.35;
const float CLOUD_LAYER_BUMP_FINE_RATIO      = 2.0;
const float CLOUD_LAYER_BUMP_FINE_WEIGHT     = 0.45;
const float CLOUD_LAYER_EXTINCTION_PER_BLOCK = 0.08;
const float CLOUD_LAYER_OPACITY_GAIN         = 2.5;
const float CLOUD_LAYER_MIN_SOFTNESS         = 0.02;

const uint CLOUD_LAYER_SEED_STRIDE    = 7919u;
const uint CLOUD_LAYER_WARP_SEED_X    = 131u;
const uint CLOUD_LAYER_WARP_SEED_Z    = 257u;
const uint CLOUD_LAYER_BUMP_SEED      = 521u;
const uint CLOUD_LAYER_BUMP_FINE_SEED = 877u;

// ── Weather Window ─────────────────────────────────────────────────────────

float unpackWeatherChannel(int channels, int layer) {
    int shift = (layer % WEATHER_MAP_LAYERS_PER_COMPONENT) * 8;
    return float((channels >> shift) & 255) / WEATHER_MAP_CHANNEL_MAX;
}

// x = coverage, y = density scale of one layer in one cell.
vec2 readWeatherCell(ivec2 cell, int layer) {
    ivec4 cellData = u_weatherCells[cell.y * WEATHER_MAP_RESOLUTION + cell.x];
    bool  upper    = layer >= WEATHER_MAP_LAYERS_PER_COMPONENT;

    return vec2(
        unpackWeatherChannel(upper ? cellData.y : cellData.x, layer),
        unpackWeatherChannel(upper ? cellData.w : cellData.z, layer) * WEATHER_MAP_DENSITY_SCALE_MAX);
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
    float reach = resolveWeatherMapReach();
    return 1.0 - smoothstep(reach - WEATHER_MAP_EDGE_FADE_CELLS * u_weatherMapOrigin.z, reach, length(positionXZ));
}

// ── Cloud Layer Shape ──────────────────────────────────────────────────────

// The cloud standing over one point of a layer: bottom and top as fractions
// of the layer's height, the column's opacity, the share of light that
// passes through its body, and how deep in a crease between bumps it stands.
// An empty column has its bottom above its top and no opacity.
struct CloudColumn {
    float bottom;
    float top;
    float alpha;
    float translucency;
    float crease;
};

const CloudColumn CLOUD_COLUMN_EMPTY = CloudColumn(1.0, 0.0, 0.0, 1.0, 0.0);

float resolveCloudLayerFeatureSize(int layer) {
    return u_weatherMapOrigin.w / max(u_weatherLayerNoise[layer].y, 1.0);
}

// Whole bump cells across the shape period, so the bumps tile with it.
float resolveCloudLayerBumpLattice(int layer) {
    vec4 noiseParams = u_weatherLayerNoise[layer];
    return max(floor(max(noiseParams.y, 1.0) * max(noiseParams.z, 1.0) * CLOUD_LAYER_BUMP_LATTICE_SCALE + 0.5), 1.0);
}

// Horizontal size of a layer's coarse bumps.
float resolveCloudLayerBumpSize(int layer) {
    return u_weatherMapOrigin.w / resolveCloudLayerBumpLattice(layer);
}

// How deep into a cloud a point lies, 0 at its rim to 1 at its core: the
// archetype's warped shape field cut by the local coverage. Puffy archetypes
// lean partly on the billow octaves turned over, which swell into rounded
// lobes split by sharp creases, so a cloud keeps its broad body while its
// outline takes a cumulus's cauliflower plan. The billow is
// recentred onto the plain field's spread, so an authored coverage covers the
// same share of sky whatever the fullness.
float resolveCloudLayerBody(int layer, vec2 positionXZ, float coverage, int octaves) {
    vec4 noiseParams = u_weatherLayerNoise[layer];
    vec4 surface     = u_weatherLayerSurface[layer];
    vec2 lattice     = max(noiseParams.xy, vec2(1.0));
    vec2 p           = (surface.xy + positionXZ) / u_weatherMapOrigin.w * lattice;
    uint seed        = uint(layer) * CLOUD_LAYER_SEED_STRIDE;

    vec2 warp = periodicGradientNoise2DPair(p, lattice, seed + CLOUD_LAYER_WARP_SEED_X, seed + CLOUD_LAYER_WARP_SEED_Z);
    p += warp * noiseParams.w * CLOUD_LAYER_WARP_AMPLITUDE;

    vec2  fbmBillow         = periodicFbmBillow2D(p, lattice, octaves, seed);
    float billow            = (CLOUD_LAYER_BILLOW_MEAN - fbmBillow.y) * CLOUD_LAYER_BILLOW_SCALE + 0.5;
    float field             = mix(fbmBillow.x, billow, u_weatherLayerShape[layer].w * CLOUD_LAYER_BILLOW_SHARE);
    float sheet             = clamp((field - 0.5) * CLOUD_LAYER_FIELD_CONTRAST + 0.5, 0.0, 1.0);
    float effectiveCoverage = clamp(coverage * (CLOUD_LAYER_COVERAGE_BIAS_BASE + surface.z), 0.0, 1.0);

    return clamp((sheet - (1.0 - effectiveCoverage)) / max(effectiveCoverage, WEATHER_MAP_EPSILON), 0.0, 1.0);
}

// Height the bumps carve from a column, as a fraction of the layer: nothing
// on a bump's crown, the most in the creases between bumps. Bumps are round
// in world space whatever the layer's elongation. bumpFade scales the coarse
// (x) and fine (y) bumps, so distant cloud skips what a pixel cannot show.
float resolveCloudLayerCarve(int layer, vec2 positionXZ, vec2 bumpFade) {
    if (bumpFade.x <= WEATHER_MAP_EPSILON)
    return 0.0;

    float lattice = resolveCloudLayerBumpLattice(layer);
    vec2  p       = (u_weatherLayerSurface[layer].xy + positionXZ) / u_weatherMapOrigin.w * lattice;
    uint  seed    = uint(layer) * CLOUD_LAYER_SEED_STRIDE;

    float carve = (1.0 - periodicSphereBumps2D(p, vec2(lattice), seed + CLOUD_LAYER_BUMP_SEED)) * bumpFade.x;

    if (bumpFade.y > WEATHER_MAP_EPSILON)
    carve += (1.0 - periodicSphereBumps2D(
        p * CLOUD_LAYER_BUMP_FINE_RATIO,
        vec2(lattice * CLOUD_LAYER_BUMP_FINE_RATIO),
        seed + CLOUD_LAYER_BUMP_FINE_SEED)) * CLOUD_LAYER_BUMP_FINE_WEIGHT * bumpFade.y;

    return carve * CLOUD_LAYER_BUMP_DEPTH;
}

// The column of one layer over a point. Pass a zero bumpFade for the cheap
// column, which always contains the bumped one.
CloudColumn resolveCloudColumn(int layer, vec2 positionXZ, int octaves, vec2 bumpFade) {
    float edgeFade = resolveWeatherMapEdgeFade(positionXZ);

    if (edgeFade <= WEATHER_MAP_EPSILON)
    return CLOUD_COLUMN_EMPTY;

    vec2  weather  = sampleWeatherLayer(layer, positionXZ);
    float coverage = weather.x * edgeFade;

    if (coverage <= WEATHER_MAP_EPSILON)
    return CLOUD_COLUMN_EMPTY;

    float body = resolveCloudLayerBody(layer, positionXZ, coverage, octaves);

    if (body <= WEATHER_MAP_EPSILON)
    return CLOUD_COLUMN_EMPTY;

    vec4  shape    = u_weatherLayerShape[layer];
    float fullness = shape.w;
    float carve    = resolveCloudLayerCarve(layer, positionXZ, bumpFade)
    * mix(CLOUD_LAYER_SHEET_BUMP_SHARE, 1.0, fullness);

    // Bumps eat into the cloud's footprint as well as its crown, so its
    // outline and walls swell into round lobes rather than one smooth wall.
    body -= carve * CLOUD_LAYER_BUMP_RIM_SHARE;

    if (body <= WEATHER_MAP_EPSILON)
    return CLOUD_COLUMN_EMPTY;

    float dome     = sqrt(body * (2.0 - body));
    float rise     = dome - carve;
    float crease   = clamp(carve / (CLOUD_LAYER_BUMP_DEPTH * (1.0 + CLOUD_LAYER_BUMP_FINE_WEIGHT)), 0.0, 1.0);

    float bottom = mix(CLOUD_LAYER_SHEET_CENTER - rise * 0.5, CLOUD_LAYER_BASE_ROUNDING * (1.0 - body), fullness);
    float top    = mix(CLOUD_LAYER_SHEET_CENTER + rise * 0.5, rise * CLOUD_LAYER_PUFFY_HEIGHT, fullness);

    if (top <= bottom)
    return CLOUD_COLUMN_EMPTY;

    // Opacity follows the uncarved dome, so carving shapes a cloud's outline
    // without thinning it into a speckle of translucent creases. Puffy cloud
    // is solid outright, only its rim softening; sheets keep the opacity of
    // their thickness so thin ones still glow through.
    float opticalDepth = dome * shape.y * shape.z * weather.y * CLOUD_LAYER_EXTINCTION_PER_BLOCK;
    float softness     = max(u_weatherLayerSurface[layer].w, CLOUD_LAYER_MIN_SOFTNESS);
    float opacity      = mix(1.0 - exp(-opticalDepth * CLOUD_LAYER_OPACITY_GAIN), 1.0, fullness);
    float alpha        = opacity * smoothstep(0.0, softness, body);

    return CloudColumn(bottom, top, alpha, exp(-opticalDepth), crease);
}

bool isInsideCloudColumn(CloudColumn column, float heightFraction) {
    return column.alpha > WEATHER_MAP_EPSILON && heightFraction >= column.bottom && heightFraction <= column.top;
}

// How far inside a column a height lies, in layer height fractions: positive
// inside, zero on its surface, negative outside. An empty column reads as
// the whole layer away, so a cloud's wall stands out sharply against it.
float resolveCloudColumnDepth(CloudColumn column, float heightFraction) {
    return min(heightFraction - column.bottom, column.top - heightFraction);
}

#endif
