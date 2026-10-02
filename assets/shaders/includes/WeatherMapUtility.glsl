#ifndef WEATHER_MAP_UTILITY_GLSL
#define WEATHER_MAP_UTILITY_GLSL

#include "includes/WeatherMapData.glsl"

/*
 * Shared reads of a grid's weather window and its cloud layers, so the
 * weather pass, its fog and terrain cloud shadows see exactly the same
 * clouds. The window is sampled bilinearly between cell centres, which is what
 * lets the scrolling weather image glide smoothly instead of stepping cell by
 * cell. A cloud layer is not a volume of density but a column of cloud raised
 * over the ground plan of its shape field, and every field is read from the
 * baked cloud noise (u_cloudNoise) in a few filtered lookups rather than
 * computed. The shape, nudged by a small warp and cut by the local coverage,
 * tells how deep into a cloud a point lies. Puffy archetypes stand on a flat
 * base, rounded and lobed toward its rim, and rise with that depth, so a
 * small cloud stays low and only a broad one towers, to a height the tower
 * field varies from cluster to cluster: most clusters stay low and a few
 * stand tall. The rise climbs in a few soft tiers, so a tall cloud stacks
 * shelf over shelf of puffs. Sheets are slabs centred in the layer. Round
 * lobes carve the column's crown, each tier's shoulder, its rim and its base,
 * so a cloud seen from the side, from above or from below has the scalloped,
 * cauliflower outline of a painted cumulus. Lobes only ever carve, so a column
 * read without them always contains the lobed one — a march can step on the
 * cheap column and pay for lobes only where it touches cloud. Every cloud is
 * drawn solid; a column's opacity, from its own thickness, only weighs the
 * shadow it casts, so thin sheets shade the ground lightly while cloud bodies
 * shade it fully. A sample is rejected before any texture is read wherever the
 * answer is already known to be empty: beyond the window's faded edge and
 * wherever the weather holds no coverage.
 */

const float WEATHER_MAP_EPSILON           = 0.001;
const float WEATHER_MAP_EDGE_MARGIN_CELLS = 1.5;
const float WEATHER_MAP_EDGE_FADE_CELLS   = 2.0;

const float CLOUD_LAYER_WARP_AMPLITUDE       = 0.6;
const vec2  CLOUD_LAYER_WARP_OFFSET_X        = vec2(0.5, 0.0);
const vec2  CLOUD_LAYER_WARP_OFFSET_Z        = vec2(0.0, 0.5);
const vec2  CLOUD_LAYER_NOISE_OFFSET         = vec2(0.3819, 0.6180);
const float CLOUD_LAYER_COVERAGE_BIAS_BASE   = 0.4;
const float CLOUD_LAYER_BASE_ROUNDING        = 0.14;
const float CLOUD_LAYER_BASE_BUMP_SHARE      = 0.35;
const float CLOUD_LAYER_SHEET_CENTER         = 0.5;
const float CLOUD_LAYER_PUFFY_HEIGHT         = 0.8;
const float CLOUD_LAYER_RISE_POWER           = 1.0;
const float CLOUD_LAYER_TOWER_MIN            = 0.25;
const float CLOUD_LAYER_TOWER_POWER          = 2.0;
const float CLOUD_LAYER_TIERS                = 3.0;
const float CLOUD_LAYER_TIER_SOFTNESS        = 0.3;
const float CLOUD_LAYER_BUMP_DEPTH           = 0.22;
const float CLOUD_LAYER_BUMP_LATTICE_SCALE   = 0.7;
const float CLOUD_LAYER_BUMP_RIM_SHARE       = 0.6;
const float CLOUD_LAYER_SHEET_BUMP_SHARE     = 0.35;
const float CLOUD_LAYER_BUMP_FINE_RATIO      = 2.0;
const float CLOUD_LAYER_BUMP_FINE_WEIGHT     = 0.45;
const float CLOUD_LAYER_EXTINCTION_PER_BLOCK = 0.08;
const float CLOUD_LAYER_OPACITY_GAIN         = 2.5;
const float CLOUD_LAYER_MIN_SOFTNESS         = 0.02;

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
// of the layer's height, the opacity of the shadow it casts, and how deep in
// a crease between lobes it stands. An empty column has its bottom above its
// top and casts no shadow.
struct CloudColumn {
    float bottom;
    float top;
    float opacity;
    float crease;
};

const CloudColumn CLOUD_COLUMN_EMPTY = CloudColumn(1.0, 0.0, 0.0, 0.0);

