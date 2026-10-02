#ifndef CLOUD_MARCH_GLSL
#define CLOUD_MARCH_GLSL

#include "includes/CameraData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/WeatherMapUtility.glsl"
#include "weather/includes/CloudDome.glsl"
#include "weather/includes/CloudVisual.glsl"

/*
 * Finds where a ray from the camera first meets solid cloud, and the fog
 * around a camera standing inside one. A layer is a column of cloud over its
 * shape field (WeatherMapUtility), so nothing is integrated along the ray: it
 * steps through the layer's shell reading only the cheap, unbumped column,
 * which always contains the true cloud, until it lands inside one; there it
 * checks the bumped column and bisects onto the true surface. Every cloud is
 * opaque, so the nearest surface across all layers is the only one seen: each
 * layer is marched only up to the nearest surface found so far, and only that
 * one surface is shaded, with its own normal. Steps lengthen with distance,
 * since far cloud covers fewer pixels per block, and the first step is offset
 * per pixel by a share of the caller's jitter: enough that neighbouring rows
 * never sample identical depths, little enough that a cloud's outline stays
 * one crisp edge rather than a dither of hits and misses. A camera inside a
 * cloud sees no surfaces at all, only the cloud's fog, read from the same
 * bumped column the surface is drawn from, so the fog starts exactly where
 * the camera passes through the surface. The engine prepends every include to
 * every stage of a program, so nothing here touches fragment-only built-ins;
 * callers pass the jitter in.
 */

const int   CLOUD_MARCH_STEP_BUDGET  = 64;
const int   CLOUD_MARCH_REFINE_STEPS = 5;

const float CLOUD_MARCH_EPSILON                = 0.001;
const float CLOUD_MARCH_MIN_THICKNESS_BLOCKS   = 1.0;
const float CLOUD_MARCH_STEP_THICKNESS_RATIO   = 0.15;
const float CLOUD_MARCH_STEP_FEATURE_RATIO     = 0.5;
const float CLOUD_MARCH_DISTANCE_STEP_RATIO    = 0.02;
const float CLOUD_MARCH_FAR_STEP_FEATURE_RATIO = 1.0;
const float CLOUD_MARCH_REFINE_RATIO           = 0.35;
const float CLOUD_MARCH_THIN_RATIO             = 0.5;
const float CLOUD_MARCH_GAP_STEP_RATIO         = 2.0;
const float CLOUD_MARCH_MIN_STEP_RATIO         = 0.1;
const float CLOUD_MARCH_JITTER_SCALE           = 0.25;
const float CLOUD_MARCH_NORMAL_OFFSET_RATIO    = 0.2;
const float CLOUD_MARCH_UNBOUNDED_DISTANCE     = 1.0e30;

const float CLOUD_FOG_DENSITY_PER_BLOCK  = 0.15;
const float CLOUD_FOG_EDGE_DENSITY_SHARE = 0.3;
const float CLOUD_FOG_EDGE_BLOCKS        = 4.0;

// ── Layers ─────────────────────────────────────────────────────────────────

float resolveCloudLayerBaseAltitude(int layer) {
    return u_weatherLayerShape[layer].x - u_weatherPlanet.y;
}

float resolveCloudLayerThickness(int layer) {
    return max(u_weatherLayerShape[layer].y, CLOUD_MARCH_MIN_THICKNESS_BLOCKS);
}

// The step a layer needs near the camera to resolve both its depth and its
// clouds.
float resolveCloudLayerStepTarget(int layer) {
    return min(
        resolveCloudLayerThickness(layer) * CLOUD_MARCH_STEP_THICKNESS_RATIO,
        resolveCloudLayerFeatureSize(layer) * CLOUD_MARCH_STEP_FEATURE_RATIO);
}

// Coarse (x) and fine (y) bump strength at a distance.
vec2 resolveCloudMarchBumpFade(int layer, float t) {
    float bumpSize = resolveCloudLayerBumpSize(layer);

    return vec2(
        resolveCloudDetailFade(bumpSize, t),
        resolveCloudDetailFade(bumpSize / CLOUD_LAYER_BUMP_FINE_RATIO, t));
}

// ── Surface ────────────────────────────────────────────────────────────────

// Where a ray first meets a layer's cloud: distance along the ray, the
// point's height fraction in the layer, and the column it stands in.
struct CloudHit {
    float       t;
    float       heightFraction;
    CloudColumn column;
};

float resolveCloudLayerHeightFraction(int layer, vec3 relativePosition) {
    return (resolveCloudDomeAltitude(relativePosition) - resolveCloudLayerBaseAltitude(layer))
    / resolveCloudLayerThickness(layer);
}

