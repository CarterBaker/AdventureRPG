#version 330 core

in  vec3 v_dir;
in  vec2 v_screenPos;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/PrecipitationData.glsl"
#include "includes/NoiseUtility.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/WeatherMapUtility.glsl"
#include "weather/includes/CloudDome.glsl"
#include "weather/includes/CloudRain.glsl"

uniform sampler2D u_sceneDepth;
uniform sampler2D u_waterDepth;

// The weather pass's distance target: its cloud surfaces and rain shafts (CloudComposite).
uniform sampler2D u_cloudDistance;

/*
 * Fullscreen rain and snow, anchored to the world. A drop carried sideways at
 * the wind's speed while falling at its own traces a straight slanted path,
 * so every drop falls down a column that stands still in the world, leaning
 * downwind, while drops stream along it. Columns are scattered one per cell
 * of a grid laid in that slanted frame, and each view ray walks the cells it
 * crosses and meets each column once, at its closest approach, so drops keep
 * their place as the camera turns and slide past with true parallax as it
 * moves. Distance is split into bands, each twice as far as the last with
 * cells twice as wide, so every band costs the same few steps and
 * neighbouring bands cross-fade rather than pop. Near drops read as distinct
 * streaks; far ones thin to lines narrower than a pixel whose opacity falls
 * with the share of the pixel they cover. A drop is skipped behind the scene
 * depth or a cloud surface and wherever it lies beneath the top of its block
 * column, so nothing falls in front of terrain, through a roof, or into a
 * cave. Drops fall only beneath a rain-bearing cloud and below its underside
 * (CloudRain), thinning out toward its edge, so no rain falls from clear sky
 * or above the cloud it comes from. Beneath the drops lie the rain shafts the
 * weather pass found hanging under every rain cloud in view, a dark mist that
 * shows a storm from far off. Written premultiplied for the composite.
 */

struct PrecipitationKind {
    float fallSpeed;
    float cellRatio;
    float spacingRatio;
    float streakShare;
    float dropRadius;
    float densityMin;
    float densityMax;
    float dropShare;
    float alpha;
    float sway;
    float swayFrequency;
    float seed;
    bool  streak;
};

const PrecipitationKind PRECIPITATION_RAIN =
PrecipitationKind(11.0, 0.3, 2.0, 0.4, 0.005, 0.3, 1.0, 0.75, 0.65, 0.0, 0.0, 0.0, true);
const PrecipitationKind PRECIPITATION_SNOW =
PrecipitationKind(1.4, 0.3, 1.0, 0.0, 0.012, 0.15, 0.7, 0.6, 0.9, 0.12, 1.3, 101.0, false);

const float PRECIPITATION_EPSILON         = 0.0001;
const float PRECIPITATION_TWO_PI          = 6.28318531;
const int   PRECIPITATION_BAND_COUNT      = 6;
const int   PRECIPITATION_BAND_STEPS      = 24;
const float PRECIPITATION_BAND_START      = 1.0;
const float PRECIPITATION_BAND_OVERLAP    = 0.2;
const float PRECIPITATION_BAND_SEED       = 17.0;
const float PRECIPITATION_DROP_SEED       = 7.0;
const float PRECIPITATION_NEAR_START      = 0.3;
const float PRECIPITATION_CELL_MARGIN     = 0.15;
const float PRECIPITATION_STEEP_FADE_LOW  = 0.05;
const float PRECIPITATION_STEEP_FADE_HIGH = 0.2;
const float PRECIPITATION_MAX_LEAN        = 3.0;
const float PRECIPITATION_COVERAGE_LIMIT  = 0.99;
const float PRECIPITATION_ZENITH_SHARE    = 0.4;
const float PRECIPITATION_NO_SCENE        = 1.0e30;

const float RAIN_SHAFT_SHADE        = 0.55;
const float RAIN_SHAFT_SATURATION   = 0.4;
const vec3  RAIN_SHAFT_LUMA_WEIGHTS = vec3(0.299, 0.587, 0.114);

