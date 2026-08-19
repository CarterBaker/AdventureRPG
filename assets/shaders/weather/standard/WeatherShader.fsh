#version 330 core

in vec3 v_dir;
in vec2 v_screenPos;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/SettingsData.glsl"
#include "includes/NoiseUtility.glsl"
#include "weather/includes/CloudDome.glsl"
#include "weather/includes/CloudVisual.glsl"

/*
* Fullscreen volumetric cloud pass. Each weather-map entry is a horizontal
 * slab — a dome-bent altitude (see CloudDome.glsl) plus a vertical
 * thickness — that the view ray is raymarched through in a handful of
 * steps for real front-to-back volume. The dome bend is re-resolved at
 * every individual raymarch sample's own world position rather than once
 * for an entry's whole footprint, which is what actually produces the
 * dome: distance from center changes continuously along a ray, so the
 * slab's altitude curves continuously with it, from an entry's authored
 * altitude near the player down to the shared fade altitude at the edge
 * of weather range. Because the bend is keyed to world position and never
 * to the ray or camera itself, a fixed point in the world always bends to
 * the same altitude regardless of view angle. Horizontal silhouette,
 * vertical puff profile, and lit shading all live in
 * weather/includes/CloudVisual.glsl, driven entirely by that entry's own
 * UBO values; this file owns only the raymarch itself — which slab a ray
 * crosses, how many steps to take through it, and how to composite one
 * entry's result over the next, given entries arrive nearest-first from
 * WeatherMapBufferSystem.
 */

uniform float u_cloudAltitudeMin;
uniform float u_cloudAltitudeMax;
uniform float u_cloudMaxDistance;
uniform vec2  u_weatherDriftDirection;
uniform float u_weatherDriftSpeed;

const float CLOUD_DENSITY_EPSILON         = 0.001;
const float CLOUD_ALPHA_SATURATION_CUTOFF = 0.985;
const float CLOUD_CULL_SAFETY_MARGIN      = 2.2;

const float CLOUD_FAR_FADE_FRACTION = 0.2;

const float CLOUD_DENSITY_OPACITY_SCALE              = 2.0;
const float CLOUD_AMBIENT_THICKNESS_REFERENCE_BLOCKS = 160.0;
const float CLOUD_MIN_SLAB_THICKNESS_BLOCKS          = 4.0;

const float CLOUD_HEIGHT_UNDULATION_FREQUENCY = 0.0012;
const float CLOUD_HEIGHT_UNDULATION_STRENGTH  = 0.35;

// How finely the ray is pre-walked to locate the entry's own bent slab
// before the fine volumetric march runs, and how much slack (as a
// fraction of one segment) each segment's bracket gets so a crossing
// isn't missed between two probe points. See resolveCloudSlab().
const int   CLOUD_DOME_SEARCH_SEGMENTS     = 16;
const float CLOUD_DOME_SEARCH_MARGIN_RATIO = 0.2;

const float CLOUD_VOLUME_STEP_LENGTH_BLOCKS   = 40.0;
const int   CLOUD_VOLUME_STEP_COUNT_MIN       = 2;
const int   CLOUD_VOLUME_STEP_COUNT_MAX       = 8;
const float CLOUD_VOLUME_DITHER_STRENGTH      = 0.65;
const float CLOUD_VOLUME_SELF_SHADOW_STRENGTH = 0.65;

// Cheap ray-vs-footprint-circle test used to skip an entry entirely when the
// view ray could never pass near its footprint. The circle is the entry's
// own bounding box's circumscribed radius inflated by
// CLOUD_CULL_SAFETY_MARGIN — a false accept only costs one skipped entry's
// worth of wasted work, a false reject would visibly clip a cloud, so this
// stays generous.
bool footprintMayBeVisible(vec2 rayOriginXZ, vec2 rayDirXZ, vec2 circleCenter, float circleRadius, float maxDist) {
    float rayDirLenXZ = length(rayDirXZ);

    if (rayDirLenXZ < 0.0001)
    return distance(rayOriginXZ, circleCenter) <= circleRadius;

    vec2  dirNorm   = rayDirXZ / rayDirLenXZ;
    vec2  toCenter  = circleCenter - rayOriginXZ;
    float tClosest  = clamp(dot(toCenter, dirNorm), 0.0, maxDist);
    vec2  closestXZ = rayOriginXZ + dirNorm * tClosest;

    return distance(closestXZ, circleCenter) <= circleRadius;
}

