#ifndef CLOUD_SHADOW_GLSL
#define CLOUD_SHADOW_GLSL

#include "includes/WeatherMapData.glsl"
#include "includes/NoiseUtility.glsl"
#include "includes/WeatherMapUtility.glsl"

/*
 * Terrain shadow cast by the grid's cloud layers, sampled by the surface
 * shader through the same density function the weather pass draws the
 * clouds with, so a shadow has the shape of the cloud above it. Each layer is
 * read once at its mid-height, displaced toward the sun by that height above
 * the fragment, so a low sun throws long shadows. sunHorizonOffset is the
 * horizontal travel toward the sun per block of height — callers precompute
 * it once per fragment as sunDirection.xz / max(sunDirection.y, minimum
 * elevation). Shadows only need the broad shape, so the shape field is read
 * at its coarsest octaves with no detail.
 */

const float CLOUD_SHADOW_HEIGHT_FRACTION = 0.4;
const int   CLOUD_SHADOW_OCTAVES         = 2;
const float CLOUD_SHADOW_EXTINCTION      = 0.08;
const float CLOUD_SHADOW_MAX             = 0.75;

float sampleCloudShadow(vec3 worldPos, vec2 sunHorizonOffset) {
    int layerCount = min(u_weatherLayerCount, WEATHER_MAP_MAX_LAYERS);

    if (layerCount == 0)
    return 0.0;

    float opticalDepth = 0.0;

    for (int layer = 0; layer < layerCount; layer++) {
        vec4  shape       = u_weatherLayerShape[layer];
        float layerHeight = shape.x + shape.y * CLOUD_SHADOW_HEIGHT_FRACTION - worldPos.y;

        if (layerHeight <= 0.0)
        continue;

        vec2  shadowXZ = worldPos.xz + sunHorizonOffset * layerHeight;
        float density  = resolveCloudLayerDensity(
            layer, shadowXZ, CLOUD_SHADOW_HEIGHT_FRACTION, CLOUD_SHADOW_OCTAVES, 0.0);

        opticalDepth += density * shape.y * CLOUD_SHADOW_EXTINCTION;
    }

    return (1.0 - exp(-opticalDepth)) * CLOUD_SHADOW_MAX;
}

#endif