// The column under a point on the ray, bumped or cheap. Points outside the
// layer's height never touch the noise.
CloudColumn sampleCloudMarchColumn(int layer, vec3 rayDir, float t, bool bumped, out float heightFraction) {
    vec3 relativePosition = rayDir * t;

    heightFraction = resolveCloudLayerHeightFraction(layer, relativePosition);

    if (heightFraction < 0.0 || heightFraction > 1.0)
    return CLOUD_COLUMN_EMPTY;

    return resolveCloudColumn(
        layer,
        u_cameraPosition.xz + relativePosition.xz,
        bumped ? resolveCloudMarchBumpFade(layer, t) : vec2(0.0));
}

// The longest step that cannot carry the ray past the column it stands
// over. Outside the column the step follows the gap to it — exactly the
// climb to a flat base, and at most a few gaps across toward a rising crown —
// so a ray slows only as it nears a surface. Inside, it may cross only part
// of the column's own thickness. Open sky imposes no limit.
float resolveCloudColumnSafeStep(int layer, vec3 rayDir, float t, CloudColumn column, float heightFraction) {
    if (column.top <= column.bottom)
    return CLOUD_MARCH_UNBOUNDED_DISTANCE;

    float thickness = resolveCloudLayerThickness(layer);
    float gap       = max(column.bottom - heightFraction, heightFraction - column.top);
    float climb     = max(abs(dot(rayDir, resolveCloudDomeNormal(rayDir * t))), CLOUD_MARCH_EPSILON);

    if (gap > 0.0)
    return gap * thickness * min(1.0 / climb, CLOUD_MARCH_GAP_STEP_RATIO);

    return (column.top - column.bottom) * CLOUD_MARCH_THIN_RATIO * thickness / climb;
}

// Steps the cheap column through the layer's stretch of the ray. Wherever it
// holds the point, the bumped column decides, and the march takes fine steps
// until it leaves the cheap column again, so a bump's crest is never stepped
// over. On a hit the surface is bisected between the last point outside and
// the first point inside.
bool findCloudLayerSurface(int layer, vec3 rayDir, float tEnter, float tExit, float jitter, out CloudHit hit) {
    float stepTarget = resolveCloudLayerStepTarget(layer);
    float stepCeil   = resolveCloudLayerFeatureSize(layer) * CLOUD_MARCH_FAR_STEP_FEATURE_RATIO;
    float tOutside   = tEnter;
    float t          = tEnter + stepTarget * jitter * CLOUD_MARCH_JITTER_SCALE;
    bool  refined    = false;

    hit = CloudHit(tExit, 0.0, CLOUD_COLUMN_EMPTY);

    for (int s = 0; s < CLOUD_MARCH_STEP_BUDGET; s++) {
        if (t > tExit)
        break;

        float       heightFraction;
        CloudColumn column = sampleCloudMarchColumn(layer, rayDir, t, false, heightFraction);
        bool        inside = isInsideCloudColumn(column, heightFraction);

        if (inside) {
            column  = sampleCloudMarchColumn(layer, rayDir, t, true, heightFraction);
            refined = true;
        } else {
            refined = false;
        }

        if (isInsideCloudColumn(column, heightFraction)) {
            hit = CloudHit(t, heightFraction, column);

            for (int r = 0; r < CLOUD_MARCH_REFINE_STEPS; r++) {
                float       tMiddle = (tOutside + hit.t) * 0.5;
                float       middleFraction;
                CloudColumn middle  = sampleCloudMarchColumn(layer, rayDir, tMiddle, true, middleFraction);

                if (isInsideCloudColumn(middle, middleFraction))
                hit = CloudHit(tMiddle, middleFraction, middle);
                else
                tOutside = tMiddle;
            }

            return true;
        }

        float stepLength = clamp(t * CLOUD_MARCH_DISTANCE_STEP_RATIO, stepTarget, max(stepCeil, stepTarget));
        stepLength = min(stepLength, resolveCloudColumnSafeStep(layer, rayDir, t, column, heightFraction));

        if (refined)
        stepLength *= CLOUD_MARCH_REFINE_RATIO;

        tOutside = t;
        t       += max(stepLength, stepTarget * CLOUD_MARCH_MIN_STEP_RATIO);
    }

    return false;
}

// How far inside the column beside a hit the hit's own height lies.
float sampleCloudColumnDepth(int layer, vec2 positionXZ, vec2 bumpFade, float heightFraction) {
    return resolveCloudColumnDepth(resolveCloudColumn(layer, positionXZ, bumpFade), heightFraction);
}