// Locates where a ray can cross this entry's own continuously-bending slab
// before the fine volumetric march runs. A flat bracket spanning straight
// from the authored altitude to the fade altitude is only tight for
// entries that already sit near the fade altitude — for anything that
// bends by hundreds of blocks (which is the whole point of the dome), that
// flat bracket is thousands of blocks long for most view angles, and a
// fixed handful of fine steps spread across it almost never lands inside
// the true, thin slab. This instead walks the ray in fixed segments,
// re-resolving the true bent altitude at each segment's own two endpoints,
// and keeps only the segment(s) where the ray's own height could reach
// that segment's own much tighter local band — so the fine march below
// only ever spends its step budget on the narrow stretch of the ray that
// can actually touch the slab, at any distance from the player.
bool resolveCloudSlab(
    vec3 rayDir, float clampedAltitude, float thickness, float heightUndulation,
    out vec3 worldPosMid, out float tNear, out float pathLength) {
    float halfThickness = thickness * 0.5;
    float segmentLength = u_cloudMaxDistance / float(CLOUD_DOME_SEARCH_SEGMENTS);
    float margin = segmentLength * CLOUD_DOME_SEARCH_MARGIN_RATIO;

    float prevT     = 0.0;
    float prevRayY  = u_cameraPosition.y;
    float prevBentY = resolveCloudDomeAltitude(clampedAltitude, length(u_cameraPosition.xz)) + heightUndulation;

    bool  found     = false;
    float foundNear = 0.0;
    float foundFar  = 0.0;

    for (int s = 1; s <= CLOUD_DOME_SEARCH_SEGMENTS; s++) {
        float t     = float(s) * segmentLength;
        vec3  pos   = u_cameraPosition + rayDir * t;
        float bentY = resolveCloudDomeAltitude(clampedAltitude, length(pos.xz)) + heightUndulation;

        float bandLow  = min(prevBentY, bentY) - halfThickness - margin;
        float bandHigh = max(prevBentY, bentY) + halfThickness + margin;
        float rayLow   = min(prevRayY, pos.y);
        float rayHigh  = max(prevRayY, pos.y);

        if (rayHigh >= bandLow && rayLow <= bandHigh) {
            if (!found)
            foundNear = prevT;
            foundFar = t;
            found = true;
        }

        prevT     = t;
        prevRayY  = pos.y;
        prevBentY = bentY;
    }

    if (!found)
    return false;

    tNear = max(foundNear, 0.0);
    float tFar = min(foundFar, u_cloudMaxDistance);

    if (tNear >= tFar)
    return false;

    pathLength  = tFar - tNear;
    worldPosMid = u_cameraPosition + rayDir * (tNear + pathLength * 0.5);

    return true;
}

