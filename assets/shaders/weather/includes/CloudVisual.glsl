#ifndef CLOUD_VISUAL_GLSL
#define CLOUD_VISUAL_GLSL

#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/WeatherMapUtility.glsl"

/*
 * Shared cloud look for the weather pass: how much detail a sample can
 * afford at its distance, and how a sample is lit. Light toward the sun is
 * attenuated through whatever of the layer still lies above the sample, so
 * bases fall into shade while crowns catch the sun — overhead the view meets
 * those shaded bases, from the side it meets the lit domes. A second, softer
 * attenuation stands in for light scattered many times inside the cloud, so
 * thick bases read grey rather than black. Ambient comes from the day's own
 * sky gradient, a forward-scattering lobe silvers edges against the sun, and
 * distance washes every sample toward the horizon sky so the far rim of the
 * dome melts into the horizon.
 */

const float CLOUD_VISUAL_EPSILON = 0.001;

const float CLOUD_VISUAL_EXTINCTION_PER_BLOCK = 0.08;
const float CLOUD_VISUAL_SELF_SHADOW          = 0.6;
const float CLOUD_VISUAL_MULTI_SCATTER_SCALE  = 0.25;
const float CLOUD_VISUAL_MULTI_SCATTER_WEIGHT = 0.55;
const float CLOUD_VISUAL_LIGHT_MIN_COSINE     = 0.15;
const float CLOUD_VISUAL_POWDER_STRENGTH      = 0.5;
const float CLOUD_VISUAL_FORWARD_SCATTER      = 0.6;
const float CLOUD_VISUAL_FORWARD_WEIGHT       = 0.18;
const float CLOUD_VISUAL_AMBIENT_BASE         = 0.45;
const float CLOUD_VISUAL_AMBIENT_TOP          = 0.95;
const float CLOUD_VISUAL_SKY_TINT_STRENGTH    = 0.35;
const vec3  CLOUD_VISUAL_MOON_TINT            = vec3(0.58, 0.74, 1.00);
const float CLOUD_VISUAL_MOON_INTENSITY_MAX   = 0.18;
const float CLOUD_VISUAL_HAZE_START           = 0.35;
const float CLOUD_VISUAL_HAZE_STRENGTH        = 0.85;

const int   CLOUD_VISUAL_OCTAVES_NEAR       = 4;
const int   CLOUD_VISUAL_OCTAVES_MID        = 3;
const int   CLOUD_VISUAL_OCTAVES_FAR        = 2;
const float CLOUD_VISUAL_OCTAVES_NEAR_ANGLE = 0.12;
const float CLOUD_VISUAL_OCTAVES_MID_ANGLE  = 0.04;
const float CLOUD_VISUAL_LOD_MIN_ANGLE      = 0.002;
const float CLOUD_VISUAL_LOD_FULL_ANGLE     = 0.012;

// ── Level of Detail ────────────────────────────────────────────────────────

// Octaves whose features are still wider than a pixel at this distance.
int resolveCloudOctaves(int layer, float sampleDistance) {
    float angle = resolveCloudLayerFeatureSize(layer) / max(sampleDistance, 1.0);

    if (angle > CLOUD_VISUAL_OCTAVES_NEAR_ANGLE)
    return CLOUD_VISUAL_OCTAVES_NEAR;

    return angle > CLOUD_VISUAL_OCTAVES_MID_ANGLE ? CLOUD_VISUAL_OCTAVES_MID : CLOUD_VISUAL_OCTAVES_FAR;
}

// Fraction of the detail octave still larger than a pixel at this distance.
float resolveCloudDetailFade(int layer, float sampleDistance) {
    float detailSize = resolveCloudLayerFeatureSize(layer) / max(u_weatherLayerNoise[layer].z, 1.0);
    float angle      = detailSize / max(sampleDistance, 1.0);

    return clamp(
        (angle - CLOUD_VISUAL_LOD_MIN_ANGLE) / (CLOUD_VISUAL_LOD_FULL_ANGLE - CLOUD_VISUAL_LOD_MIN_ANGLE),
        0.0, 1.0);
}

// ── Lighting ───────────────────────────────────────────────────────────────

// Henyey-Greenstein normalised so isotropic scattering is 1, blended in as a
// lobe rather than replacing the flat response.
float resolveCloudPhase(float cosTheta) {
    float g  = CLOUD_VISUAL_FORWARD_SCATTER;
    float hg = (1.0 - g * g) / pow(max(1.0 + g * g - 2.0 * g * cosTheta, CLOUD_VISUAL_EPSILON), 1.5);

    return mix(1.0, hg, CLOUD_VISUAL_FORWARD_WEIGHT);
}

// Transmittance toward a light through the part of the layer above the
// sample, measured along the light's slant through the shell.
float resolveCloudLightTransmittance(vec3 domeNormal, vec3 lightDir, float extinction, float remainingBlocks) {
    float cosine      = max(dot(domeNormal, lightDir), CLOUD_VISUAL_LIGHT_MIN_COSINE);
    float opticalPath = extinction * remainingBlocks / cosine * CLOUD_VISUAL_SELF_SHADOW;

    return mix(
        exp(-opticalPath),
        exp(-opticalPath * CLOUD_VISUAL_MULTI_SCATTER_SCALE),
        CLOUD_VISUAL_MULTI_SCATTER_WEIGHT);
}

vec3 shadeCloudSample(
    int layer, vec3 rayDir, vec3 domeNormal, float heightFraction, float density, float thickness,
    float horizontalDistance) {
    vec4  colorParams = u_weatherLayerColor[layer];
    vec3  sunDir      = normalize(u_sunDirection);
    vec3  moonDir     = normalize(u_moonDirection);
    float h           = clamp(heightFraction, 0.0, 1.0);
    float extinction  = density * CLOUD_VISUAL_EXTINCTION_PER_BLOCK;
    float remaining   = (1.0 - h) * thickness;

    float powder = mix(1.0, 1.0 - exp(-extinction * thickness * 2.0), CLOUD_VISUAL_POWDER_STRENGTH);

    vec3 sunLight = u_sunColor * u_sunIntensity
    * resolveCloudLightTransmittance(domeNormal, sunDir, extinction, remaining)
    * resolveCloudPhase(dot(rayDir, sunDir)) * powder;

    vec3 moonLight = u_moonColor * CLOUD_VISUAL_MOON_TINT * min(u_moonIntensity, CLOUD_VISUAL_MOON_INTENSITY_MAX)
    * resolveCloudLightTransmittance(domeNormal, moonDir, extinction, remaining);

    vec3 ambient = mix(u_skyHorizonColor, u_skyZenithColor, h)
    * mix(CLOUD_VISUAL_AMBIENT_BASE, CLOUD_VISUAL_AMBIENT_TOP, h);

    float luminance = dot(colorParams.rgb, vec3(0.299, 0.587, 0.114));
    vec3  albedo    = mix(vec3(luminance), colorParams.rgb, colorParams.a);
    albedo = mix(albedo, albedo * u_skyCloudColor, CLOUD_VISUAL_SKY_TINT_STRENGTH);

    vec3 shaded = albedo * (ambient + sunLight + moonLight);

    float haze = smoothstep(
        u_weatherMapOrigin.w * CLOUD_VISUAL_HAZE_START, u_weatherMapOrigin.w, horizontalDistance);

    return mix(shaded, u_skyHorizonColor, haze * CLOUD_VISUAL_HAZE_STRENGTH);
}

#endif