// Outward normal of a cloud's surface at a hit: the direction in which the
// hit's depth inside the cloud falls fastest. Across the ground it is read
// from the hit's own column and one beside it on each axis, at the bump scale;
// upward it falls by one per block toward the crown and rises by one toward
// the underside. A wall standing over open sky drops sharply sideways, so
// walls face outward rather than splitting into a lit crown and a shaded
// underside. The normal is turned onto the planet's local up.
vec3 resolveCloudSurfaceNormal(int layer, vec3 rayDir, CloudHit hit) {
    vec3  relativePosition = rayDir * hit.t;
    vec3  up               = resolveCloudDomeNormal(relativePosition);
    float offset           = resolveCloudLayerBumpSize(layer) * CLOUD_MARCH_NORMAL_OFFSET_RATIO;
    vec2  position         = u_cameraPosition.xz + relativePosition.xz;
    vec2  bumpFade         = resolveCloudMarchBumpFade(layer, hit.t);
    float height           = hit.heightFraction;

    float depth = resolveCloudColumnDepth(hit.column, height);
    float east  = sampleCloudColumnDepth(layer, position + vec2(offset, 0.0), bumpFade, height);
    float south = sampleCloudColumnDepth(layer, position + vec2(0.0, offset), bumpFade, height);

    vec2  slope    = vec2(east - depth, south - depth) * resolveCloudLayerThickness(layer) / offset;
    bool  crown    = hit.column.top - height < height - hit.column.bottom;
    float vertical = crown ? -1.0 : 1.0;

    return normalize(-(vec3(slope.x, 0.0, slope.y) + up * vertical));
}

// ── March ──────────────────────────────────────────────────────────────────

// The nearest cloud surface along the ray across every layer, out to the
// edge of the weather map or the world's ground. Each layer is marched only
// up to the nearest surface found so far.
bool findNearestCloudSurface(vec3 rayDir, float jitter, out int hitLayer, out CloudHit hit) {
    int   layerCount       = min(u_weatherLayerCount, WEATHER_MAP_MAX_LAYERS);
    float limit            = resolveCloudDomeGroundDistance(rayDir);
    float horizontalLength = length(rayDir.xz);

    if (horizontalLength > CLOUD_MARCH_EPSILON)
    limit = min(limit, resolveWeatherMapReach() / horizontalLength);

    hitLayer = -1;
    hit      = CloudHit(limit, 0.0, CLOUD_COLUMN_EMPTY);

    for (int layer = 0; layer < layerCount; layer++) {
        float baseAltitude = resolveCloudLayerBaseAltitude(layer);
        float tEnter;
        float tExit;

        if (!resolveCloudDomeInterval(
            baseAltitude, baseAltitude + resolveCloudLayerThickness(layer), rayDir, limit, tEnter, tExit))
        continue;

        CloudHit layerHit;

        if (!findCloudLayerSurface(layer, rayDir, tEnter, tExit, jitter, layerHit))
        continue;

        hitLayer = layer;
        hit      = layerHit;
        limit    = layerHit.t;
    }

    return hitLayer >= 0;
}

// Painted color of a surface findNearestCloudSurface found.
vec3 shadeCloudHit(vec3 rayDir, int layer, CloudHit hit) {
    CloudColumn column       = hit.column;
    float       columnHeight = (hit.heightFraction - column.bottom)
    / max(column.top - column.bottom, CLOUD_MARCH_EPSILON);

    return shadeCloudSurface(
        resolveCloudLight(rayDir), resolveCloudLayerAlbedo(layer), resolveCloudSurfaceNormal(layer, rayDir, hit),
        clamp(columnHeight, 0.0, 1.0), column.crease, u_weatherLayerShape[layer].w, length(rayDir.xz) * hit.t);
}

// ── Fog ────────────────────────────────────────────────────────────────────

// Density per block of the fog around a camera standing inside a cloud, and
// its color; 0 when the camera stands in open air. The fog thins toward the
// cloud's base and crown, so climbing into a cloud thickens it over a few
// blocks rather than at once.
float resolveCloudFog(out vec3 fogColor) {
    int   layerCount     = min(u_weatherLayerCount, WEATHER_MAP_MAX_LAYERS);
    float cameraAltitude = resolveCloudDomeCameraAltitude();

    fogColor = vec3(0.0);

    for (int layer = 0; layer < layerCount; layer++) {
        float thickness      = resolveCloudLayerThickness(layer);
        float heightFraction = (cameraAltitude - resolveCloudLayerBaseAltitude(layer)) / thickness;

        if (heightFraction < 0.0 || heightFraction > 1.0)
        continue;

        CloudColumn column = resolveCloudColumn(layer, u_cameraPosition.xz, resolveCloudMarchBumpFade(layer, 0.0));

        if (!isInsideCloudColumn(column, heightFraction))
        continue;

        float depthBlocks = resolveCloudColumnDepth(column, heightFraction) * thickness;
        float edge        = mix(CLOUD_FOG_EDGE_DENSITY_SHARE, 1.0, smoothstep(0.0, CLOUD_FOG_EDGE_BLOCKS, depthBlocks));

        fogColor = shadeCloudFog(resolveCloudLight(vec3(0.0, 1.0, 0.0)), resolveCloudLayerAlbedo(layer));

        return CLOUD_FOG_DENSITY_PER_BLOCK * u_weatherLayerShape[layer].z * edge;
    }

    return 0.0;
}

#endif
