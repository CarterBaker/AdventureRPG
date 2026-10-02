#ifndef CLOUD_VISUAL_GLSL
#define CLOUD_VISUAL_GLSL

#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/WeatherMapUtility.glsl"
#include "sky/util/SkyColor.glsl"

/*
 * Shared cloud look for the weather pass: how much detail a surface can
 * afford at its distance, and how a cloud's surface is painted. The look is a
 * flat cartoon sky. Clouds are solid and painted in a few flat tones: the
 * light reaching the cloud gives its body color, and each tone darker mixes a
 * larger share of the sky's own blue into it, so shade always reads as sky
 * reflected in white cloud rather than grey. Which tone a surface takes
 * follows how squarely it faces the light, darkened toward the column's flat
 * base and into the creases that part one lobe from the next, so every lobe
 * of a cumulus carries a bright crown over bands of cooler shade. Wherever the
 * surface turns away from the view, the outline, it is painted in the body
 * color, ringing every cloud and every lobe with a bright edge. Every color
 * comes from the same SkyColorData palette the sky pass draws: sunlight is
 * tinted by the palette's cloud light and follows the sky's own daylight, so
 * day cloud reads white all day long, the twilight glow and anti-solar belt
 * wash the clouds facing them, clouds keep catching a share of sunlight
 * through twilight while the ground below has dimmed, and distance hazes a
 * cloud toward the sky behind it so far clouds melt into the horizon.
 */

const float CLOUD_VISUAL_EPSILON = 0.001;

const float CLOUD_VISUAL_LOW_SUN_FLOOR      = 0.7;
const float CLOUD_VISUAL_SKY_TINT_STRENGTH  = 0.35;
const vec3  CLOUD_VISUAL_MOON_TINT          = vec3(0.58, 0.74, 1.00);
const float CLOUD_VISUAL_MOON_INTENSITY_MAX = 0.18;
const float CLOUD_VISUAL_AMBIENT_SHARE      = 0.12;
const float CLOUD_VISUAL_HAZE_START         = 0.3;
const float CLOUD_VISUAL_HAZE_STRENGTH      = 1.0;
const float CLOUD_VISUAL_HAZE_HORIZON_SHARE = 0.5;
const float CLOUD_VISUAL_TWILIGHT_WEIGHT    = 0.8;

const float CLOUD_VISUAL_SHADE_ZENITH_SHARE  = 0.85;
const float CLOUD_VISUAL_SHADE_PALETTE_SHARE = 0.2;
const float CLOUD_VISUAL_TONE_LIGHT          = 0.14;
const float CLOUD_VISUAL_TONE_MID            = 0.28;
const float CLOUD_VISUAL_TONE_SHADE          = 0.44;
const float CLOUD_VISUAL_BAND_LIGHT          = 0.74;
const float CLOUD_VISUAL_BAND_MID            = 0.52;
const float CLOUD_VISUAL_BAND_SHADE          = 0.32;
const float CLOUD_VISUAL_BAND_EDGE           = 0.02;
const float CLOUD_VISUAL_OUTLINE_START       = 0.88;
const float CLOUD_VISUAL_OUTLINE_EDGE        = 0.04;
const float CLOUD_VISUAL_CREASE_SHADE        = 0.6;
const float CLOUD_VISUAL_BASE_SHADE          = 0.45;
const float CLOUD_VISUAL_BASE_SHADE_HEIGHT   = 0.4;

const int   CLOUD_VISUAL_OCTAVES_NEAR       = 4;
const int   CLOUD_VISUAL_OCTAVES_MID        = 3;
const int   CLOUD_VISUAL_OCTAVES_FAR        = 2;
const float CLOUD_VISUAL_OCTAVES_NEAR_ANGLE = 0.12;
const float CLOUD_VISUAL_OCTAVES_MID_ANGLE  = 0.04;
const float CLOUD_VISUAL_LOD_MIN_ANGLE      = 0.006;
const float CLOUD_VISUAL_LOD_FULL_ANGLE     = 0.024;

// ── Level of Detail ────────────────────────────────────────────────────────

