#ifndef CLOUD_SHADOW_GLSL
#define CLOUD_SHADOW_GLSL

#include "includes/WeatherMapData.glsl"
#include "includes/NoiseUtility.glsl"

/*
* Cheap terrain shadow cast by the physical cloud layer, for the deferred
 * lighting pass. Reuses each weather entry's own footprint bounds and
 * coverage intensity instead of raymarching — one flat-plane intersection
 * per entry along the light direction, then a single soft radial+noise
 * coverage sample, composited front-to-back exactly like WeatherShader.fsh's
 * visual pass but without any puff shape, fake normal, or rim/ambient
 * shading work, since a shadow only needs an occlusion fraction, not an
 * image.
 */

const float CLOUD_SHADOW_DENSITY_EPSILON  = 0.001;
const float CLOUD_SHADOW_OUTER_FADE_START = 0.85;
const float CLOUD_SHADOW_OUTER_FADE_END   = 1.35;
const float CLOUD_SHADOW_NOISE_SCALE      = 0.01;
const float CLOUD_SHADOW_STRENGTH         = 0.55;
const float CLOUD_SHADOW_SATURATION       = 0.985;

float sampleCloudShadow(vec3 worldPos, vec3 lightDir) {
    int entryCount = min(u_weatherEntryCount, WEATHER_MAP_MAX_ENTRIES);

    if (entryCount == 0 || lightDir.y <= 0.0001)
    return 0.0;

    float shadow = 0.0;

    for (int i = 0; i < entryCount; i++) {
        if (shadow > CLOUD_SHADOW_SATURATION)
        break;

        vec4  patternState = u_weatherPatternState[i];
        float intensity     = patternState.x;
        float fadeAlpha     = patternState.y;
        float rangeFade     = patternState.w;

        if (intensity <= CLOUD_SHADOW_DENSITY_EPSILON ||
            fadeAlpha <= CLOUD_SHADOW_DENSITY_EPSILON ||
            rangeFade <= CLOUD_SHADOW_DENSITY_EPSILON)
        continue;

        vec4 shape = u_weatherCloudShape[i];

        if (shape.z <= CLOUD_SHADOW_DENSITY_EPSILON)
        continue;

        float planeY = clamp(shape.y, u_weatherCloudLayerMinY, u_weatherCloudLayerMaxY);
        float t      = (planeY - worldPos.y) / lightDir.y;

        if (t <= 0.0)
        continue;

        vec3 hitPos = worldPos + lightDir * t;

        vec4 bounds        = u_weatherBounds[i];
        vec2 boxCenter      = (bounds.xy + bounds.zw) * 0.5;
        vec2 boxHalfExtent  = max((bounds.zw - bounds.xy) * 0.5, vec2(1.0));
        vec2 fromCenter     = hitPos.xz - boxCenter;
        vec2 spreadNorm     = fromCenter / boxHalfExtent;
        float radialDist    = length(spreadNorm);

        if (radialDist > CLOUD_SHADOW_OUTER_FADE_END)
        continue;

        float outerFade = 1.0 - smoothstep(CLOUD_SHADOW_OUTER_FADE_START, CLOUD_SHADOW_OUTER_FADE_END, radialDist);

        if (outerFade <= CLOUD_SHADOW_DENSITY_EPSILON)
        continue;

        vec4  variance1    = u_weatherCloudVariance1[i];
        float patternSeed  = variance1.z;
        float noiseSample  = gradientNoise2D(
            hitPos.xz * CLOUD_SHADOW_NOISE_SCALE + vec2(patternSeed * 12.9898, patternSeed * 78.233));

        float coverage       = clamp(intensity + (noiseSample - 0.5) * 0.6, 0.0, 1.0) * outerFade;
        float densityOpacity = clamp(shape.z * 1.5, 0.0, 1.0);
        float entryShadow    = coverage * densityOpacity * fadeAlpha * rangeFade * CLOUD_SHADOW_STRENGTH;

        shadow += entryShadow * (1.0 - shadow);
    }

    return clamp(shadow, 0.0, 1.0);
}

#endif