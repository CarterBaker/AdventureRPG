#ifndef CLOUD_MARCH_GLSL
#define CLOUD_MARCH_GLSL

#include "includes/CameraData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/WeatherMapUtility.glsl"
#include "weather/includes/CloudDome.glsl"
#include "weather/includes/CloudVisual.glsl"

/*
 * Integrates the grid's cloud layers along a ray from the camera, out to a
 * given distance, at one of two qualities. The sky march runs in the weather
 * pass at reduced resolution: it refines finely where the ray meets cloud,
 * resolves full detail, and — wherever a ray's steps grow coarse next to the
 * layer, as they do toward the horizon — offsets them by a per-pixel
 * interleaved gradient, which turns step slicing into fine grain that the
 * pass's own upscale then smooths away. Rays with fine steps, such as those
 * looking up through a layer, keep centred steps and no grain at all. The fog march runs per terrain
 * fragment in the lighting pass, only where cloud stands between the camera
 * and the fragment, with a few fixed steps and no detail — terrain fog needs
 * the cloud's density, not its fine silhouette. Layers are marched nearest
 * first, whichever side of them the camera is on, and composited front to
 * back through one running transmittance.
 */

struct CloudMarchQuality {
    int  stepCountMin;
    int  stepCountMax;
    int  stepBudget;
    int  octaveLimit;
    bool refine;
    bool detail;
};

const CloudMarchQuality CLOUD_MARCH_SKY = CloudMarchQuality(6, 32, 64, 4, true, true);
const CloudMarchQuality CLOUD_MARCH_FOG = CloudMarchQuality(4, 8, 8, 2, false, false);

const float CLOUD_MARCH_EPSILON              = 0.001;
const float CLOUD_MARCH_TRANSMITTANCE_CUTOFF = 0.02;
const float CLOUD_MARCH_MIN_THICKNESS_BLOCKS = 1.0;
const float CLOUD_MARCH_STEP_THICKNESS_RATIO = 0.2;
const float CLOUD_MARCH_STEP_FEATURE_RATIO   = 0.6;
const float CLOUD_MARCH_REFINE_RATIO         = 0.25;
const float CLOUD_MARCH_CENTERED_OFFSET      = 0.5;
const float CLOUD_MARCH_JITTER_FULL_RATIO    = 2.0;
const float CLOUD_MARCH_UNBOUNDED_DISTANCE   = 1.0e30;
const vec3  CLOUD_MARCH_JITTER_MAGIC         = vec3(0.06711056, 0.00583715, 52.9829189);

// Interleaved gradient noise: a per-pixel step offset whose neighbours are
// as different as possible, so slicing breaks into the finest grain.
float resolveCloudMarchJitter() {
    return fract(CLOUD_MARCH_JITTER_MAGIC.z * fract(dot(gl_FragCoord.xy, CLOUD_MARCH_JITTER_MAGIC.xy)));
}