const float RAIN_HEAD_SHARE    = 0.3;
const float RAIN_BRIGHTNESS    = 1.1;
const float SNOW_SWAY_ASPECT   = 0.7;
const float SNOW_SWAY_SEED     = 5.0;
const float SNOW_FLAKE_CORE    = 0.5;
const float SNOW_BRIGHTNESS    = 1.5;

// ── Scene ──────────────────────────────────────────────────────────────────

// Distance from the camera to the nearest opaque or water surface along this pixel.
float resolveSceneDistance(vec2 uv) {
    float depth = min(texture(u_sceneDepth, uv).r, texture(u_waterDepth, uv).r);

    if (depth >= 1.0)
    return PRECIPITATION_NO_SCENE;

    vec4 view = u_inverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);

    return length(view.xyz / view.w);
}

// Distance to the cloud surface the weather pass drew along this pixel, if any.
float resolveCloudSurfaceDistance(vec2 uv) {
    ivec2 size  = textureSize(u_cloudDistance, 0);
    float cloud = texelFetch(u_cloudDistance, clamp(ivec2(uv * vec2(size)), ivec2(0), size - 1), 0).x;

    return cloud > 0.0 ? cloud : PRECIPITATION_NO_SCENE;
}

// ── Shelter ────────────────────────────────────────────────────────────────

// Absolute sub-voxel Y just above the highest block or item in the column
// under a point, read from the ring-buffered column map.
int resolveColumnTop(vec2 positionXZ) {
    ivec2 column = u_precipitationWindow.zw + ivec2(floor(positionXZ));
    ivec2 local  = column - u_precipitationWindow.xy;

    if (any(lessThan(local, ivec2(0))) || any(greaterThanEqual(local, ivec2(PRECIPITATION_MAP_SIZE))))
    return 0;

    ivec2 slot  = column & (PRECIPITATION_MAP_SIZE - 1);
    int   index = slot.y * PRECIPITATION_MAP_SIZE + slot.x;
    int   word  = u_precipitationColumns[index >> 3][(index >> 1) & 3];

    return (index & 1) == 0 ? (word & 0xFFFF) : ((word >> 16) & 0xFFFF);
}

bool isSheltered(vec3 position) {
    return position.y < float(resolveColumnTop(position.xz)) / PRECIPITATION_HEIGHTS_PER_BLOCK;
}

// ── Columns ────────────────────────────────────────────────────────────────

// Horizontal blocks a drop drifts per block it falls.
vec2 resolveLean(PrecipitationKind kind) {
    vec2  lean   = u_precipitationState.zw / kind.fallSpeed;
    float amount = length(lean);

    return amount > PRECIPITATION_MAX_LEAN ? lean * (PRECIPITATION_MAX_LEAN / amount) : lean;
}

// Where the ray passes closest to a column: distance along the ray, and the
// column's point there relative to the camera. The column runs through
// linePoint at eye level along the unit lineDir.
float resolveColumnApproach(vec3 dir, vec3 lineDir, vec3 linePoint, out vec3 closest) {
    float beta  = dot(dir, lineDir);
    float along = dot(lineDir, linePoint);
    float t     = (dot(dir, linePoint) - beta * along) / max(1.0 - beta * beta, PRECIPITATION_EPSILON);

    closest = linePoint + lineDir * (t * beta - along);

    return t;
}

// ── Bands ──────────────────────────────────────────────────────────────────

float resolveBandInner(int band) {
    return PRECIPITATION_BAND_START * exp2(float(band));
}

float resolveBandStart(int band) {
    return band == 0 ? PRECIPITATION_NEAR_START : resolveBandInner(band) * (1.0 - PRECIPITATION_BAND_OVERLAP);
}

float resolveBandEnd(int band) {
    return resolveBandInner(band) * 2.0 * (1.0 + PRECIPITATION_BAND_OVERLAP);
}

