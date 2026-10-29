#ifndef CLOUD_RAIN_GLSL
#define CLOUD_RAIN_GLSL

#include "includes/CameraData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/WeatherMapUtility.glsl"
#include "weather/includes/CloudDome.glsl"

/*
 * Where rain falls, shared by the falling drops and the weather pass's rain
 * shafts so both see the same storms. Rain falls only beneath a cloud whose
 * archetype carries some, and only below that cloud's own underside as the
 * dome bends it, so no drop or shaft ever stands above the cloud it falls
 * from. The weather pass marches the shafts and writes where they begin along
 * the view ray and their optical depth into its distance target's z and w
 * (CloudComposite); a surface takes the shafts in front of it as if their
 * whole depth were packed in, at full density, right after they begin, so a
 * hill inside a storm is veiled while one in front of it stays clear.
 */

const float CLOUD_RAIN_SHAFT_DENSITY_PER_BLOCK = 0.0008;
const float CLOUD_RAIN_SHAFT_MAX_OPACITY       = 0.85;

// The highest a layer's cloud underside ever reaches, as a share of the
// layer: a sheet's never rises past the layer's centre and a puffy cloud's
// never past its rounded base.
float resolveCloudLayerUndersideCeiling(int layer) {
    return mix(CLOUD_LAYER_SHEET_CENTER, CLOUD_LAYER_BASE_ROUNDING, u_weatherLayerShape[layer].w);
}

// How much rain falls at a point relativePosition away from the camera, 0 to
// 1: the strongest rain-bearing cloud standing over it, each counting only
// below its own underside. bumpFade lobes the columns as the cloud march does.
float resolveCloudRainCover(vec3 relativePosition, vec2 bumpFade) {
    int   layerCount = min(u_weatherLayerCount, WEATHER_MAP_MAX_LAYERS);
    vec2  positionXZ = u_cameraPosition.xz + relativePosition.xz;
    float height     = u_cameraPosition.y + relativePosition.y;
    float cover      = 0.0;

    for (int layer = 0; layer < layerCount; layer++) {
        float share = resolveCloudLayerPrecipitation(layer);

        if (share <= cover)
        continue;

        CloudColumn column = resolveCloudColumn(layer, positionXZ, bumpFade);

        if (column.top <= column.bottom
        || height > resolveCloudLayerWorldHeight(layer, column.bottom, relativePosition.xz))
        continue;

        cover = max(cover, column.opacity * share);
    }

    return cover;
}

// The rain shafts the weather pass wrote at a screen position, softened by
// four half-texel taps into a tent over its reduced-resolution target, which
// averages away the march's per-pixel jitter.
vec2 sampleRainShaft(sampler2D cloudDistance, vec2 uv) {
    vec2 halfTexel = 0.5 / vec2(textureSize(cloudDistance, 0));

    return 0.25 * (
        texture(cloudDistance, uv + vec2(-halfTexel.x, -halfTexel.y)).zw +
        texture(cloudDistance, uv + vec2( halfTexel.x, -halfTexel.y)).zw +
        texture(cloudDistance, uv + vec2(-halfTexel.x,  halfTexel.y)).zw +
        texture(cloudDistance, uv + vec2( halfTexel.x,  halfTexel.y)).zw);
}

// Opacity of the rain shafts standing in front of a surface surfaceDistance
// away, from where the shafts begin along the ray (x) and their optical
// depth (y).
float resolveRainShaftOpacity(vec2 shaft, float surfaceDistance) {
    float depth = min(max(surfaceDistance - shaft.x, 0.0) * CLOUD_RAIN_SHAFT_DENSITY_PER_BLOCK, shaft.y);
    return (1.0 - exp(-depth)) * CLOUD_RAIN_SHAFT_MAX_OPACITY;
}

#endif