// Whole repeats of the cloud noise across the shape period on x and z, as
// near as the layer's lattice of clouds allows, so the shapes tile with it.
vec2 resolveCloudLayerRepeats(int layer) {
    vec2 lattice = max(u_weatherLayerNoise[layer].xy, vec2(1.0));
    return max(floor(lattice / CLOUD_NOISE_SHAPE_CELLS + 0.5), vec2(1.0));
}

float resolveCloudLayerFeatureSize(int layer) {
    return u_weatherMapOrigin.w / (resolveCloudLayerRepeats(layer).y * CLOUD_NOISE_SHAPE_CELLS);
}

// Whole repeats of the lobe cells across the shape period, the same on both
// axes, so lobes are round in world space whatever the layer's elongation.
float resolveCloudLayerBumpRepeats(int layer) {
    vec4  noiseParams = u_weatherLayerNoise[layer];
    float lobes       = max(noiseParams.y, 1.0) * max(noiseParams.z, 1.0) * CLOUD_LAYER_BUMP_LATTICE_SCALE;

    return max(floor(lobes / CLOUD_NOISE_LOBE_CELLS + 0.5), 1.0);
}

// Horizontal size of a layer's coarse lobes.
float resolveCloudLayerBumpSize(int layer) {
    return u_weatherMapOrigin.w / (resolveCloudLayerBumpRepeats(layer) * CLOUD_NOISE_LOBE_CELLS);
}

// Where a point lies on the cloud noise, in shape-period units, each layer
// offset so no two layers share their clouds.
vec2 resolveCloudLayerNoisePosition(int layer, vec2 positionXZ) {
    return (u_weatherLayerSurface[layer].xy + positionXZ) / u_weatherMapOrigin.w
    + CLOUD_LAYER_NOISE_OFFSET * float(layer);
}

// How deep into a cloud a point lies, 0 at its rim to 1 at its core (x), and
// how tall its cluster stands, 0 to 1 (y). The shape is read through a small
// warp drawn from the tower field, and the coverage cuts the evenly spread
// shape so it covers exactly its share of sky.
vec2 resolveCloudLayerBody(int layer, vec2 noisePosition, float coverage) {
    vec4  noiseParams = u_weatherLayerNoise[layer];
    vec2  shapeUV     = noisePosition * resolveCloudLayerRepeats(layer);
    float towerX      = texture(u_cloudNoise, shapeUV + CLOUD_LAYER_WARP_OFFSET_X).z;
    float towerZ      = texture(u_cloudNoise, shapeUV + CLOUD_LAYER_WARP_OFFSET_Z).z;
    vec2  warp        = vec2(towerX, towerZ) - 0.5;

    shapeUV += warp * noiseParams.w * CLOUD_LAYER_WARP_AMPLITUDE / CLOUD_NOISE_SHAPE_CELLS;

    float shape             = texture(u_cloudNoise, shapeUV).x;
    float effectiveCoverage = clamp(
        coverage * (CLOUD_LAYER_COVERAGE_BIAS_BASE + u_weatherLayerSurface[layer].z), 0.0, 1.0);
    float body              = clamp(
        (shape - (1.0 - effectiveCoverage)) / max(effectiveCoverage, WEATHER_MAP_EPSILON), 0.0, 1.0);

    return vec2(body, towerX);
}

// Height the lobes carve from a column, as a fraction of the layer: nothing
// on a lobe's crown, the most in the creases between lobes. bumpFade scales
// the coarse (x) and fine (y) lobes, so distant cloud skips what a pixel
// cannot show.
float resolveCloudLayerCarve(int layer, vec2 noisePosition, vec2 bumpFade) {
    if (bumpFade.x <= WEATHER_MAP_EPSILON)
    return 0.0;

    vec2  lobeUV = noisePosition * resolveCloudLayerBumpRepeats(layer);
    float carve  = (1.0 - texture(u_cloudNoise, lobeUV).y) * bumpFade.x;

    if (bumpFade.y > WEATHER_MAP_EPSILON)
    carve += (1.0 - texture(u_cloudNoise, lobeUV * CLOUD_LAYER_BUMP_FINE_RATIO).y)
    * CLOUD_LAYER_BUMP_FINE_WEIGHT * bumpFade.y;

    return carve * CLOUD_LAYER_BUMP_DEPTH;
}