// Fades a band in across its inner edge and out across its outer edge, over
// the same stretch its neighbour fades across, so the two always sum to one.
float resolveBandWeight(int band, float t) {
    float inner  = resolveBandInner(band);
    float outer  = inner * 2.0;
    float fadeIn = band == 0
    ? smoothstep(PRECIPITATION_NEAR_START, inner, t)
    : smoothstep(inner * (1.0 - PRECIPITATION_BAND_OVERLAP), inner * (1.0 + PRECIPITATION_BAND_OVERLAP), t);

    return fadeIn * (1.0 - smoothstep(
        outer * (1.0 - PRECIPITATION_BAND_OVERLAP), outer * (1.0 + PRECIPITATION_BAND_OVERLAP), t));
}

// ── Drops ──────────────────────────────────────────────────────────────────

// Position down a column in drops: the integer part names one drop, the
// fraction is how far above that drop a height lies. It advances with time
// in world height, so drops fall at their own speed.
float resolveDropCoordinate(PrecipitationKind kind, float height, float spacing, float phase) {
    return (height + u_time * kind.fallSpeed) / spacing + phase;
}

// A drop's lot, 0 to 1: only drops whose lot lies within the share of a
// column the kind lets fall fall at all, so no column reads as an evenly
// spaced string of drops.
float resolveDropLot(vec2 cellId, float drop, float seed) {
    return hash31(vec3(cellId, seed + drop * PRECIPITATION_DROP_SEED));
}

// Beneath a rain cloud the share narrows with the cloud's cover, so its rain
// thins out toward its edge, and nothing falls in the clear or above it.
bool isDropRainedOn(PrecipitationKind kind, vec3 relativePosition, float lot) {
    return lot <= kind.dropShare * resolveCloudRainCover(relativePosition, vec2(1.0, 0.0));
}

// ── Rain ───────────────────────────────────────────────────────────────────

// A thin streak with a bright head, trailing up the column behind the drop.
float resolveRainDrop(
    PrecipitationKind kind, int band, vec3 dir, vec3 lineDir, vec3 linePoint, vec2 cellId, float seed,
    float spacing, float phase, float pixelAngle, float sceneDistance) {
    vec3  closest;
    float t = resolveColumnApproach(dir, lineDir, linePoint, closest);

    if (t <= PRECIPITATION_EPSILON || t >= sceneDistance)
    return 0.0;

    float coordinate = resolveDropCoordinate(kind, u_cameraPosition.y + closest.y, spacing, phase);
    float along      = fract(coordinate) * spacing;
    float extent     = spacing * kind.streakShare;
    float head       = extent * RAIN_HEAD_SHARE;
    float streak     = smoothstep(0.0, head, along) * (1.0 - smoothstep(head, extent, along));
    float lot        = resolveDropLot(cellId, floor(coordinate), seed);

    if (streak <= PRECIPITATION_EPSILON || lot > kind.dropShare)
    return 0.0;

    float footprint = max(kind.dropRadius, pixelAngle * t);
    float across    = (1.0 - smoothstep(0.0, footprint, length(dir * t - closest))) * kind.dropRadius / footprint;

    if (across <= PRECIPITATION_EPSILON || isSheltered(u_cameraPosition + closest)
    || !isDropRainedOn(kind, closest, lot))
    return 0.0;

    return across * streak * resolveBandWeight(band, t);
}

// ── Snow ───────────────────────────────────────────────────────────────────

