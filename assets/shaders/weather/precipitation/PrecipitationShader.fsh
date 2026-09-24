#version 330 core

in  vec3 v_dir;
in  vec2 v_screenPos;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/PrecipitationData.glsl"
#include "includes/NoiseUtility.glsl"

uniform sampler2D u_sceneDepth;

/*
 * Fullscreen rain and snow. Each view ray crosses a set of cylinders around
 * the camera, nearest first; where it meets one, a pattern of falling
 * streaks or flakes is drawn on that cylinder's surface in coordinates that
 * travel with the drops themselves, so they fall, lean with the wind, and
 * stay anchored to the world as the camera turns. A cylinder is skipped
 * behind the scene depth, so nothing falls in front of terrain it should be
 * behind, and wherever the point lies beneath the top of its block column,
 * so nothing falls through a roof, an overhang, or the ceiling of a cave.
 * Nearer cylinders read larger and faster, farther ones fainter, which gives
 * the fall its depth. Written premultiplied for the composite.
 */

const float PRECIPITATION_EPSILON         = 0.0001;
const float PRECIPITATION_PI              = 3.14159265;
const float PRECIPITATION_TWO_PI          = 6.28318531;
const int   PRECIPITATION_LAYER_COUNT     = 7;
const float PRECIPITATION_NEAR_RADIUS     = 1.2;
const float PRECIPITATION_RADIUS_GROWTH   = 1.65;
const float PRECIPITATION_LAYER_FADE      = 0.85;
const float PRECIPITATION_STEEP_FADE_LOW  = 0.08;
const float PRECIPITATION_STEEP_FADE_HIGH = 0.3;
const float PRECIPITATION_MAX_LEAN        = 3.0;
const float PRECIPITATION_ZENITH_SHARE    = 0.4;
const float PRECIPITATION_NO_SCENE        = 1.0e30;

const float RAIN_COLUMN_COUNT  = 288.0;
const float RAIN_STREAK_WIDTH  = 0.08;
const float RAIN_FALL_SPEED    = 16.0;
const float RAIN_PERIOD_RATIO  = 0.55;
const float RAIN_STREAK_LENGTH = 0.12;
const float RAIN_HEAD_SHARE    = 0.3;
const float RAIN_DENSITY_MIN   = 0.2;
const float RAIN_DENSITY_MAX   = 0.9;
const float RAIN_ALPHA         = 0.45;
const float RAIN_BRIGHTNESS    = 1.1;

const float SNOW_COLUMN_COUNT   = 96.0;
const float SNOW_ROW_ANGLE      = 0.065;
const float SNOW_FALL_SPEED     = 1.6;
const float SNOW_FLAKE_BLOCKS   = 0.014;
const float SNOW_FLAKE_CELL_MAX = 0.2;
const float SNOW_SWAY_COLUMNS   = 0.3;
const float SNOW_SWAY_FREQUENCY = 1.3;
const float SNOW_DENSITY_MIN    = 0.08;
const float SNOW_DENSITY_MAX    = 0.35;
const float SNOW_ALPHA          = 0.8;
const float SNOW_BRIGHTNESS     = 1.5;

// ── Scene ──────────────────────────────────────────────────────────────────

// Distance from the camera to the nearest opaque surface along this pixel.
float resolveSceneDistance(vec2 uv) {
    float depth = texture(u_sceneDepth, uv).r;

    if (depth >= 1.0)
    return PRECIPITATION_NO_SCENE;

    vec4 view = u_inverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);

    return length(view.xyz / view.w);
}

// ── Shelter ────────────────────────────────────────────────────────────────

// Absolute block Y just above the highest block in the column under a point,
// read from the ring-buffered column map.
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
    return position.y < float(resolveColumnTop(position.xz));
}

// ── Rain ───────────────────────────────────────────────────────────────────

// Thin streaks in fixed columns around the cylinder. Column coordinates are
// sheared by the lean, measured from eye level so the shear stays small, so
// each streak follows the drop's slanted path, and the phase down the column
// advances with time in world height so the streaks fall.
float resolveRainLayer(
    float angle, float height, float eyeOffset, float radius, float layerSeed, float lean, float pixelAngle,
    float intensity) {
    float columnScale = RAIN_COLUMN_COUNT / PRECIPITATION_TWO_PI;
    float column      = (angle + lean * eyeOffset / radius) * columnScale;
    float cell        = mod(floor(column), RAIN_COLUMN_COUNT);

    if (hash31(vec3(cell, layerSeed, 1.0)) > mix(RAIN_DENSITY_MIN, RAIN_DENSITY_MAX, intensity))
    return 0.0;

    float center    = 0.2 + 0.6 * hash31(vec3(cell, layerSeed, 2.0));
    float halfWidth = RAIN_STREAK_WIDTH * 0.5;
    float across    = 1.0 - smoothstep(halfWidth, halfWidth + pixelAngle * columnScale, abs(fract(column) - center));

    float period = radius * RAIN_PERIOD_RATIO;
    float along  = fract((height + u_time * RAIN_FALL_SPEED) / period + hash31(vec3(cell, layerSeed, 3.0)));
    float head   = RAIN_STREAK_LENGTH * RAIN_HEAD_SHARE;
    float streak = smoothstep(0.0, head, along) * (1.0 - smoothstep(head, RAIN_STREAK_LENGTH, along));

    return across * streak;
}

