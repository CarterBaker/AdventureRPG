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

// Fullscreen volumetric cloud pass. Each weather-map entry is a horizontal
// slab — a dome-bent altitude (see CloudDome.glsl) plus a vertical
// thickness — raymarched through in a handful of steps for real
// front-to-back volume. Horizontal silhouette, vertical puff profile, and
// lit shading live in CloudVisual.glsl, driven by that entry's own UBO
// values; this file owns the raymarch itself and the dome-bend crossing
// search, given entries arrive nearest-first from WeatherMapBufferSystem.

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

// Dome-crossing search tuning. The curved search only needs to cover
// [0, u_weatherRangeBlocks] — resolveCloudDomeAltitude is a fixed
// y = u_cloudDomeFadeAltitude plane past that, solved exactly instead of
// searched. SEARCH_SEGMENTS brackets a sign change within the curved
// region; BISECTION_STEPS collapses a found bracket to a precise crossing.
// GRAZE_REFINE_STEPS does the equivalent for rays that graze the curved
// region without crossing it. SLOPE_PROBE_BLOCKS sets the finite-
// difference spacing used to measure the crossing's local penetration
// rate, which is what lets the slab's real thickness be projected onto
// the ray. MIN_PENETRATION_RATE guards that projection for near-tangential
// rays.
const int   CLOUD_DOME_SEARCH_SEGMENTS      = 20;
const float CLOUD_DOME_SEARCH_MARGIN_RATIO  = 0.2;
const int   CLOUD_DOME_BISECTION_STEPS      = 6;
const int   CLOUD_DOME_GRAZE_REFINE_STEPS   = 10;
const float CLOUD_DOME_SLOPE_PROBE_BLOCKS   = 4.0;
const float CLOUD_DOME_MIN_PENETRATION_RATE = 0.02;

// A finer, per-raymarch-step height offset layered on top of the one
// fixed-per-entry undulation below, so different clumps within the same
// cloud slot settle at slightly different elevations instead of the
// whole slot reading as one flat sheet.
const float CLOUD_LOCAL_HEIGHT_JITTER_FREQUENCY = 0.004;
const float CLOUD_LOCAL_HEIGHT_JITTER_STRENGTH  = 0.18;

const float CLOUD_VOLUME_STEP_LENGTH_BLOCKS   = 40.0;
const int   CLOUD_VOLUME_STEP_COUNT_MIN       = 2;
const int   CLOUD_VOLUME_STEP_COUNT_MAX       = 8;
const float CLOUD_VOLUME_DITHER_STRENGTH      = 0.65;
const float CLOUD_VOLUME_SELF_SHADOW_STRENGTH = 0.65;

// Cheap ray-vs-footprint-circle test used to skip an entry entirely when
// the view ray could never pass near its footprint. A false accept only
// costs one skipped entry's worth of wasted work; a false reject would
// visibly clip a cloud, so this stays generous.
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

// Signed vertical distance between the ray's height at distance t and this
// entry's continuously-bent dome altitude at that same t. A root of this
// function along t is exactly where the ray crosses the slab's own center
// altitude.
float sampleDomeHeightDelta(vec3 rayDir, float t, float clampedAltitude, float heightUndulation) {
    vec3 pos = u_cameraPosition + rayDir * t;
    float domeY = resolveCloudDomeAltitude(clampedAltitude, length(pos.xz)) + heightUndulation;
    return pos.y - domeY;
}

