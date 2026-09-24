#ifndef CLOUD_MARCH_GLSL
#define CLOUD_MARCH_GLSL

#include "includes/CameraData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/WeatherMapUtility.glsl"
#include "weather/includes/CloudDome.glsl"
#include "weather/includes/CloudVisual.glsl"

/*
 * Integrates every cloud layer along a ray from the camera in one march, out
 * to a given distance, at one of two qualities. All layers are sampled at the
 * same steps and composited front to back through one running transmittance,
 * so a near cloud always covers a far one whichever layer each belongs to —
 * toward the horizon every layer's stretch of the ray overlaps, and marching
 * them one after another would paint a low layer's distant clouds over a high
 * layer's near ones. The sky march runs in the weather pass at reduced
 * resolution: it refines finely where the ray meets cloud and resolves full
 * detail. The fog march runs per terrain fragment in the lighting pass, only
 * where cloud stands between the camera and the fragment, with a few fixed
 * steps and no detail. The first step is offset per pixel by the caller's
 * jitter, so neighbouring rows never sample identical depths — at grazing
 * angles identical depths slice distant cloud into horizontal stripes. Light
 * reaching a sample is attenuated through the layer above it along the
 * light's slant, but never through more than about one cloud's width or
 * depth, since light enters a cloud through its nearest flank. The engine
 * prepends every include to every stage of a program, so nothing here
 * touches fragment-only built-ins; callers pass the jitter in.
 */

struct CloudMarchQuality {
    int  stepCountMin;
    int  stepCountMax;
    int  stepBudget;
    int  octaveLimit;
    bool refine;
    bool detail;
};

const CloudMarchQuality CLOUD_MARCH_SKY = CloudMarchQuality(8, 48, 96, 4, true, true);
const CloudMarchQuality CLOUD_MARCH_FOG = CloudMarchQuality(4, 8, 8, 2, false, false);

const float CLOUD_MARCH_EPSILON              = 0.001;
const float CLOUD_MARCH_TRANSMITTANCE_CUTOFF = 0.02;
const float CLOUD_MARCH_MIN_THICKNESS_BLOCKS = 1.0;
const float CLOUD_MARCH_STEP_THICKNESS_RATIO = 0.2;
const float CLOUD_MARCH_STEP_FEATURE_RATIO   = 0.6;
const float CLOUD_MARCH_REFINE_RATIO         = 0.25;
const float CLOUD_MARCH_CENTERED_OFFSET      = 0.5;
const float CLOUD_MARCH_UNBOUNDED_DISTANCE   = 1.0e30;
const float CLOUD_MARCH_LIGHT_REACH_RATIO    = 1.0;

// ── Layers ─────────────────────────────────────────────────────────────────

float resolveCloudLayerBaseAltitude(int layer) {
    return u_weatherLayerShape[layer].x - u_weatherPlanet.y;
}

float resolveCloudLayerThickness(int layer) {
    return max(u_weatherLayerShape[layer].y, CLOUD_MARCH_MIN_THICKNESS_BLOCKS);
}

// The coarse step a layer needs to resolve both its depth and its features.
float resolveCloudLayerStepTarget(int layer) {
    return min(
        resolveCloudLayerThickness(layer) * CLOUD_MARCH_STEP_THICKNESS_RATIO,
        resolveCloudLayerFeatureSize(layer) * CLOUD_MARCH_STEP_FEATURE_RATIO);
}

// Blocks of cloud light may cross before it must have entered through a
// flank: one feature across, and never more than the layer is deep.
float resolveCloudLayerLightReach(int layer) {
    return min(resolveCloudLayerFeatureSize(layer), resolveCloudLayerThickness(layer))
    * CLOUD_MARCH_LIGHT_REACH_RATIO;
}

// Density of one layer at a point on the ray, or zero where the ray is not
// inside that layer's stretch.
float sampleCloudLayerAt(
    int layer, CloudMarchQuality quality, float t, float layerEnter, float layerExit,
    vec3 relativePosition, float altitude) {
    if (t < layerEnter || t > layerExit)
    return 0.0;

    float heightFraction = (altitude - resolveCloudLayerBaseAltitude(layer)) / resolveCloudLayerThickness(layer);

    if (heightFraction < 0.0 || heightFraction > 1.0)
    return 0.0;

    float featureSize = resolveCloudLayerFeatureSize(layer);
    float detailSize  = featureSize / max(u_weatherLayerNoise[layer].z, 1.0);
    int   octaves     = min(resolveCloudOctaves(featureSize, t), quality.octaveLimit);
    float detailFade  = quality.detail ? resolveCloudDetailFade(detailSize, t) : 0.0;

    return resolveCloudLayerDensity(
        layer, u_cameraPosition.xz + relativePosition.xz, heightFraction, octaves, detailFade);
}