// A soft round flake at the nearest of the column's flakes, swaying about
// the column on its own rhythm as it drifts down it.
float resolveSnowFlake(
    PrecipitationKind kind, int band, vec3 dir, vec3 lineDir, vec3 linePoint, vec2 cellId, float seed,
    float spacing, float phase, float cellSize, float pixelAngle, float sceneDistance) {
    vec3 closest;
    resolveColumnApproach(dir, lineDir, linePoint, closest);

    float coordinate = resolveDropCoordinate(kind, u_cameraPosition.y + closest.y, spacing, phase);
    float drop       = floor(coordinate + 0.5);
    float lot        = resolveDropLot(cellId, drop, seed);

    if (lot > kind.dropShare)
    return 0.0;

    float swayPhase = hash31(vec3(cellId, seed + drop * PRECIPITATION_DROP_SEED + SNOW_SWAY_SEED));
    float swayAt    = u_time * kind.swayFrequency + swayPhase * PRECIPITATION_TWO_PI;
    vec3  sway      = vec3(sin(swayAt), 0.0, cos(swayAt * SNOW_SWAY_ASPECT)) * kind.sway * cellSize;
    vec3  flake     = closest - lineDir * ((coordinate - drop) * spacing / lineDir.y) + sway;
    float t         = dot(flake, dir);

    if (t <= PRECIPITATION_EPSILON || t >= sceneDistance)
    return 0.0;

    float footprint = max(kind.dropRadius, pixelAngle * t);
    float cover     = kind.dropRadius / footprint;
    float disc      = (1.0 - smoothstep(footprint * SNOW_FLAKE_CORE, footprint, length(flake - dir * t)))
    * cover * cover;

    if (disc <= PRECIPITATION_EPSILON || isSheltered(u_cameraPosition + flake)
    || !isDropRainedOn(kind, flake, lot))
    return 0.0;

    return disc * resolveBandWeight(band, t);
}

// ── March ──────────────────────────────────────────────────────────────────

// Walks the cells of one band's grid that the ray crosses, in the frame where
// every column stands upright, and gathers the drop in each occupied cell.
// Spacing down a column grows with the cells, so every band is a scaled copy
// of the first.
float integratePrecipitationBand(
    PrecipitationKind kind, int band, vec3 dir, vec2 lean, vec3 lineDir, float pixelAngle, float intensity,
    float sceneDistance, float coverage) {
    float tStart = resolveBandStart(band);
    float tEnd   = min(resolveBandEnd(band), sceneDistance);

    if (tStart >= tEnd)
    return coverage;

    float cellSize = resolveBandInner(band) * kind.cellRatio;
    float spacing  = cellSize * kind.spacingRatio;
    float density  = mix(kind.densityMin, kind.densityMax, intensity);
    float seed     = kind.seed + float(band) * PRECIPITATION_BAND_SEED;

    vec2 origin   = (u_cameraPosition.xz + lean * u_cameraPosition.y) / cellSize;
    vec2 baseCell = floor(origin);
    vec2 local    = origin - baseCell;
    vec2 velocity = (dir.xz + lean * dir.y) / cellSize;
    vec2 position = local + velocity * tStart;
    vec2 cell     = floor(position);
    vec2 stepDir  = sign(velocity);
    vec2 tDelta   = 1.0 / max(abs(velocity), vec2(PRECIPITATION_EPSILON));
    vec2 tNext    = tStart + mix(position - cell, cell + 1.0 - position, step(0.0, stepDir)) * tDelta;

    for (int s = 0; s < PRECIPITATION_BAND_STEPS; s++) {
        vec2 cellId = baseCell + cell;

        if (hash31(vec3(cellId, seed)) <= density) {
            vec2 offset = mix(
                vec2(PRECIPITATION_CELL_MARGIN), vec2(1.0 - PRECIPITATION_CELL_MARGIN),
                vec2(hash31(vec3(cellId, seed + 1.0)), hash31(vec3(cellId, seed + 2.0))));
            vec2  column    = (cell + offset - local) * cellSize;
            vec3  linePoint = vec3(column.x, 0.0, column.y);
            float phase     = hash31(vec3(cellId, seed + 3.0));
            float drop      = kind.streak
            ? resolveRainDrop(
                kind, band, dir, lineDir, linePoint, cellId, seed, spacing, phase, pixelAngle, sceneDistance)
            : resolveSnowFlake(
                kind, band, dir, lineDir, linePoint, cellId, seed, spacing, phase, cellSize, pixelAngle,
                sceneDistance);

            coverage += (1.0 - coverage) * drop * kind.alpha;
        }

        float tCell = min(tNext.x, tNext.y);

        if (tCell > tEnd || coverage > PRECIPITATION_COVERAGE_LIMIT)
        break;

        if (tNext.x < tNext.y) {
            tNext.x += tDelta.x;
            cell.x  += stepDir.x;
        } else {
            tNext.y += tDelta.y;
            cell.y  += stepDir.y;
        }
    }

    return coverage;
}

