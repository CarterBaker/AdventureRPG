#ifndef CLOUD_MARCH_GLSL
#define CLOUD_MARCH_GLSL

#include "includes/CameraData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/WeatherMapUtility.glsl"
#include "weather/includes/CloudDome.glsl"
#include "weather/includes/CloudVisual.glsl"

/*
 * Finds where a ray from the camera first meets each cloud layer, out to a
 * given distance, and composites those surfaces front to back. A layer is a
 * column of cloud over its shape field (WeatherMapUtility), so nothing is
 * integrated along the ray: it steps through the layer's shell reading only
 * the cheap, unbumped column, which always contains the true cloud, until it
 * lands inside one; there it checks the bumped column, bisects onto the true
 * surface, and shades that one point with its own normal. A layer whose cloud
 * is opaque ends the ray, so every layer behind it is only marched up to that
 * cloud, and a translucent sheet lets the layers behind it show through. Steps
 * lengthen with distance, since far cloud covers fewer pixels per block, and
 * the first step is offset per pixel by a share of the caller's jitter: enough
 * that neighbouring rows never sample identical depths, little enough that a
 * cloud's outline stays one crisp edge rather than a dither of hits and
 * misses. The sky march runs in the weather pass at reduced resolution with
 * bumps and surface normals. The fog march runs per terrain fragment in the
 * lighting pass, only where cloud stands between the camera and the fragment,
 * with a few steps, no bumps and the planet's up as its normal. The engine
 * prepends every include to every stage of a program, so nothing here touches
 * fragment-only built-ins; callers pass the jitter in.
 */

struct CloudMarchQuality {
    int  stepBudget;
    int  octaveLimit;
    int  refineSteps;
    bool bumps;
    bool surfaceNormal;
};

const CloudMarchQuality CLOUD_MARCH_SKY = CloudMarchQuality(64, 3, 6, true, true);
const CloudMarchQuality CLOUD_MARCH_FOG = CloudMarchQuality(8, 2, 2, false, false);

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
const float CLOUD_MARCH_OPAQUE_ALPHA           = 0.98;
const float CLOUD_MARCH_UNBOUNDED_DISTANCE     = 1.0e30;

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

int resolveCloudMarchOctaves(int layer, CloudMarchQuality quality, float t) {
    return min(resolveCloudOctaves(resolveCloudLayerFeatureSize(layer), t), quality.octaveLimit);
}

