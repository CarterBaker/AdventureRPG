#version 330 core

in vec3 v_dir;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/NoiseUtility.glsl"
#include "weather/includes/CloudDome.glsl"
#include "weather/includes/CloudVisual.glsl"

/*
 * Fullscreen volumetric cloud pass. Each weather-map entry is a horizontal
 * slab — a dome-bent altitude plus a vertical thickness — and this file finds
 * where the view ray actually enters and leaves that slab by solving the two
 * boundary crossings, not by projecting a thickness around the centre
 * crossing. Using the real entry point is what lets a ray aimed at a cloud's
 * crown saturate near its top while a ray aimed lower enters at the base, so
 * the archetype's vertical profile becomes a visible silhouette from the side
 * instead of being averaged into the same flat stamp at every angle. Steps are
 * placed deterministically with no screen-space jitter, and everything is
 * sampled through CloudVisual's angular-size LOD, so the image is stable under
 * camera rotation rather than crawling with the screen.
 */

uniform float u_cloudAltitudeMin;
uniform float u_cloudAltitudeMax;
uniform float u_cloudMaxDistance;
uniform vec2  u_weatherDriftDirection;

const float CLOUD_DENSITY_EPSILON         = 0.001;
const float CLOUD_ALPHA_SATURATION_CUTOFF = 0.985;
const float CLOUD_CULL_SAFETY_MARGIN      = 2.2;
const float CLOUD_MIN_SLAB_THICKNESS      = 4.0;

const int   CLOUD_SLAB_BISECTION_STEPS = 16;
const float CLOUD_DENSITY_OPACITY_SCALE = 2.2;
const float CLOUD_AMBIENT_THICKNESS_REFERENCE_BLOCKS = 160.0;

const float CLOUD_STEP_FEATURE_RATIO    = 0.55;
const int   CLOUD_VOLUME_STEP_COUNT_MIN = 3;
const int   CLOUD_VOLUME_STEP_COUNT_MAX = 10;
const float CLOUD_VOLUME_SELF_SHADOW_STRENGTH = 0.7;

const float CLOUD_HAZE_START_FRACTION = 0.18;
const float CLOUD_HAZE_STRENGTH       = 0.9;
const float CLOUD_FAR_FADE_FRACTION   = 0.12;

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

// Signed vertical offset of the ray from this entry's bent slab centre.
float sampleSlabOffset(vec3 rayDir, float t, float authoredAltitude, float heightOffset) {
    vec3 pos = u_cameraPosition + rayDir * t;
    return pos.y - (resolveCloudDomeAltitude(authoredAltitude, length(pos.xz)) + heightOffset);
}

float bisectSlabOffset(
    vec3 rayDir, float authoredAltitude, float heightOffset, float target, float lo, float hi) {
    float fLo = sampleSlabOffset(rayDir, lo, authoredAltitude, heightOffset) - target;

    for (int i = 0; i < CLOUD_SLAB_BISECTION_STEPS; i++) {
        float mid  = (lo + hi) * 0.5;
        float fMid = sampleSlabOffset(rayDir, mid, authoredAltitude, heightOffset) - target;

        if (fLo * fMid <= 0.0) {
            hi = mid;
        } else {
            lo  = mid;
            fLo = fMid;
        }
    }

    return (lo + hi) * 0.5;
}

bool entryBandMayBeReached(vec3 rayDir, float authoredAltitude, float slabThickness) {
    float reach = slabThickness * (0.5 + u_weatherHeightVariation.x + u_weatherHeightVariation.y);

    float bandMin = min(authoredAltitude, u_cloudDomeFadeAltitude) - reach;
    float bandMax = max(authoredAltitude, u_cloudDomeFadeAltitude) + reach;

    if (u_cameraPosition.y < bandMin && rayDir.y <= 0.0)
    return false;

    if (u_cameraPosition.y > bandMax && rayDir.y >= 0.0)
    return false;

    return true;
}

