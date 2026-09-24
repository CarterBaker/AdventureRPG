#version 330 core

in vec3 v_dir;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/NoiseUtility.glsl"
#include "includes/WeatherMapUtility.glsl"
#include "weather/includes/CloudDome.glsl"
#include "weather/includes/CloudVisual.glsl"

/*
 * Fullscreen cloud pass. Every live cloud layer is a dome shell over the
 * camera (CloudDome) whose density comes from the grid's window of the
 * scrolling weather image (WeatherMapUtility). Layers are ordered by
 * altitude, so they are marched nearest first and composited front to back
 * through one running transmittance. Steps are sized to each layer's own
 * thickness, so a ray straight up crosses a thin slab in a few steps while a
 * grazing ray toward the horizon spends its budget travelling through the
 * clouds' sides. Steps are also kept shorter than the layer's own features,
 * so a grazing ray resolves each lobe instead of slicing through it. Steps
 * sit at fixed positions along the ray with no screen-space jitter, so the
 * image stays stable as the camera turns.
 */

const float CLOUD_PASS_EPSILON                = 0.001;
const float CLOUD_PASS_TRANSMITTANCE_CUTOFF   = 0.02;
const float CLOUD_PASS_ALPHA_DISCARD          = 0.003;
const float CLOUD_PASS_HORIZON_FADE_BELOW     = -0.02;
const float CLOUD_PASS_HORIZON_FADE_ABOVE     = 0.01;
const float CLOUD_PASS_STEP_THICKNESS_RATIO   = 0.2;
const float CLOUD_PASS_STEP_FEATURE_RATIO     = 0.6;
const int   CLOUD_PASS_STEP_COUNT_MIN         = 6;
const int   CLOUD_PASS_STEP_COUNT_MAX         = 24;
const int   CLOUD_PASS_STEP_BUDGET            = 48;
const float CLOUD_PASS_REFINE_RATIO           = 0.2;

// Coarse steps until the ray meets cloud, then back up half a step and
// continue in fine steps, so the surface the eye actually sees is resolved
// finely while empty sky is crossed cheaply. Once a coarse step's worth of
// fine steps finds nothing, the ray has left that cloud and returns to coarse
// steps until it meets the next one.
void marchCloudLayer(int layer, vec3 rayDir, vec3 dome, float tEnter, float tExit,
    inout vec3 color, inout float transmittance) {
    float thickness  = dome.z - dome.y;
    float pathLength = tExit - tEnter;
    float stepTarget = min(
        thickness * CLOUD_PASS_STEP_THICKNESS_RATIO,
        resolveCloudLayerFeatureSize(layer) * CLOUD_PASS_STEP_FEATURE_RATIO);
    int   stepCount  = clamp(
        int(ceil(pathLength / max(stepTarget, CLOUD_PASS_EPSILON))),
        CLOUD_PASS_STEP_COUNT_MIN, CLOUD_PASS_STEP_COUNT_MAX);

    float coarseStep = pathLength / float(stepCount);
    int   exitSteps  = int(ceil(1.0 / CLOUD_PASS_REFINE_RATIO));
    bool  refined    = false;
    int   emptySteps = 0;
    float stepLength = coarseStep;
    float t          = tEnter + stepLength * 0.5;

    for (int s = 0; s < CLOUD_PASS_STEP_BUDGET; s++) {
        if (t > tExit || transmittance <= CLOUD_PASS_TRANSMITTANCE_CUTOFF)
        break;

        vec3  relativePosition   = rayDir * t;
        vec2  positionXZ         = u_cameraPosition.xz + relativePosition.xz;
        float heightFraction     = resolveCloudDomeHeightFraction(dome, relativePosition);
        float horizontalDistance = length(relativePosition.xz);

        float density = resolveCloudLayerDensity(
            layer, positionXZ, heightFraction,
            resolveCloudOctaves(layer, t), resolveCloudDetailFade(layer, t));

        if (density <= CLOUD_PASS_EPSILON) {
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
            stepLength = coarseStep * CLOUD_PASS_REFINE_RATIO;
            t          = max(tEnter, t - coarseStep * 0.5) + stepLength * 0.5;
            continue;
        }

        emptySteps = 0;

        float stepTransmittance = exp(-density * CLOUD_VISUAL_EXTINCTION_PER_BLOCK * stepLength);
        vec3  domeNormal        = resolveCloudDomeNormal(dome, relativePosition);

        vec3 lit = shadeCloudSample(
            layer, rayDir, domeNormal, heightFraction, density, thickness, horizontalDistance);

        color         += lit * (1.0 - stepTransmittance) * transmittance;
        transmittance *= stepTransmittance;

        t += stepLength;
    }
}

void main() {
    int layerCount = min(u_weatherLayerCount, WEATHER_MAP_MAX_LAYERS);

    if (layerCount == 0)
    discard;

    vec3  rayDir      = normalize(v_dir);
    float horizonFade = smoothstep(CLOUD_PASS_HORIZON_FADE_BELOW, CLOUD_PASS_HORIZON_FADE_ABOVE, rayDir.y);

    if (horizonFade <= CLOUD_PASS_EPSILON)
    discard;

    float reach         = resolveWeatherMapReach();
    vec3  color         = vec3(0.0);
    float transmittance = 1.0;

    for (int layer = 0; layer < layerCount; layer++) {
        if (transmittance <= CLOUD_PASS_TRANSMITTANCE_CUTOFF)
        break;

        vec3  dome = resolveCloudDome(layer);
        float tEnter;
        float tExit;

        if (!resolveCloudDomeInterval(dome, rayDir, reach, tEnter, tExit))
        continue;

        marchCloudLayer(layer, rayDir, dome, tEnter, tExit, color, transmittance);
    }

    float coverage = 1.0 - transmittance;
    float alpha    = coverage * horizonFade;

    if (alpha <= CLOUD_PASS_ALPHA_DISCARD)
    discard;

    fragColor = vec4(color / max(coverage, CLOUD_PASS_EPSILON), alpha);
}