// ── Snow ───────────────────────────────────────────────────────────────────

// Soft round flakes scattered one per cell of a grid that falls slowly, each
// row swaying on its own rhythm and the whole fall drifting with the wind.
// The wind's shear is taken back out of the flake's own offset so flakes stay
// round, and a flake never outgrows its cell so none is clipped square.
float resolveSnowLayer(
    float angle, float height, float eyeOffset, float radius, float layerSeed, float lean, float pixelAngle,
    float intensity) {
    float cellAngle     = PRECIPITATION_TWO_PI / SNOW_COLUMN_COUNT;
    float rowCoordinate = (height + u_time * SNOW_FALL_SPEED) / (radius * SNOW_ROW_ANGLE);
    float row           = floor(rowCoordinate);
    float sway          = sin(u_time * SNOW_SWAY_FREQUENCY + hash31(vec3(row, layerSeed, 4.0)) * PRECIPITATION_TWO_PI)
    * SNOW_SWAY_COLUMNS;
    float column        = (angle + lean * eyeOffset / radius) / cellAngle + sway;
    float cell          = mod(floor(column), SNOW_COLUMN_COUNT);

    if (hash31(vec3(cell, row, layerSeed)) > mix(SNOW_DENSITY_MIN, SNOW_DENSITY_MAX, intensity))
    return 0.0;

    vec2 center = 0.25 + 0.5 * vec2(
        hash31(vec3(cell, row, layerSeed + 5.0)),
        hash31(vec3(cell, row, layerSeed + 6.0)));
    float rowOffset = (fract(rowCoordinate) - center.y) * SNOW_ROW_ANGLE;
    vec2  offset    = vec2((fract(column) - center.x) * cellAngle - lean * rowOffset, rowOffset);

    float flakeRadius = min(SNOW_FLAKE_BLOCKS / radius, SNOW_FLAKE_CELL_MAX * min(cellAngle, SNOW_ROW_ANGLE));

    return 1.0 - smoothstep(flakeRadius * 0.5, flakeRadius + pixelAngle, length(offset));
}

// ── Main ───────────────────────────────────────────────────────────────────

void main() {
    vec3  dir        = normalize(v_dir);
    float pixelAngle = length(fwidth(dir));
    float intensity  = u_precipitationState.x;
    float horizontal = length(dir.xz);
    float steepFade  = smoothstep(PRECIPITATION_STEEP_FADE_LOW, PRECIPITATION_STEEP_FADE_HIGH, horizontal);

    if (intensity <= PRECIPITATION_EPSILON || steepFade <= PRECIPITATION_EPSILON)
    discard;

    float sceneDistance = resolveSceneDistance(v_screenPos * 0.5 + 0.5);
    float angle         = atan(dir.z, dir.x) + PRECIPITATION_PI;
    vec2  tangent       = vec2(-dir.z, dir.x) / horizontal;
    float windAcross    = dot(u_precipitationState.zw, tangent);
    float rainLean      = clamp(windAcross / RAIN_FALL_SPEED, -PRECIPITATION_MAX_LEAN, PRECIPITATION_MAX_LEAN);
    float snowLean      = clamp(windAcross / SNOW_FALL_SPEED, -PRECIPITATION_MAX_LEAN, PRECIPITATION_MAX_LEAN);
    float snow          = u_precipitationState.y;

    float coverage   = 0.0;
    float radius     = PRECIPITATION_NEAR_RADIUS;
    float layerAlpha = steepFade;

    for (int layer = 0; layer < PRECIPITATION_LAYER_COUNT; layer++) {
        float t = radius / horizontal;

        if (t > sceneDistance)
        break;

        vec3 position = u_cameraPosition + dir * t;

        if (!isSheltered(position)) {
            float seed      = float(layer);
            float eyeOffset = position.y - u_cameraPosition.y;
            float rain      = snow < 1.0
            ? resolveRainLayer(angle, position.y, eyeOffset, radius, seed, rainLean, pixelAngle, intensity) * RAIN_ALPHA
            : 0.0;
            float flakes    = snow > 0.0
            ? resolveSnowLayer(angle, position.y, eyeOffset, radius, seed, snowLean, pixelAngle, intensity) * SNOW_ALPHA
            : 0.0;

            coverage += (1.0 - coverage) * mix(rain, flakes, snow) * layerAlpha;
        }

        radius     *= PRECIPITATION_RADIUS_GROWTH;
        layerAlpha *= PRECIPITATION_LAYER_FADE;
    }

    if (coverage <= PRECIPITATION_EPSILON)
    discard;

    vec3 skyLight = mix(u_skyHorizonColor, u_skyZenithColor, PRECIPITATION_ZENITH_SHARE);
    vec3 color    = skyLight * mix(RAIN_BRIGHTNESS, SNOW_BRIGHTNESS, snow);

    fragColor = vec4(color * coverage, coverage);
}