// Coverage of one kind of precipitation along the view ray, faded where the
// ray runs nearly along the columns and every drop would collapse to a point.
float integratePrecipitation(
    PrecipitationKind kind, vec3 dir, float pixelAngle, float intensity, float sceneDistance) {
    vec2  lean      = resolveLean(kind);
    vec3  lineDir   = normalize(vec3(-lean.x, 1.0, -lean.y));
    float alignment = dot(dir, lineDir);
    float steepFade = smoothstep(
        PRECIPITATION_STEEP_FADE_LOW, PRECIPITATION_STEEP_FADE_HIGH, sqrt(max(1.0 - alignment * alignment, 0.0)));
    float coverage  = 0.0;

    if (steepFade <= PRECIPITATION_EPSILON)
    return 0.0;

    for (int band = 0; band < PRECIPITATION_BAND_COUNT; band++) {
        coverage = integratePrecipitationBand(
            kind, band, dir, lean, lineDir, pixelAngle, intensity, sceneDistance, coverage);

        if (coverage > PRECIPITATION_COVERAGE_LIMIT)
        break;
    }

    return coverage * steepFade;
}

// ── Main ───────────────────────────────────────────────────────────────────

void main() {
    vec3  dir        = normalize(v_dir);
    float pixelAngle = length(fwidth(dir));
    float intensity  = u_precipitationState.x;
    float snow       = u_precipitationState.y;
    vec2  uv         = v_screenPos * 0.5 + 0.5;
    vec2  shaft      = sampleRainShaft(u_cloudDistance, uv);
    bool  falling    = intensity > PRECIPITATION_EPSILON;

    if (!falling && shaft.y <= PRECIPITATION_EPSILON)
    discard;

    float sceneDistance = resolveSceneDistance(uv);
    float shaftCover    = resolveRainShaftOpacity(shaft, sceneDistance);
    float dropDistance  = min(sceneDistance, resolveCloudSurfaceDistance(uv));
    float rain          = falling && snow < 1.0
    ? integratePrecipitation(PRECIPITATION_RAIN, dir, pixelAngle, intensity, dropDistance)
    : 0.0;
    float flakes        = falling && snow > 0.0
    ? integratePrecipitation(PRECIPITATION_SNOW, dir, pixelAngle, intensity, dropDistance)
    : 0.0;
    float coverage      = mix(rain, flakes, snow);

    if (coverage <= PRECIPITATION_EPSILON && shaftCover <= PRECIPITATION_EPSILON)
    discard;

    vec3  skyLight   = mix(u_skyHorizonColor, u_skyZenithColor, PRECIPITATION_ZENITH_SHARE);
    vec3  dropColor  = skyLight * mix(RAIN_BRIGHTNESS, SNOW_BRIGHTNESS, snow);
    vec3  shaftColor = mix(vec3(dot(skyLight, RAIN_SHAFT_LUMA_WEIGHTS)), skyLight, RAIN_SHAFT_SATURATION)
    * RAIN_SHAFT_SHADE;
    float shaftShown = shaftCover * (1.0 - coverage);

    fragColor = vec4(dropColor * coverage + shaftColor * shaftShown, coverage + shaftShown);
}