// Lit color of one layer's sample.
vec3 shadeCloudLayerAt(int layer, CloudLight light, float density, vec3 relativePosition, float altitude) {
    float thickness      = resolveCloudLayerThickness(layer);
    float heightFraction = (altitude - resolveCloudLayerBaseAltitude(layer)) / thickness;
    float lightReach     = resolveCloudLayerLightReach(layer);
    vec3  domeNormal     = resolveCloudDomeNormal(relativePosition);

    float sunPath  = min(resolveCloudLightPath(domeNormal, light.sunDir, heightFraction, thickness), lightReach);
    float moonPath = min(resolveCloudLightPath(domeNormal, light.moonDir, heightFraction, thickness), lightReach);

    return shadeCloudSample(
        light, resolveCloudLayerAlbedo(layer), heightFraction, density, thickness,
        resolveCloudLightOpticalDepth(density, sunPath), resolveCloudLightOpticalDepth(density, moonPath),
        length(relativePosition.xz));
}

// ── March ──────────────────────────────────────────────────────────────────

// Coarse steps until the ray meets cloud in any layer, then back up half a
// step and continue in fine steps, so the surface the eye actually sees is
// resolved finely while empty sky is crossed cheaply. Once a coarse step's
// worth of fine steps finds nothing, the ray has left that cloud and returns
// to coarse steps until it meets the next one.
void integrateCloudLayers(
    vec3 rayDir, float maxDistance, CloudMarchQuality quality, float jitter,
    inout vec3 color, inout float transmittance) {
    int   layerCount       = min(u_weatherLayerCount, WEATHER_MAP_MAX_LAYERS);
    float limit            = min(maxDistance, resolveCloudDomeGroundDistance(rayDir));
    float horizontalLength = length(rayDir.xz);

    if (horizontalLength > CLOUD_MARCH_EPSILON)
    limit = min(limit, resolveWeatherMapReach() / horizontalLength);

    float enters[WEATHER_MAP_MAX_LAYERS];
    float exits[WEATHER_MAP_MAX_LAYERS];
    float marchStart = CLOUD_DOME_NO_HIT;
    float marchEnd   = 0.0;
    float stepTarget = CLOUD_DOME_NO_HIT;

    for (int layer = 0; layer < WEATHER_MAP_MAX_LAYERS; layer++) {
        enters[layer] = CLOUD_DOME_NO_HIT;
        exits[layer]  = 0.0;

        if (layer >= layerCount)
        continue;

        float baseAltitude = resolveCloudLayerBaseAltitude(layer);
        float topAltitude  = baseAltitude + resolveCloudLayerThickness(layer);
        float tEnter;
        float tExit;

        if (resolveCloudDomeInterval(baseAltitude, topAltitude, rayDir, limit, tEnter, tExit)) {
            enters[layer] = tEnter;
            exits[layer]  = tExit;
            marchStart    = min(marchStart, tEnter);
            marchEnd      = max(marchEnd, tExit);
            stepTarget    = min(stepTarget, resolveCloudLayerStepTarget(layer));
        }
    }

    if (marchEnd <= marchStart)
    return;

    CloudLight light      = resolveCloudLight(rayDir);
    float      pathLength = marchEnd - marchStart;
    int        stepCount  = clamp(
        int(ceil(pathLength / max(stepTarget, CLOUD_MARCH_EPSILON))),
        quality.stepCountMin, quality.stepCountMax);

    float coarseStep = pathLength / float(stepCount);
    int   exitSteps  = int(ceil(1.0 / CLOUD_MARCH_REFINE_RATIO));
    bool  refined    = false;
    int   emptySteps = 0;
    float stepLength = coarseStep;
    float t          = marchStart + stepLength * jitter;

    float layerDensity[WEATHER_MAP_MAX_LAYERS];

    for (int s = 0; s < quality.stepBudget; s++) {
        if (t > marchEnd || transmittance <= CLOUD_MARCH_TRANSMITTANCE_CUTOFF)
        break;

        vec3  relativePosition = rayDir * t;
        float altitude         = resolveCloudDomeAltitude(relativePosition);
        float density          = 0.0;

        for (int layer = 0; layer < layerCount; layer++) {
            layerDensity[layer] = sampleCloudLayerAt(
                layer, quality, t, enters[layer], exits[layer], relativePosition, altitude);
            density += layerDensity[layer];
        }

        if (density <= CLOUD_MARCH_EPSILON) {
            emptySteps++;

            if (refined && emptySteps >= exitSteps) {
                refined    = false;
                stepLength = coarseStep;
            }

            t += stepLength;
            continue;
        }

        if (quality.refine && !refined) {
            refined    = true;
            emptySteps = 0;
            stepLength = coarseStep * CLOUD_MARCH_REFINE_RATIO;
            t          = max(marchStart, t - coarseStep * CLOUD_MARCH_CENTERED_OFFSET)
            + stepLength * CLOUD_MARCH_CENTERED_OFFSET;
            continue;
        }

        emptySteps = 0;

        vec3 lit = vec3(0.0);

        for (int layer = 0; layer < layerCount; layer++) {
            if (layerDensity[layer] > CLOUD_MARCH_EPSILON)
            lit += shadeCloudLayerAt(layer, light, layerDensity[layer], relativePosition, altitude)
            * layerDensity[layer];
        }

        float stepTransmittance = exp(-density * CLOUD_VISUAL_EXTINCTION_PER_BLOCK * stepLength);

        color         += (lit / density) * (1.0 - stepTransmittance) * transmittance;
        transmittance *= stepTransmittance;

        t += stepLength;
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