// The slab offset is monotonic along the ray wherever the dome falls no
// faster than the ray descends, which covers every view except a shallow
// downward one, so the entry and exit boundaries are solved directly from the
// two endpoints with no tolerance band and no fabricated crossing.
bool resolveCloudSlabInterval(
    vec3 rayDir, float authoredAltitude, float thickness, float heightOffset,
    out float tEnter, out float tExit) {
    float halfThickness = thickness * 0.5;
    float maxT          = u_cloudMaxDistance;

    float hNear = sampleSlabOffset(rayDir, 0.0, authoredAltitude, heightOffset);
    float hFar  = sampleSlabOffset(rayDir, maxT, authoredAltitude, heightOffset);

    bool insideNear = abs(hNear) <= halfThickness;
    bool insideFar  = abs(hFar) <= halfThickness;

    if (!insideNear && !insideFar && (hNear > halfThickness) == (hFar > halfThickness))
    return false;

    bool  rising      = hFar >= hNear;
    float enterTarget = rising ? -halfThickness : halfThickness;
    float exitTarget  = rising ? halfThickness : -halfThickness;

    tEnter = insideNear
    ? 0.0
    : bisectSlabOffset(rayDir, authoredAltitude, heightOffset, enterTarget, 0.0, maxT);

    tExit = insideFar
    ? maxT
    : bisectSlabOffset(rayDir, authoredAltitude, heightOffset, exitTarget, tEnter, maxT);

    return tExit > tEnter;
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

    vec2  orientationDir = u_weatherDriftDirection;
    float orientationLen = length(orientationDir);
    orientationDir = orientationLen > 0.0001 ? orientationDir / orientationLen : vec2(1.0, 0.0);

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

        float authoredAltitude = clamp(shape.y, u_cloudAltitudeMin, u_cloudAltitudeMax);
        float slabThickness    = max(shape.x, CLOUD_MIN_SLAB_THICKNESS);

        if (!entryBandMayBeReached(rayDir, authoredAltitude, slabThickness))
        continue;

        vec4  bounds          = u_weatherBounds[i];
        vec2  boxCenter       = (bounds.xy + bounds.zw) * 0.5;
        float boxHalfDiagonal = length(bounds.zw - bounds.xy) * 0.5;

        if (!footprintMayBeVisible(u_cameraPosition.xz, rayDir.xz, boxCenter, boxHalfDiagonal * CLOUD_CULL_SAFETY_MARGIN, u_cloudMaxDistance))
        continue;

        vec4  variance1   = u_weatherCloudVariance1[i];
        float patternSeed = variance1.z;

        float patternHeightOffset = (hash31(vec3(
                    patternSeed * 73.1,
                    variance1.y * 41.7,
                    patternSeed + 11.3)) - 0.5) * 2.0
        * slabThickness * u_weatherHeightVariation.x;

        float tEnter;
        float tExit;

        if (!resolveCloudSlabInterval(rayDir, authoredAltitude, slabThickness, patternHeightOffset, tEnter, tExit))
        continue;

        float farFadeStart = u_cloudMaxDistance * (1.0 - CLOUD_FAR_FADE_FRACTION);
        float farFade      = 1.0 - smoothstep(farFadeStart, u_cloudMaxDistance, tEnter);

        if (farFade <= CLOUD_DENSITY_EPSILON)
        continue;

        vec4 noiseParams    = u_weatherCloudNoise[i];
        vec4 colorScale     = u_weatherCloudColorScale[i];
        vec4 materialParams = u_weatherCloudMaterial[i];
        vec4 variance0      = u_weatherCloudVariance0[i];

        float fullness      = clamp(materialParams.y, 0.0, 1.0);
        float thicknessNorm = clamp(shape.x / CLOUD_AMBIENT_THICKNESS_REFERENCE_BLOCKS, 0.0, 1.0);

        // Steps are sized to the archetype's own feature size, so the march
        // resolves individual lobes rather than skipping between them, and the
        // total marched distance is bounded because opacity saturates at the
        // front of the cloud long before a grazing interval ends.
        float desiredStep = max(colorScale.w, 16.0) * CLOUD_STEP_FEATURE_RATIO;
        float pathLength  = tExit - tEnter;

        int   stepCount  = clamp(int(pathLength / desiredStep) + 1,
            CLOUD_VOLUME_STEP_COUNT_MIN, CLOUD_VOLUME_STEP_COUNT_MAX);
        float stepLength = min(pathLength / float(stepCount), desiredStep);

        float travelStepRatio = stepLength / slabThickness;

        vec3  entryColor     = vec3(0.0);
        float entryRemaining = 1.0;

        for (int s = 0; s < CLOUD_VOLUME_STEP_COUNT_MAX; s++) {
            if (s >= stepCount)
            break;

            if (entryRemaining <= CLOUD_DENSITY_EPSILON)
            break;

            float tStep        = tEnter + (float(s) + 0.5) * stepLength;
            vec3  stepWorldPos = u_cameraPosition + rayDir * tStep;

            vec2  stepGradient;
            float stepShadingBias;
            float stepCoverage = resolveCloudCoverage(
                bounds, noiseParams, colorScale, variance0, variance1,
                intensity, fullness, stepWorldPos, orientationDir, tStep,
                stepGradient, stepShadingBias);

            if (stepCoverage <= CLOUD_DENSITY_EPSILON)
            continue;

            vec2  patternLocalXZ = stepWorldPos.xz - boxCenter;
            float stepDistance   = length(stepWorldPos.xz);
            float lodFade        = resolveCloudLodFade(max(colorScale.w, 4.0), tStep);

            float localHeightJitter = gradientNoise2D(
                patternLocalXZ * u_weatherHeightVariation.z
                + vec2(patternSeed * 17.3, patternSeed * 29.9))
            * slabThickness * u_weatherHeightVariation.y;

            float stepBentAltitude = resolveCloudDomeAltitude(authoredAltitude, stepDistance)
            + patternHeightOffset + localHeightJitter;

            float verticalNorm, verticalT;
            float verticalDensity = resolveCloudVerticalDensity(
                stepWorldPos, patternLocalXZ,
                stepBentAltitude - slabThickness * 0.5,
                stepBentAltitude + slabThickness * 0.5,
                fullness, stepShadingBias, patternSeed, lodFade,
                verticalNorm, verticalT);

            if (verticalDensity <= CLOUD_DENSITY_EPSILON)
            continue;

            float opticalDepth = stepCoverage * verticalDensity * shape.z
            * CLOUD_DENSITY_OPACITY_SCALE * travelStepRatio;
            float stepTransmittance = exp(-opticalDepth);
            float stepAlpha         = 1.0 - stepTransmittance;

            if (stepAlpha <= CLOUD_DENSITY_EPSILON)
            continue;

            vec3  domeNormal = resolveCloudDomeNormal(authoredAltitude, stepWorldPos);
            float sideFacing = 1.0 - abs(dot(rayDir, domeNormal));

            float horizontalBody = pow(stepCoverage, mix(2.2, 0.8, fullness));
            float verticalBody   = 1.0 - clamp(verticalNorm, 0.0, 1.0);
            float bodyDepth      = clamp(
                mix(horizontalBody, verticalBody, mix(0.15, 0.9, sideFacing)), 0.0, 1.0);

            vec3  stepNormal = resolveCloudStepNormal(domeNormal, stepGradient, verticalT, fullness);
            float selfShadow = mix(1.0, entryRemaining, CLOUD_VOLUME_SELF_SHADOW_STRENGTH);

            entryColor += shadeCloudStep(
                    rayDir, stepNormal, bodyDepth, selfShadow, thicknessNorm, colorScale, materialParams)
            * stepAlpha * entryRemaining;
            entryRemaining *= stepTransmittance;
        }

        float entryAlpha = clamp((1.0 - entryRemaining) * fadeAlpha * rangeFade * farFade, 0.0, 1.0);

        if (entryAlpha <= CLOUD_DENSITY_EPSILON)
        continue;

        vec3 entryStraightColor = entryColor / max(1.0 - entryRemaining, 0.0001);

        // Aerial perspective. Distant clouds wash toward the horizon sky, which
        // is what makes the far edge of the dome read as cotton sitting on the
        // horizon rather than as a separate hard layer pasted over the sky.
        float haze = smoothstep(u_cloudMaxDistance * CLOUD_HAZE_START_FRACTION, u_cloudMaxDistance, tEnter);
        entryStraightColor = mix(entryStraightColor, u_skyHorizonColor, haze * CLOUD_HAZE_STRENGTH);

        float remaining = 1.0 - accumulatedAlpha;

        accumulatedColor += entryStraightColor * entryAlpha * remaining;
        accumulatedAlpha += entryAlpha * remaining;
    }

    if (accumulatedAlpha <= 0.003)
    discard;

    fragColor = vec4(accumulatedColor / max(accumulatedAlpha, 0.0001), accumulatedAlpha);
}