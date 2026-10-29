#ifndef CLOUD_SHADOW_GLSL
#define CLOUD_SHADOW_GLSL

#include "includes/CameraData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/WeatherMapUtility.glsl"
#include "weather/includes/CloudDome.glsl"

/*
 * Terrain shadow cast by the grid's cloud layers, sampled by the surface
 * shader through the same cloud columns the weather pass draws, so a shadow
 * has the shape of the cloud above it and a thin sheet casts a lighter
 * one. Each layer is read once at its mid-height, at the height the dome
 * bends it down to over the fragment, displaced toward the sun by that height
 * above the fragment, so a low sun throws long shadows and a distant hill
 * takes the shadow of the cloud drawn above it. sunHorizonOffset is the
 * horizontal travel toward the sun per block of height — callers precompute
 * it once per fragment as sunDirection.xz / max(sunDirection.y, minimum
 * elevation). Shadows only need the broad shape, so the column is read with
 * no lobes.
 */

const float CLOUD_SHADOW_HEIGHT_FRACTION = 0.4;
const float CLOUD_SHADOW_MAX             = 0.85;

float sampleCloudShadow(vec3 worldPos, vec2 sunHorizonOffset) {
    int layerCount = min(u_weatherLayerCount, WEATHER_MAP_MAX_LAYERS);

    if (layerCount == 0)
    return 0.0;

    vec2  cameraOffset = worldPos.xz - u_cameraPosition.xz;
    float visibility   = 1.0;

    for (int layer = 0; layer < layerCount; layer++) {
        float layerHeight = resolveCloudLayerWorldHeight(layer, CLOUD_SHADOW_HEIGHT_FRACTION, cameraOffset)
        - worldPos.y;

        if (layerHeight <= 0.0)
        continue;

        vec2        shadowXZ = worldPos.xz + sunHorizonOffset * layerHeight;
        CloudColumn column   = resolveCloudColumn(layer, shadowXZ, vec2(0.0));

        visibility *= 1.0 - column.opacity * CLOUD_SHADOW_MAX;
    }

    return 1.0 - visibility;
}

#endif