// Coarse (x) and fine (y) bump strength at a distance, none at all for a
// quality without bumps.
vec2 resolveCloudMarchBumpFade(int layer, CloudMarchQuality quality, float t) {
    if (!quality.bumps)
    return vec2(0.0);

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
CloudColumn sampleCloudMarchColumn(
    int layer, CloudMarchQuality quality, vec3 rayDir, float t, bool bumped, out float heightFraction) {
    vec3 relativePosition = rayDir * t;

    heightFraction = resolveCloudLayerHeightFraction(layer, relativePosition);

    if (heightFraction < 0.0 || heightFraction > 1.0)
    return CLOUD_COLUMN_EMPTY;

    return resolveCloudColumn(
        layer,
        u_cameraPosition.xz + relativePosition.xz,
        resolveCloudMarchOctaves(layer, quality, t),
        bumped ? resolveCloudMarchBumpFade(layer, quality, t) : vec2(0.0));
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
bool findCloudLayerSurface(
    int layer, CloudMarchQuality quality, vec3 rayDir, float tEnter, float tExit, float jitter,
    out CloudHit hit) {
    float stepTarget = resolveCloudLayerStepTarget(layer);
    float stepCeil   = resolveCloudLayerFeatureSize(layer) * CLOUD_MARCH_FAR_STEP_FEATURE_RATIO;
    float tOutside   = tEnter;
    float t          = tEnter + stepTarget * jitter * CLOUD_MARCH_JITTER_SCALE;
    bool  refined    = false;

    hit = CloudHit(tExit, 0.0, CLOUD_COLUMN_EMPTY);

    for (int s = 0; s < quality.stepBudget; s++) {
        if (t > tExit)
        break;

        float       heightFraction;
        CloudColumn column = sampleCloudMarchColumn(layer, quality, rayDir, t, false, heightFraction);
        bool        inside = isInsideCloudColumn(column, heightFraction);

        if (inside && quality.bumps) {
            column  = sampleCloudMarchColumn(layer, quality, rayDir, t, true, heightFraction);
            refined = true;
        } else if (!inside) {
            refined = false;
        }

        if (isInsideCloudColumn(column, heightFraction)) {
            hit = CloudHit(t, heightFraction, column);

            for (int r = 0; r < quality.refineSteps; r++) {
                float       tMiddle = (tOutside + hit.t) * 0.5;
                float       middleFraction;
                CloudColumn middle  = sampleCloudMarchColumn(layer, quality, rayDir, tMiddle, true, middleFraction);

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
float sampleCloudColumnDepth(int layer, vec2 positionXZ, int octaves, vec2 bumpFade, float heightFraction) {
    return resolveCloudColumnDepth(resolveCloudColumn(layer, positionXZ, octaves, bumpFade), heightFraction);
}

// Outward normal of a cloud's surface at a hit: the direction in which the
// hit's depth inside the cloud falls fastest. Across the ground it is read
// from the hit's own column and one beside it on each axis, at the bump scale;
// upward it falls by one per block toward the crown and rises by one toward
// the underside. A wall standing over open sky drops sharply sideways, so
// walls face outward rather than splitting into a lit crown and a shaded
// underside. The normal is turned onto the planet's local up.
vec3 resolveCloudSurfaceNormal(int layer, CloudMarchQuality quality, vec3 rayDir, CloudHit hit) {
    vec3 relativePosition = rayDir * hit.t;
    vec3 up               = resolveCloudDomeNormal(relativePosition);

    if (!quality.surfaceNormal)
    return up;

    float offset   = resolveCloudLayerBumpSize(layer) * CLOUD_MARCH_NORMAL_OFFSET_RATIO;
    vec2  position = u_cameraPosition.xz + relativePosition.xz;
    int   octaves  = resolveCloudMarchOctaves(layer, quality, hit.t);
    vec2  bumpFade = resolveCloudMarchBumpFade(layer, quality, hit.t);
    float height   = hit.heightFraction;

    float depth = resolveCloudColumnDepth(hit.column, height);
    float east  = sampleCloudColumnDepth(layer, position + vec2(offset, 0.0), octaves, bumpFade, height);
    float south = sampleCloudColumnDepth(layer, position + vec2(0.0, offset), octaves, bumpFade, height);

    vec2  slope    = vec2(east - depth, south - depth) * resolveCloudLayerThickness(layer) / offset;
    bool  crown    = hit.column.top - height < height - hit.column.bottom;
    float vertical = crown ? -1.0 : 1.0;

    return normalize(-(vec3(slope.x, 0.0, slope.y) + up * vertical));
}

// ── March ──────────────────────────────────────────────────────────────────

// Each layer's first surface along the ray, shaded once and kept in order of
// distance, then composited front to back through one running
// transmittance. Layers are marched only up to the nearest opaque cloud
// found so far, and the light is resolved only once a ray meets cloud.
void integrateCloudLayers(
    vec3 rayDir, float maxDistance, CloudMarchQuality quality, float jitter,
    inout vec3 color, inout float transmittance) {
    int   layerCount       = min(u_weatherLayerCount, WEATHER_MAP_MAX_LAYERS);
    float limit            = min(maxDistance, resolveCloudDomeGroundDistance(rayDir));
    float horizontalLength = length(rayDir.xz);

    if (horizontalLength > CLOUD_MARCH_EPSILON)
    limit = min(limit, resolveWeatherMapReach() / horizontalLength);

    float      hitDistance[WEATHER_MAP_MAX_LAYERS];
    vec3       hitColor[WEATHER_MAP_MAX_LAYERS];
    float      hitAlpha[WEATHER_MAP_MAX_LAYERS];
    int        hitCount      = 0;
    CloudLight light;
    bool       lightResolved = false;

    for (int layer = 0; layer < layerCount; layer++) {
        float baseAltitude = resolveCloudLayerBaseAltitude(layer);
        float tEnter;
        float tExit;

        if (!resolveCloudDomeInterval(
            baseAltitude, baseAltitude + resolveCloudLayerThickness(layer), rayDir, limit, tEnter, tExit))
        continue;

        CloudHit hit;

        if (!findCloudLayerSurface(layer, quality, rayDir, tEnter, tExit, jitter, hit))
        continue;

        if (!lightResolved) {
            light         = resolveCloudLight(rayDir);
            lightResolved = true;
        }

        CloudColumn column       = hit.column;
        float       distance     = length(rayDir.xz) * hit.t;
        float       alpha        = column.alpha * resolveCloudDistanceOpacity(distance);
        float       columnHeight = (hit.heightFraction - column.bottom)
        / max(column.top - column.bottom, CLOUD_MARCH_EPSILON);
        vec3        shaded       = shadeCloudSurface(
            light, resolveCloudLayerAlbedo(layer), resolveCloudSurfaceNormal(layer, quality, rayDir, hit), rayDir,
            clamp(columnHeight, 0.0, 1.0), column.translucency, column.crease, u_weatherLayerShape[layer].w,
            distance);

        int slot = hitCount;

        while (slot > 0 && hitDistance[slot - 1] > hit.t) {
            hitDistance[slot] = hitDistance[slot - 1];
            hitColor[slot]    = hitColor[slot - 1];
            hitAlpha[slot]    = hitAlpha[slot - 1];
            slot--;
        }

        hitDistance[slot] = hit.t;
        hitColor[slot]    = shaded;
        hitAlpha[slot]    = alpha;
        hitCount++;

        if (alpha >= CLOUD_MARCH_OPAQUE_ALPHA)
        limit = min(limit, hit.t);
    }

    for (int i = 0; i < hitCount; i++) {
        if (hitDistance[i] > limit)
        break;

        color         += hitColor[i] * hitAlpha[i] * transmittance;
        transmittance *= 1.0 - hitAlpha[i];
    }
}

// The sky seen along a view ray, out to the edge of the weather map.
void integrateCloudSky(vec3 rayDir, float jitter, inout vec3 color, inout float transmittance) {
    integrateCloudLayers(rayDir, CLOUD_MARCH_UNBOUNDED_DISTANCE, CLOUD_MARCH_SKY, jitter, color, transmittance);
}

// Cloud standing between the camera and a surface fragmentDistance away.
void integrateCloudFog(
    vec3 rayDir, float fragmentDistance, float jitter, inout vec3 color, inout float transmittance) {
    integrateCloudLayers(rayDir, fragmentDistance, CLOUD_MARCH_FOG, jitter, color, transmittance);
}

#endif