// The rise of a puffy column over its depth into the cloud: proportional to
// that depth, so height follows breadth, climbing in soft tiers.
float resolveCloudLayerRise(float body, float tower) {
    float stand = mix(CLOUD_LAYER_TOWER_MIN, 1.0, pow(tower, CLOUD_LAYER_TOWER_POWER)) * CLOUD_LAYER_PUFFY_HEIGHT;
    float rise  = pow(body, CLOUD_LAYER_RISE_POWER) * stand;
    float tiers = rise * CLOUD_LAYER_TIERS;
    float tier  = floor(tiers);
    float shelf = smoothstep(0.5 - CLOUD_LAYER_TIER_SOFTNESS, 0.5 + CLOUD_LAYER_TIER_SOFTNESS, tiers - tier);

    return (tier + shelf) / CLOUD_LAYER_TIERS;
}

// The column of one layer over a point. Pass a zero bumpFade for the cheap
// column, which always contains the lobed one.
CloudColumn resolveCloudColumn(int layer, vec2 positionXZ, vec2 bumpFade) {
    float edgeFade = resolveWeatherMapEdgeFade(positionXZ);

    if (edgeFade <= WEATHER_MAP_EPSILON)
    return CLOUD_COLUMN_EMPTY;

    vec2  weather  = sampleWeatherLayer(layer, positionXZ);
    float coverage = weather.x * edgeFade;

    if (coverage <= WEATHER_MAP_EPSILON)
    return CLOUD_COLUMN_EMPTY;

    vec2  noisePosition = resolveCloudLayerNoisePosition(layer, positionXZ);
    vec2  bodyTower     = resolveCloudLayerBody(layer, noisePosition, coverage);
    float body          = bodyTower.x;

    if (body <= WEATHER_MAP_EPSILON)
    return CLOUD_COLUMN_EMPTY;

    vec4  shape    = u_weatherLayerShape[layer];
    float fullness = shape.w;
    float carve    = resolveCloudLayerCarve(layer, noisePosition, bumpFade)
    * mix(CLOUD_LAYER_SHEET_BUMP_SHARE, 1.0, fullness);

    // Lobes eat into the cloud's footprint as well as its crown, so its
    // outline and walls swell into round lobes rather than one smooth wall.
    body -= carve * CLOUD_LAYER_BUMP_RIM_SHARE;

    if (body <= WEATHER_MAP_EPSILON)
    return CLOUD_COLUMN_EMPTY;

    float dome   = sqrt(body * (2.0 - body));
    float sheet  = dome - carve;
    float puffy  = resolveCloudLayerRise(body, bodyTower.y) - carve;
    float crease = clamp(carve / (CLOUD_LAYER_BUMP_DEPTH * (1.0 + CLOUD_LAYER_BUMP_FINE_WEIGHT)), 0.0, 1.0);

    float base   = CLOUD_LAYER_BASE_ROUNDING * (1.0 - body) * (1.0 - body) + carve * CLOUD_LAYER_BASE_BUMP_SHARE;
    float bottom = mix(CLOUD_LAYER_SHEET_CENTER - sheet * 0.5, base, fullness);
    float top    = mix(CLOUD_LAYER_SHEET_CENTER + sheet * 0.5, puffy, fullness);

    if (top <= bottom)
    return CLOUD_COLUMN_EMPTY;

    // Shadow opacity follows the uncarved dome, so carving shapes a cloud's
    // outline without thinning its shadow into a speckle. Puffy cloud shades
    // fully, only its rim softening; sheets shade by their thickness.
    float opticalDepth = dome * shape.y * shape.z * weather.y * CLOUD_LAYER_EXTINCTION_PER_BLOCK;
    float softness     = max(u_weatherLayerSurface[layer].w, CLOUD_LAYER_MIN_SOFTNESS);
    float opacity      = mix(1.0 - exp(-opticalDepth * CLOUD_LAYER_OPACITY_GAIN), 1.0, fullness);

    return CloudColumn(bottom, top, opacity * smoothstep(0.0, softness, body), crease);
}

bool isInsideCloudColumn(CloudColumn column, float heightFraction) {
    return heightFraction >= column.bottom && heightFraction <= column.top;
}

// How far inside a column a height lies, in layer height fractions: positive
// inside, zero on its surface, negative outside. An empty column reads as
// the whole layer away, so a cloud's wall stands out sharply against it.
float resolveCloudColumnDepth(CloudColumn column, float heightFraction) {
    return min(heightFraction - column.bottom, column.top - heightFraction);
}

#endif