// Coarse steps until the ray meets cloud, then back up half a step and
// continue in fine steps, so the surface the eye actually sees is resolved
// finely while empty sky is crossed cheaply. Once a coarse step's worth of
// fine steps finds nothing, the ray has left that cloud and returns to coarse
// steps until it meets the next one.
void marchCloudLayer(
    int layer, CloudMarchQuality quality, CloudLight light, vec3 rayDir, float tEnter, float tExit,
    float stepOffset, inout vec3 color, inout float transmittance) {
    vec4  shape        = u_weatherLayerShape[layer];
    float baseAltitude = shape.x - u_weatherPlanet.y;
    float thickness    = max(shape.y, CLOUD_MARCH_MIN_THICKNESS_BLOCKS);
    float featureSize  = resolveCloudLayerFeatureSize(layer);
    float detailSize   = featureSize / max(u_weatherLayerNoise[layer].z, 1.0);
    vec3  albedo       = resolveCloudLayerAlbedo(layer);
    float pathLength   = tExit - tEnter;
    float stepTarget   = min(
        thickness * CLOUD_MARCH_STEP_THICKNESS_RATIO,
        featureSize * CLOUD_MARCH_STEP_FEATURE_RATIO);
    int   stepCount    = clamp(
        int(ceil(pathLength / max(stepTarget, CLOUD_MARCH_EPSILON))),
        quality.stepCountMin, quality.stepCountMax);

    float coarseStep = pathLength / float(stepCount);
    float coarseness = clamp(
        (coarseStep / max(stepTarget, CLOUD_MARCH_EPSILON) - 1.0) / (CLOUD_MARCH_JITTER_FULL_RATIO - 1.0), 0.0, 1.0);
    float offset     = mix(CLOUD_MARCH_CENTERED_OFFSET, stepOffset, coarseness);
    int   exitSteps  = int(ceil(1.0 / CLOUD_MARCH_REFINE_RATIO));
    bool  refined    = false;
    int   emptySteps = 0;
    float stepLength = coarseStep;
    float t          = tEnter + stepLength * offset;

    for (int s = 0; s < quality.stepBudget; s++) {
        if (t > tExit || transmittance <= CLOUD_MARCH_TRANSMITTANCE_CUTOFF)
        break;

        vec3  relativePosition = rayDir * t;
        float heightFraction   = (resolveCloudDomeAltitude(relativePosition) - baseAltitude) / thickness;
        int   octaves          = min(resolveCloudOctaves(featureSize, t), quality.octaveLimit);
        float detailFade       = quality.detail ? resolveCloudDetailFade(detailSize, t) : 0.0;

        float density = resolveCloudLayerDensity(
            layer, u_cameraPosition.xz + relativePosition.xz, heightFraction, octaves, detailFade);

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
            t          = max(tEnter, t - coarseStep * CLOUD_MARCH_CENTERED_OFFSET) + stepLength * offset;
            continue;
        }

        emptySteps = 0;

        float stepTransmittance = exp(-density * CLOUD_VISUAL_EXTINCTION_PER_BLOCK * stepLength);

        vec3 lit = shadeCloudSample(
            light, albedo, resolveCloudDomeNormal(relativePosition), heightFraction, density, thickness,
            length(relativePosition.xz));

        color         += lit * (1.0 - stepTransmittance) * transmittance;
        transmittance *= stepTransmittance;

        t += stepLength;
    }
}

void integrateCloudLayers(
    vec3 rayDir, float maxDistance, CloudMarchQuality quality, float stepOffset,
    inout vec3 color, inout float transmittance) {
    int   layerCount       = min(u_weatherLayerCount, WEATHER_MAP_MAX_LAYERS);
    float limit            = min(maxDistance, resolveCloudDomeGroundDistance(rayDir));
    float horizontalLength = length(rayDir.xz);

    if (horizontalLength > CLOUD_MARCH_EPSILON)
    limit = min(limit, resolveWeatherMapReach() / horizontalLength);

    float enters[WEATHER_MAP_MAX_LAYERS];
    float exits[WEATHER_MAP_MAX_LAYERS];
    bool  anyLayer = false;

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
            anyLayer      = true;
        }
    }

    if (!anyLayer)
    return;

    CloudLight light = resolveCloudLight(rayDir);

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

        marchCloudLayer(
            nearest, quality, light, rayDir, enters[nearest], exits[nearest], stepOffset, color, transmittance);
        enters[nearest] = CLOUD_DOME_NO_HIT;
    }
}

// The sky seen along a view ray, out to the edge of the weather map.
void integrateCloudSky(vec3 rayDir, inout vec3 color, inout float transmittance) {
    integrateCloudLayers(
        rayDir, CLOUD_MARCH_UNBOUNDED_DISTANCE, CLOUD_MARCH_SKY, resolveCloudMarchJitter(), color, transmittance);
}

// Cloud standing between the camera and a surface fragmentDistance away.
void integrateCloudFog(vec3 rayDir, float fragmentDistance, inout vec3 color, inout float transmittance) {
    integrateCloudLayers(
        rayDir, fragmentDistance, CLOUD_MARCH_FOG, CLOUD_MARCH_CENTERED_OFFSET, color, transmittance);
}

#endif