// Octaves whose features are still wider than a pixel at this distance.
int resolveCloudOctaves(float featureSize, float sampleDistance) {
    float angle = featureSize / max(sampleDistance, 1.0);

    if (angle > CLOUD_VISUAL_OCTAVES_NEAR_ANGLE)
    return CLOUD_VISUAL_OCTAVES_NEAR;

    return angle > CLOUD_VISUAL_OCTAVES_MID_ANGLE ? CLOUD_VISUAL_OCTAVES_MID : CLOUD_VISUAL_OCTAVES_FAR;
}

// Fraction of the detail octave still larger than a pixel at this distance.
float resolveCloudDetailFade(float detailSize, float sampleDistance) {
    float angle = detailSize / max(sampleDistance, 1.0);

    return clamp(
        (angle - CLOUD_VISUAL_LOD_MIN_ANGLE) / (CLOUD_VISUAL_LOD_FULL_ANGLE - CLOUD_VISUAL_LOD_MIN_ANGLE),
        0.0, 1.0);
}

// ── Lighting ───────────────────────────────────────────────────────────────

// Everything about the lights that is constant along one ray, resolved once
// per ray rather than once per surface. bodyLight is the light a fully lit
// surface is painted in, shadeTint the sky color its shade tones lean toward,
// and sunShare how much of the light the sun rather than the moon gives, which
// decides whose direction the tones follow.
struct CloudLight {
    vec3  sunDir;
    vec3  moonDir;
    vec3  bodyLight;
    vec3  shadeTint;
    vec3  hazeColor;
    float sunShare;
};

float resolveCloudLuminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

CloudLight resolveCloudLight(vec3 rayDir) {
    CloudLight light;

    light.sunDir  = normalize(u_sunDirection);
    light.moonDir = normalize(u_moonDirection);

    float daylight     = max(u_sunIntensity, u_skyBlend.z);
    float sunIntensity = max(daylight, CLOUD_VISUAL_LOW_SUN_FLOOR * clamp(u_skyBlend.x, 0.0, 1.0));
    vec3  sunlight     = u_sunColor * u_skyCloudLightColor * sunIntensity;
    vec3  moonlight    = u_moonColor * CLOUD_VISUAL_MOON_TINT * min(u_moonIntensity, CLOUD_VISUAL_MOON_INTENSITY_MAX);
    vec3  twilight     = (u_skyGlowColor * clamp(resolveSkyGlowMask(rayDir, light.sunDir) * u_skyBlend.x, 0.0, 1.0)
    + u_skyBeltColor * clamp(resolveSkyBeltMask(rayDir, light.sunDir) * u_skyBlend.y, 0.0, 1.0))
    * CLOUD_VISUAL_TWILIGHT_WEIGHT;
    vec3  ambient      = mix(u_skyHorizonColor, u_skyZenithColor, CLOUD_VISUAL_SHADE_ZENITH_SHARE);

    float sunLuminance  = resolveCloudLuminance(sunlight);
    float moonLuminance = resolveCloudLuminance(moonlight);

    light.bodyLight = sunlight + moonlight + twilight + ambient * CLOUD_VISUAL_AMBIENT_SHARE;
    light.shadeTint = mix(ambient, u_skyCloudShadowColor, CLOUD_VISUAL_SHADE_PALETTE_SHARE);
    light.sunShare  = sunLuminance / max(sunLuminance + moonLuminance, CLOUD_VISUAL_EPSILON);
    light.hazeColor = mix(
        resolveSkyColor(rayDir, light.sunDir, 0.0), u_skyHorizonColor, CLOUD_VISUAL_HAZE_HORIZON_SHARE);

    return light;
}

// A layer's albedo, constant across the layer, resolved once per layer.
vec3 resolveCloudLayerAlbedo(int layer) {
    vec4  colorParams = u_weatherLayerColor[layer];
    float luminance   = resolveCloudLuminance(colorParams.rgb);
    vec3  albedo      = mix(vec3(luminance), colorParams.rgb, colorParams.a);

    return mix(albedo, albedo * u_skyCloudColor, CLOUD_VISUAL_SKY_TINT_STRENGTH);
}

// ── Distance ───────────────────────────────────────────────────────────────

