#ifndef CLOUD_MARCH_GLSL
#define CLOUD_MARCH_GLSL

#include "includes/CameraData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/WeatherMapUtility.glsl"
#include "weather/includes/CloudDome.glsl"
#include "weather/includes/CloudVisual.glsl"

/*
 * Integrates the grid's cloud layers along a ray from the camera, out to a
 * given distance. The weather pass integrates to the edge of the weather map
 * for the sky; the lighting pass integrates only as far as each fragment, so
 * a peak wrapped in cloud, or terrain seen from inside one, fogs exactly as
 * the sky's clouds are drawn. Layers are marched nearest first, whichever
 * side of them the camera is on, and composited front to back through one
 * running transmittance. Steps are sized to each layer's thickness and
 * features, and fixed along the ray with no screen-space jitter, so the image
 * stays stable as the camera turns.
 */

const float CLOUD_MARCH_EPSILON                = 0.001;
const float CLOUD_MARCH_TRANSMITTANCE_CUTOFF   = 0.02;
const float CLOUD_MARCH_MIN_THICKNESS_BLOCKS   = 1.0;
const float CLOUD_MARCH_STEP_THICKNESS_RATIO   = 0.2;
const float CLOUD_MARCH_STEP_FEATURE_RATIO     = 0.6;
const int   CLOUD_MARCH_STEP_COUNT_MIN         = 6;
const int   CLOUD_MARCH_STEP_COUNT_MAX         = 24;
const int   CLOUD_MARCH_STEP_BUDGET            = 48;
const float CLOUD_MARCH_REFINE_RATIO           = 0.2;

// Coarse steps until the ray meets cloud, then back up half a step and
// continue in fine steps, so the surface the eye actually sees is resolved
// finely while empty sky is crossed cheaply. Once a coarse step's worth of
// fine steps finds nothing, the ray has left that cloud and returns to coarse
// steps until it meets the next one.
void marchCloudLayer(int layer, vec3 rayDir, float tEnter, float tExit,
    inout vec3 color, inout float transmittance) {
    vec4  shape        = u_weatherLayerShape[layer];
    float baseAltitude = shape.x - u_weatherPlanet.y;
    float thickness    = max(shape.y, CLOUD_MARCH_MIN_THICKNESS_BLOCKS);
    float pathLength   = tExit - tEnter;
    float stepTarget   = min(
        thickness * CLOUD_MARCH_STEP_THICKNESS_RATIO,
        resolveCloudLayerFeatureSize(layer) * CLOUD_MARCH_STEP_FEATURE_RATIO);
    int   stepCount    = clamp(
        int(ceil(pathLength / max(stepTarget, CLOUD_MARCH_EPSILON))),
        CLOUD_MARCH_STEP_COUNT_MIN, CLOUD_MARCH_STEP_COUNT_MAX);

    float coarseStep = pathLength / float(stepCount);
    int   exitSteps  = int(ceil(1.0 / CLOUD_MARCH_REFINE_RATIO));
    bool  refined    = false;
    int   emptySteps = 0;
    float stepLength = coarseStep;
    float t          = tEnter + stepLength * 0.5;

    for (int s = 0; s < CLOUD_MARCH_STEP_BUDGET; s++) {
        if (t > tExit || transmittance <= CLOUD_MARCH_TRANSMITTANCE_CUTOFF)
        break;

        vec3  relativePosition   = rayDir * t;
        vec2  positionXZ         = u_cameraPosition.xz + relativePosition.xz;
        float heightFraction     = (resolveCloudDomeAltitude(relativePosition) - baseAltitude) / thickness;
        float horizontalDistance = length(relativePosition.xz);

        float density = resolveCloudLayerDensity(
            layer, positionXZ, heightFraction,
            resolveCloudOctaves(layer, t), resolveCloudDetailFade(layer, t));

        if (density <= CLOUD_MARCH_EPSILON) {
            emptySteps++;

            if (refined && emptySteps >= exitSteps) {
                refined    = false;
                stepLength = coarseStep;
            }

            t += stepLength;
            continue;
        }

        if (!refined) {
            refined    = true;
            emptySteps = 0;
            stepLength = coarseStep * CLOUD_MARCH_REFINE_RATIO;
            t          = max(tEnter, t - coarseStep * 0.5) + stepLength * 0.5;
            continue;
        }

        emptySteps = 0;

        float stepTransmittance = exp(-density * CLOUD_VISUAL_EXTINCTION_PER_BLOCK * stepLength);
        vec3  domeNormal        = resolveCloudDomeNormal(relativePosition);

        vec3 lit = shadeCloudSample(
            layer, rayDir, domeNormal, heightFraction, density, thickness, horizontalDistance);

        color         += lit * (1.0 - stepTransmittance) * transmittance;
        transmittance *= stepTransmittance;

        t += stepLength;
    }
}

void integrateCloudLayers(vec3 rayDir, float maxDistance, inout vec3 color, inout float transmittance) {
    int   layerCount       = min(u_weatherLayerCount, WEATHER_MAP_MAX_LAYERS);
    float limit            = min(maxDistance, resolveCloudDomeGroundDistance(rayDir));
    float horizontalLength = length(rayDir.xz);

    if (horizontalLength > CLOUD_MARCH_EPSILON)
    limit = min(limit, resolveWeatherMapReach() / horizontalLength);

    float enters[WEATHER_MAP_MAX_LAYERS];
    float exits[WEATHER_MAP_MAX_LAYERS];

    for (int layer = 0; layer < WEATHER_MAP_MAX_LAYERS; layer++) {
        enters[layer] = CLOUD_DOME_NO_HIT;
        exits[layer]  = 0.0;

        if (layer >= layerCount)
        continue;

        vec4  shape        = u_weatherLayerShape[layer];
        float baseAltitude = shape.x - u_weatherPlanet.y;
        float topAltitude  = baseAltitude + max(shape.y, CLOUD_MARCH_MIN_THICKNESS_BLOCKS);
        float tEnter;
        float tExit;

        if (resolveCloudDomeInterval(baseAltitude, topAltitude, rayDir, limit, tEnter, tExit)) {
            enters[layer] = tEnter;
            exits[layer]  = tExit;
        }
    }

    for (int pass = 0; pass < layerCount; pass++) {
        if (transmittance <= CLOUD_MARCH_TRANSMITTANCE_CUTOFF)
        break;

        int   nearest      = -1;
        float nearestEnter = CLOUD_DOME_NO_HIT;

        for (int layer = 0; layer < layerCount; layer++) {
            if (enters[layer] < nearestEnter) {
                nearest      = layer;
                nearestEnter = enters[layer];
            }
        }

        if (nearest < 0)
        break;

        marchCloudLayer(nearest, rayDir, enters[nearest], exits[nearest], color, transmittance);
        enters[nearest] = CLOUD_DOME_NO_HIT;
    }
}

#endif
