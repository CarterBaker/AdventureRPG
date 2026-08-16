#ifndef CLOUD_SHADOW_GLSL
#define CLOUD_SHADOW_GLSL

#include "includes/WeatherMapData.glsl"
#include "includes/NoiseUtility.glsl"

/*
* Cheap terrain shadow cast by in-range weather patterns, sampled directly
 * by the surface shader against the same WeatherMapData the visible puffs
 * in WeatherShader.fsh read. No raymarch and no true occlusion test — each
 * entry's own footprint is tested against the fragment's world XZ, offset
 * opposite the sun by that entry's own altitude so the shadow falls where
 * the cloud the player sees would actually cast it rather than sitting
 * directly beneath the puff regardless of sun angle. sunHorizonOffset is
 * the per-block-of-altitude horizontal drift toward the shadow — callers
 * precompute it once per fragment as -sunDirection.xz / max(sunDirection.y,
 * minimum elevation), so a low sun casts a longer shadow than a high one
 * at no extra per-entry cost.
 */

const float CLOUD_SHADOW_DENSITY_EPSILON  = 0.001;
const float CLOUD_SHADOW_OUTER_FADE_START = 0.7;
const float CLOUD_SHADOW_OUTER_FADE_END   = 1.1;
const float CLOUD_SHADOW_NOISE_SCALE      = 0.01;
const float CLOUD_SHADOW_STRENGTH         = 0.6;
const float CLOUD_SHADOW_SATURATION       = 0.95;

float sampleCloudShadow(vec3 worldPos, vec2 sunHorizonOffset) {
    int entryCount = min(u_weatherEntryCount, WEATHER_MAP_MAX_ENTRIES);

    if (entryCount == 0)
    return 0.0;

    float shadow = 0.0;

    for (int i = 0; i < entryCount; i++) {
        if (shadow > CLOUD_SHADOW_SATURATION)
        break;

        vec4  patternState = u_weatherPatternState[i];
        float intensity    = patternState.x;
        float fadeAlpha    = patternState.y;
        float rangeFade    = patternState.w;

        if (intensity <= CLOUD_SHADOW_DENSITY_EPSILON ||
            fadeAlpha <= CLOUD_SHADOW_DENSITY_EPSILON ||
            rangeFade <= CLOUD_SHADOW_DENSITY_EPSILON)
        continue;

        vec4 shape = u_weatherCloudShape[i];

        if (shape.z <= CLOUD_SHADOW_DENSITY_EPSILON)
        continue;

        vec4 bounds        = u_weatherBounds[i];
        vec2 boxCenter     = (bounds.xy + bounds.zw) * 0.5;
        vec2 boxHalfExtent = max((bounds.zw - bounds.xy) * 0.5, vec2(1.0));
        vec2 shadowPos     = worldPos.xz + sunHorizonOffset * shape.y;
        vec2 fromCenter    = shadowPos - boxCenter;
        vec2 spreadNorm    = fromCenter / boxHalfExtent;
        float radialDist   = length(spreadNorm);

        if (radialDist > CLOUD_SHADOW_OUTER_FADE_END)
        continue;

        float outerFade = 1.0 - smoothstep(CLOUD_SHADOW_OUTER_FADE_START, CLOUD_SHADOW_OUTER_FADE_END, radialDist);

        if (outerFade <= CLOUD_SHADOW_DENSITY_EPSILON)
        continue;

        vec4  variance1   = u_weatherCloudVariance1[i];
        float patternSeed = variance1.z;
        float noiseSample = gradientNoise2D(
            shadowPos * CLOUD_SHADOW_NOISE_SCALE + vec2(patternSeed * 12.9898, patternSeed * 78.233));

        float coverage       = clamp(intensity + (noiseSample - 0.5) * 0.6, 0.0, 1.0) * outerFade;
        float densityOpacity = clamp(shape.z * 1.5, 0.0, 1.0);
        float entryShadow    = coverage * densityOpacity * fadeAlpha * rangeFade * CLOUD_SHADOW_STRENGTH;

        shadow += entryShadow * (1.0 - shadow);
    }

    return clamp(shadow, 0.0, 1.0);
}

#endif