// Locates where a ray crosses this entry's own continuously-bending slab.
// The curved region [0, u_weatherRangeBlocks] is scanned coarsely for a
// genuine sign change, refined by bisection. Rays that only graze the
// curved band without crossing it — the normal case near the horizon,
// since that's exactly where the dome bends toward tangency with the view
// — are refined with a short ternary search around the closest coarse
// sample instead of accepting the coarse sample's grid position directly.
// If neither finds a crossing, the flat region beyond u_weatherRangeBlocks
// (a fixed y = u_cloudDomeFadeAltitude plane) is solved analytically, so
// the configured fade altitude is always reachable at the horizon instead
// of depending on where a coarse sample happened to fall. Once tCenter is
// resolved, the slab's known thickness is projected onto the ray using
// the crossing's own local penetration rate.
bool resolveCloudSlab(
    vec3 rayDir, float clampedAltitude, float thickness, float heightUndulation,
    out vec3 worldPosMid, out float tNear, out float pathLength) {
    float halfThickness = thickness * 0.5;

    float curvedSearchDistance = min(u_cloudMaxDistance, u_weatherRangeBlocks);
    float segmentLength = max(curvedSearchDistance / float(CLOUD_DOME_SEARCH_SEGMENTS), 0.0001);

    float prevT = 0.0;
    float prevF = sampleDomeHeightDelta(rayDir, 0.0, clampedAltitude, heightUndulation);

    float bracketLo = -1.0;
    float bracketHi = -1.0;
    float bestAbsF  = abs(prevF);
    float bestT     = 0.0;

    for (int s = 1; s <= CLOUD_DOME_SEARCH_SEGMENTS; s++) {
        float t = min(float(s) * segmentLength, curvedSearchDistance);
        float f = sampleDomeHeightDelta(rayDir, t, clampedAltitude, heightUndulation);

        if (abs(f) < bestAbsF) {
            bestAbsF = abs(f);
            bestT = t;
        }

        if (prevF * f < 0.0) {
            bracketLo = prevT;
            bracketHi = t;
            break;
        }

        prevT = t;
        prevF = f;
    }

    float tCenter = 0.0;
    bool foundCrossing = false;

    if (bracketLo >= 0.0) {
        float lo  = bracketLo;
        float hi  = bracketHi;
        float fLo = sampleDomeHeightDelta(rayDir, lo, clampedAltitude, heightUndulation);

        for (int i = 0; i < CLOUD_DOME_BISECTION_STEPS; i++) {
            float mid  = (lo + hi) * 0.5;
            float fMid = sampleDomeHeightDelta(rayDir, mid, clampedAltitude, heightUndulation);

            if (fLo * fMid <= 0.0) {
                hi = mid;
            } else {
                lo = mid;
                fLo = fMid;
            }
        }

        tCenter = (lo + hi) * 0.5;
        foundCrossing = true;
    } else if (bestAbsF <= halfThickness + segmentLength * CLOUD_DOME_SEARCH_MARGIN_RATIO) {
        float lo = max(bestT - segmentLength, 0.0);
        float hi = min(bestT + segmentLength, curvedSearchDistance);

        for (int i = 0; i < CLOUD_DOME_GRAZE_REFINE_STEPS; i++) {
            float m1 = mix(lo, hi, 1.0 / 3.0);
            float m2 = mix(lo, hi, 2.0 / 3.0);
            float absF1 = abs(sampleDomeHeightDelta(rayDir, m1, clampedAltitude, heightUndulation));
            float absF2 = abs(sampleDomeHeightDelta(rayDir, m2, clampedAltitude, heightUndulation));

            if (absF1 < absF2)
            hi = m2;
            else
            lo = m1;
        }

        tCenter = (lo + hi) * 0.5;
        foundCrossing = true;
    }

    // Flat-plane solve for the region beyond the curved search — the
    // dome is exactly y = u_cloudDomeFadeAltitude + heightUndulation out
    // there, so this is a direct ray/plane intersection rather than a
    // search, and always finds the crossing when one exists.
    if (!foundCrossing && curvedSearchDistance < u_cloudMaxDistance && abs(rayDir.y) > 0.0001) {
        float flatPlaneY = u_cloudDomeFadeAltitude + heightUndulation;
        float tPlane = (flatPlaneY - u_cameraPosition.y) / rayDir.y;

        if (tPlane >= curvedSearchDistance && tPlane <= u_cloudMaxDistance) {
            tCenter = tPlane;
            foundCrossing = true;
        }
    }

    if (!foundCrossing)
    return false;

    float tBack = max(tCenter - CLOUD_DOME_SLOPE_PROBE_BLOCKS, 0.0);
    float tFwd  = min(tCenter + CLOUD_DOME_SLOPE_PROBE_BLOCKS, u_cloudMaxDistance);
    float fBack = sampleDomeHeightDelta(rayDir, tBack, clampedAltitude, heightUndulation);
    float fFwd  = sampleDomeHeightDelta(rayDir, tFwd, clampedAltitude, heightUndulation);

    float penetrationRate = max(
        abs(fFwd - fBack) / max(tFwd - tBack, 0.0001),
        CLOUD_DOME_MIN_PENETRATION_RATE);

    float halfPathLength = min(halfThickness / penetrationRate, u_cloudMaxDistance * 0.5);

    tNear = clamp(tCenter - halfPathLength, 0.0, u_cloudMaxDistance);
    float tFar = clamp(tCenter + halfPathLength, 0.0, u_cloudMaxDistance);

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
        // slightly different heights than each other.
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

            // The dome bend itself, re-resolved at THIS sample's own
            // horizontal distance from world center so the slab curves
            // continuously along the ray. localHeightJitter is sampled at
            // this same step's own world position (unlike heightUndulation
            // above, which is one fixed value for the whole entry) so
            // different clumps within this entry's silhouette read as
            // separately floating rather than perfectly flat.
            float stepDistanceFromCenter = length(stepWorldPos.xz);

            float localHeightJitter = gradientNoise2D(
                (stepWorldPos.xz + chunkOffsetBlocks) * CLOUD_LOCAL_HEIGHT_JITTER_FREQUENCY
                + vec2(patternSeed * 17.3, patternSeed * 29.9)) * slabThickness * CLOUD_LOCAL_HEIGHT_JITTER_STRENGTH;

            float stepBentAltitude = resolveCloudDomeAltitude(clampedAltitude, stepDistanceFromCenter)
            + heightUndulation + localHeightJitter;
            float stepSlabBottomY  = stepBentAltitude - slabThickness * 0.5;
            float stepSlabTopY     = stepBentAltitude + slabThickness * 0.5;

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