void main() {
    int entryCount = min(u_weatherEntryCount, WEATHER_MAP_MAX_ENTRIES);

    if (entryCount == 0)
    discard;

    vec3 rayDir = normalize(v_dir);

    if (u_weatherCloudLayerMaxY > u_weatherCloudLayerMinY) {
        bool belowBand = u_cameraPosition.y < u_weatherCloudLayerMinY && rayDir.y <= 0.0;
        bool aboveBand = u_cameraPosition.y > u_weatherCloudLayerMaxY && rayDir.y >= 0.0;
        if (belowBand || aboveBand)
        discard;
    }

    vec2 chunkOffsetBlocks = vec2(float(u_playerChunkX), float(u_playerChunkZ)) * u_chunkSize;

    vec2  driftDirNorm = u_weatherDriftDirection;
    float driftDirLen  = length(driftDirNorm);
    driftDirNorm = driftDirLen > 0.0001 ? driftDirNorm / driftDirLen : vec2(1.0, 0.0);
    float driftAngle = atan(driftDirNorm.y, driftDirNorm.x);
    float driftSpeed = u_weatherDriftSpeed;

    vec3  accumulatedColor = vec3(0.0);
    float accumulatedAlpha = 0.0;

    for (int i = 0; i < entryCount; i++) {
        if (accumulatedAlpha > CLOUD_ALPHA_SATURATION_CUTOFF)
        break;

        vec4  patternState = u_weatherPatternState[i];
        float intensity = patternState.x;
        float fadeAlpha = patternState.y;
        float rangeFade = patternState.w;

        if (intensity <= CLOUD_DENSITY_EPSILON || fadeAlpha <= CLOUD_DENSITY_EPSILON || rangeFade <= CLOUD_DENSITY_EPSILON)
        continue;

        vec4 shape = u_weatherCloudShape[i];

        if (shape.z <= CLOUD_DENSITY_EPSILON)
        continue;

        vec4  bounds         = u_weatherBounds[i];
        vec2  boxCenter       = (bounds.xy + bounds.zw) * 0.5;
        float boxHalfDiagonal = length(bounds.zw - bounds.xy) * 0.5;

        if (!footprintMayBeVisible(u_cameraPosition.xz, rayDir.xz, boxCenter, boxHalfDiagonal * CLOUD_CULL_SAFETY_MARGIN, u_cloudMaxDistance))
        continue;

        vec4  variance1   = u_weatherCloudVariance1[i];
        float patternSeed = variance1.z;

        float clampedAltitude = clamp(shape.y, u_cloudAltitudeMin, u_cloudAltitudeMax);
        float slabThickness   = max(shape.x, CLOUD_MIN_SLAB_THICKNESS_BLOCKS);

        // Fixed per-pattern jitter, anchored to the entry's own footprint
        // center rather than the ray, so different patterns settle at
        // slightly different heights instead of reading as one flat sheet.
        // Computed once per entry; only the dome bend itself is
        // re-evaluated per raymarch step below.
        float heightUndulation = gradientNoise2D(
            (boxCenter + chunkOffsetBlocks) * CLOUD_HEIGHT_UNDULATION_FREQUENCY
            + vec2(patternSeed * 31.7, patternSeed * 57.1)) * slabThickness * CLOUD_HEIGHT_UNDULATION_STRENGTH;

        vec3  worldPosMid;
        float tNear;
        float pathLength;

        if (!resolveCloudSlab(rayDir, clampedAltitude, slabThickness, heightUndulation, worldPosMid, tNear, pathLength))
        continue;

        float farFadeStart = u_cloudMaxDistance * (1.0 - CLOUD_FAR_FADE_FRACTION);
        float farFade        = 1.0 - smoothstep(farFadeStart, u_cloudMaxDistance, tNear);

        if (farFade <= CLOUD_DENSITY_EPSILON)
        continue;

        vec4 noiseParams    = u_weatherCloudNoise[i];
        vec4 colorScale     = u_weatherCloudColorScale[i];
        vec4 materialParams = u_weatherCloudMaterial[i];
        vec4 variance0      = u_weatherCloudVariance0[i];

        vec2  gradient;
        float shadingBias;
        float horizontalCoverage = resolveCloudCoverage(
            bounds, shape, noiseParams, colorScale, variance0, variance1,
            intensity, worldPosMid, driftDirNorm, driftAngle, driftSpeed,
            gradient, shadingBias);

        if (horizontalCoverage <= CLOUD_DENSITY_EPSILON)
        continue;

        float fullness           = materialParams.y;
        float horizontalPuffTerm = pow(horizontalCoverage, mix(2.2, 0.8, fullness));
        float thicknessNorm      = clamp(shape.x / CLOUD_AMBIENT_THICKNESS_REFERENCE_BLOCKS, 0.0, 1.0);

        int   stepCount       = clamp(int(pathLength / CLOUD_VOLUME_STEP_LENGTH_BLOCKS) + 1,
            CLOUD_VOLUME_STEP_COUNT_MIN, CLOUD_VOLUME_STEP_COUNT_MAX);
        float stepLength      = pathLength / float(stepCount);
        float travelStepRatio = stepLength / max(shape.x, CLOUD_MIN_SLAB_THICKNESS_BLOCKS);
        float ditherSeed      = hash31(vec3(v_screenPos * 371.7, float(i) * 17.1 + 3.0));

        vec3  entryColor     = vec3(0.0);
        float entryRemaining = 1.0;

        for (int s = 0; s < CLOUD_VOLUME_STEP_COUNT_MAX; s++) {
            if (s >= stepCount)
            break;

            if (entryRemaining <= CLOUD_DENSITY_EPSILON)
            break;

            float sampleOffset = (float(s) + 0.5) + (ditherSeed - 0.5) * CLOUD_VOLUME_DITHER_STRENGTH;
            float tStep         = clamp(tNear + sampleOffset * stepLength, tNear, tNear + pathLength);
            vec3  stepWorldPos  = u_cameraPosition + rayDir * tStep;

            // This is the actual dome bend: re-resolved at THIS sample's
            // own horizontal distance from world center, so the slab this
            // step tests against curves continuously along the ray instead
            // of sitting at one flat height for the entry's whole footprint.
            float stepDistanceFromCenter = length(stepWorldPos.xz);
            float stepBentAltitude       = resolveCloudDomeAltitude(clampedAltitude, stepDistanceFromCenter)
            + heightUndulation;
            float stepSlabBottomY        = stepBentAltitude - slabThickness * 0.5;
            float stepSlabTopY           = stepBentAltitude + slabThickness * 0.5;

            float verticalNorm, verticalSign;
            float verticalDensity = resolveCloudVerticalDensity(
                stepWorldPos, stepSlabBottomY, stepSlabTopY, fullness, shadingBias, patternSeed,
                verticalNorm, verticalSign);

            if (verticalDensity <= CLOUD_DENSITY_EPSILON)
            continue;

            float localDensity = horizontalCoverage * verticalDensity;
            float opticalDepth = localDensity * shape.z * CLOUD_DENSITY_OPACITY_SCALE * travelStepRatio;
            float stepTransmittance = exp(-opticalDepth);
            float stepAlpha         = 1.0 - stepTransmittance;

            if (stepAlpha <= CLOUD_DENSITY_EPSILON)
            continue;

            float verticalShape = 1.0 - clamp(verticalNorm, 0.0, 1.0);
            float puffHeight    = clamp(mix(horizontalPuffTerm, verticalShape, 0.5), 0.0, 1.0);
            float lift           = verticalSign < 0.0 ? mix(-0.6, -0.15, fullness) : mix(0.3, 0.75, fullness);

            vec3 fakeNormal = normalize(vec3(
                    gradient.x * (1.0 - puffHeight),
                    max(puffHeight + lift, 0.05),
                    gradient.y * (1.0 - puffHeight)));

            float selfShadow = mix(1.0, entryRemaining, CLOUD_VOLUME_SELF_SHADOW_STRENGTH);
            vec3  stepColor   = shadeCloudStep(rayDir, fakeNormal, puffHeight, selfShadow, thicknessNorm, colorScale, materialParams);

            entryColor     += stepColor * stepAlpha * entryRemaining;
            entryRemaining *= stepTransmittance;
        }

        float entryAlpha = clamp((1.0 - entryRemaining) * fadeAlpha * rangeFade * farFade, 0.0, 1.0);

        if (entryAlpha <= CLOUD_DENSITY_EPSILON)
        continue;

        vec3  entryStraightColor = entryColor / max(1.0 - entryRemaining, 0.0001);
        float remaining           = 1.0 - accumulatedAlpha;

        accumulatedColor += entryStraightColor * entryAlpha * remaining;
        accumulatedAlpha += entryAlpha * remaining;
    }

    if (accumulatedAlpha <= 0.003)
    discard;

    vec3 straightColor = accumulatedColor / max(accumulatedAlpha, 0.0001);
    fragColor = vec4(straightColor, accumulatedAlpha);
}