// How far a cloud has faded into the distance, 0 near to 1 at the edge of the
// weather map, so the horizon holds a soft band of cloud melting into the sky
// rather than a speckle of clouds too small to resolve.
float resolveCloudHaze(float horizontalDistance) {
    float reach = resolveWeatherMapReach();
    return smoothstep(reach * CLOUD_VISUAL_HAZE_START, reach, horizontalDistance);
}

// ── Toon ───────────────────────────────────────────────────────────────────

// How squarely a surface faces the light painting it, 0 turned away to 1
// facing it, wrapped so light reaches around a lobe's flank.
float resolveCloudFacing(CloudLight light, vec3 normal) {
    float sunFacing  = dot(normal, light.sunDir) * 0.5 + 0.5;
    float moonFacing = dot(normal, light.moonDir) * 0.5 + 0.5;

    return mix(moonFacing, sunFacing, light.sunShare);
}

// Share of the sky's color a surface takes for its light, settled onto the
// flat tones with a narrow edge between them, so bands read painted.
float resolveCloudTone(float lighting) {
    float tone = CLOUD_VISUAL_TONE_SHADE;

    tone = mix(tone, CLOUD_VISUAL_TONE_MID, smoothstep(
        CLOUD_VISUAL_BAND_SHADE - CLOUD_VISUAL_BAND_EDGE, CLOUD_VISUAL_BAND_SHADE + CLOUD_VISUAL_BAND_EDGE, lighting));
    tone = mix(tone, CLOUD_VISUAL_TONE_LIGHT, smoothstep(
        CLOUD_VISUAL_BAND_MID - CLOUD_VISUAL_BAND_EDGE, CLOUD_VISUAL_BAND_MID + CLOUD_VISUAL_BAND_EDGE, lighting));
    tone = mix(tone, 0.0, smoothstep(
        CLOUD_VISUAL_BAND_LIGHT - CLOUD_VISUAL_BAND_EDGE, CLOUD_VISUAL_BAND_LIGHT + CLOUD_VISUAL_BAND_EDGE, lighting));

    return tone;
}

// The body color of a layer's cloud under this light with a given share of
// the sky's color mixed in.
vec3 paintCloudTone(CloudLight light, vec3 albedo, float tone) {
    return albedo * light.bodyLight * mix(vec3(1.0), light.shadeTint, tone);
}

// ── Shading ────────────────────────────────────────────────────────────────

// Painted color of a cloud's surface. normal faces out of the cloud,
// columnHeight runs from 0 at the column's base to 1 at its crown, crease is
// how deep between bumps the surface lies, and fullness is the archetype's,
// since only puffy cloud darkens toward a base and into the creases that part
// one lobe from the next.
vec3 shadeCloudSurface(
    CloudLight light, vec3 albedo, vec3 normal, vec3 rayDir,
    float columnHeight, float crease, float fullness, float horizontalDistance) {
    float baseShade = mix(
        1.0,
        mix(CLOUD_VISUAL_BASE_SHADE, 1.0, smoothstep(0.0, CLOUD_VISUAL_BASE_SHADE_HEIGHT, columnHeight))
        * mix(1.0, CLOUD_VISUAL_CREASE_SHADE, crease),
        fullness);

    float haze    = resolveCloudHaze(horizontalDistance);
    float outline = smoothstep(
        CLOUD_VISUAL_OUTLINE_START - CLOUD_VISUAL_OUTLINE_EDGE,
        CLOUD_VISUAL_OUTLINE_START + CLOUD_VISUAL_OUTLINE_EDGE,
        1.0 - abs(dot(normal, rayDir))) * (1.0 - haze);

    float tone    = resolveCloudTone(resolveCloudFacing(light, normal) * baseShade) * (1.0 - outline);
    vec3  painted = paintCloudTone(light, albedo, tone);

    return mix(painted, light.hazeColor, haze * CLOUD_VISUAL_HAZE_STRENGTH);
}

// Color of the fog filling a cloud seen from inside: its body in the light
// tone, the soft white every face of the cloud blends toward up close.
vec3 shadeCloudFog(CloudLight light, vec3 albedo) {
    return paintCloudTone(light, albedo, CLOUD_VISUAL_TONE_LIGHT);
}

#endif
