#ifndef CLOUD_VISUAL_GLSL
#define CLOUD_VISUAL_GLSL

#include "includes/SunLightData.glsl"
#include "includes/MoonLightData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherMapData.glsl"
#include "includes/WeatherMapUtility.glsl"
#include "sky/util/SkyColor.glsl"

/*
 * Shared cloud look for the weather and lighting passes: how much detail a
 * surface can afford at its distance, and how a cloud's surface is lit. The
 * look is a painted anime sky. Light falls on the surface's own normal through
 * a soft ramp that leans toward two tones, so each lobe of a cumulus blends
 * from a bright crown into a cool shaded flank, the creases between lobes
 * darken, and a column darkens toward its flat base. Shade is the
 * palette's cloud shadow, a soft lavender grey kept distinct from the sky
 * behind it, tinted a little by whichever part of the dome the surface faces,
 * so undersides lean toward the horizon and flanks toward the zenith. Flanks
 * turned toward the sun catch a bright rim, thin cloud backlit by the sun
 * glows with a silver lining, and light passes through translucent sheets to
 * their far side. Every color comes from the same SkyColorData palette the sky
 * pass draws: sunlight is tinted by the palette's cloud light, the twilight
 * glow and anti-solar belt wash the clouds facing them, clouds keep catching a
 * share of sunlight through twilight while the ground below has dimmed, and
 * distance hazes a cloud toward the sky behind it, lifted toward the pale
 * horizon, so far clouds fade into soft pastel the way a painted sky's
 * distance does.
 */

const float CLOUD_VISUAL_EPSILON = 0.001;

const float CLOUD_VISUAL_FORWARD_SCATTER      = 0.6;
const float CLOUD_VISUAL_FORWARD_WEIGHT       = 0.18;
const float CLOUD_VISUAL_LOW_SUN_FLOOR        = 0.4;
const float CLOUD_VISUAL_SKY_TINT_STRENGTH    = 0.35;
const vec3  CLOUD_VISUAL_MOON_TINT            = vec3(0.58, 0.74, 1.00);
const float CLOUD_VISUAL_MOON_INTENSITY_MAX   = 0.18;
const float CLOUD_VISUAL_HAZE_START           = 0.5;
const float CLOUD_VISUAL_HAZE_STRENGTH        = 0.6;
const float CLOUD_VISUAL_HAZE_HORIZON_SHARE   = 0.5;
const float CLOUD_VISUAL_HAZE_THINNING        = 0.6;
const float CLOUD_VISUAL_TWILIGHT_WEIGHT      = 0.8;
const float CLOUD_VISUAL_TWILIGHT_BASE        = 0.45;

const float CLOUD_VISUAL_TOON_STEP            = 0.55;
const float CLOUD_VISUAL_TOON_EDGE            = 0.06;
const float CLOUD_VISUAL_TOON_HARDNESS        = 0.15;
const float CLOUD_VISUAL_CREASE_SHADE         = 0.5;
const float CLOUD_VISUAL_BASE_SHADE           = 0.4;
const float CLOUD_VISUAL_BASE_SHADE_HEIGHT    = 0.45;
const float CLOUD_VISUAL_SHADOW_SKY_SHARE     = 0.25;
const float CLOUD_VISUAL_AMBIENT_BASE         = 0.85;
const float CLOUD_VISUAL_AMBIENT_TOP          = 1.1;
const float CLOUD_VISUAL_RIM_POWER            = 3.0;
const float CLOUD_VISUAL_RIM_WRAP             = 0.0;
const float CLOUD_VISUAL_RIM_STRENGTH         = 0.15;
const float CLOUD_VISUAL_SILVER_FOCUS         = 8.0;
const float CLOUD_VISUAL_SILVER_STRENGTH      = 0.8;

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
// per ray rather than once per surface.
struct CloudLight {
    vec3  sunDir;
    vec3  moonDir;
    vec3  sunRadiance;
    vec3  moonRadiance;
    vec3  silverRadiance;
    vec3  twilightRadiance;
    vec3  hazeColor;
};

// Henyey-Greenstein normalised so isotropic scattering is 1, blended in as a
// lobe rather than replacing the flat response.
float resolveCloudPhase(float cosTheta) {
    float g  = CLOUD_VISUAL_FORWARD_SCATTER;
    float hg = (1.0 - g * g) / pow(max(1.0 + g * g - 2.0 * g * cosTheta, CLOUD_VISUAL_EPSILON), 1.5);

    return mix(1.0, hg, CLOUD_VISUAL_FORWARD_WEIGHT);
}

CloudLight resolveCloudLight(vec3 rayDir) {
    CloudLight light;

    light.sunDir       = normalize(u_sunDirection);
    light.moonDir      = normalize(u_moonDirection);
    float sunIntensity = max(u_sunIntensity, CLOUD_VISUAL_LOW_SUN_FLOOR * clamp(u_skyBlend.x, 0.0, 1.0));
    vec3  sunlight     = u_sunColor * u_skyCloudLightColor * sunIntensity;
    float towardSun    = dot(rayDir, light.sunDir);

    light.sunRadiance    = sunlight * resolveCloudPhase(towardSun);
    light.silverRadiance = sunlight * pow(max(towardSun, 0.0), CLOUD_VISUAL_SILVER_FOCUS)
    * CLOUD_VISUAL_SILVER_STRENGTH;
    light.moonRadiance   = u_moonColor * CLOUD_VISUAL_MOON_TINT
    * min(u_moonIntensity, CLOUD_VISUAL_MOON_INTENSITY_MAX);

    light.twilightRadiance = (u_skyGlowColor * clamp(resolveSkyGlowMask(rayDir, light.sunDir) * u_skyBlend.x, 0.0, 1.0)
    + u_skyBeltColor * clamp(resolveSkyBeltMask(rayDir, light.sunDir) * u_skyBlend.y, 0.0, 1.0))
    * CLOUD_VISUAL_TWILIGHT_WEIGHT;
    light.hazeColor = mix(
        resolveSkyColor(rayDir, light.sunDir, 0.0), u_skyHorizonColor, CLOUD_VISUAL_HAZE_HORIZON_SHARE);

    return light;
}

// A layer's albedo, constant across the layer, resolved once per layer.
vec3 resolveCloudLayerAlbedo(int layer) {
    vec4  colorParams = u_weatherLayerColor[layer];
    float luminance   = dot(colorParams.rgb, vec3(0.299, 0.587, 0.114));
    vec3  albedo      = mix(vec3(luminance), colorParams.rgb, colorParams.a);

    return mix(albedo, albedo * u_skyCloudColor, CLOUD_VISUAL_SKY_TINT_STRENGTH);
}

// ── Distance ───────────────────────────────────────────────────────────────

// How far a cloud has faded into the distance, 0 near to 1 at the edge of the
// weather map. Distant cloud both hazes toward the sky and thins, so the
// horizon holds a soft pastel band rather than a speckle of clouds too small
// to resolve.
float resolveCloudHaze(float horizontalDistance) {
    float reach = resolveWeatherMapReach();
    return smoothstep(reach * CLOUD_VISUAL_HAZE_START, reach, horizontalDistance);
}

// Share of a cloud's opacity left at its distance.
float resolveCloudDistanceOpacity(float horizontalDistance) {
    return 1.0 - resolveCloudHaze(horizontalDistance) * CLOUD_VISUAL_HAZE_THINNING;
}

// ── Toon ───────────────────────────────────────────────────────────────────

// How squarely a surface faces a light, 0 turned away to 1 facing it, wrapped
// so light reaches around a bump's flank. A translucent column also takes
// light through from its far side, and even a partly translucent one glows
// with it, so a thin sheet seen from below still reads bright.
float resolveCloudFacing(vec3 normal, vec3 lightDir, float translucency) {
    float cosine = dot(normal, lightDir);
    return mix(cosine, abs(cosine), sqrt(translucency)) * 0.5 + 0.5;
}

// Settles facing into the lit (1) and shaded (0) tone joined by a soft edge,
// keeping a little of the gradient so the flat tones still read painted, not
// printed.
float resolveCloudToon(float facing) {
    float band = smoothstep(
        CLOUD_VISUAL_TOON_STEP - CLOUD_VISUAL_TOON_EDGE,
        CLOUD_VISUAL_TOON_STEP + CLOUD_VISUAL_TOON_EDGE,
        facing);

    return mix(facing, band, CLOUD_VISUAL_TOON_HARDNESS);
}

// ── Shading ────────────────────────────────────────────────────────────────

// Lit color of a cloud's surface. normal faces out of the cloud, columnHeight
// runs from 0 at the column's base to 1 at its crown, translucency is the
// share of light the column lets through, crease how deep between bumps the
// surface lies, and fullness is the archetype's, since only puffy cloud
// darkens toward a base and into the creases that part one lobe from the
// next.
vec3 shadeCloudSurface(
    CloudLight light, vec3 albedo, vec3 normal, vec3 rayDir,
    float columnHeight, float translucency, float crease, float fullness, float horizontalDistance) {
    float baseShade = mix(
        1.0,
        mix(CLOUD_VISUAL_BASE_SHADE, 1.0, smoothstep(0.0, CLOUD_VISUAL_BASE_SHADE_HEIGHT, columnHeight))
        * mix(1.0, CLOUD_VISUAL_CREASE_SHADE, crease),
        fullness);

    float sunTone  = resolveCloudToon(resolveCloudFacing(normal, light.sunDir, translucency) * baseShade);
    float moonTone = resolveCloudToon(resolveCloudFacing(normal, light.moonDir, translucency) * baseShade);

    vec3 skyFaced = mix(u_skyHorizonColor, u_skyZenithColor, clamp(normal.y * 0.5 + 0.5, 0.0, 1.0));
    vec3 shade    = mix(u_skyCloudShadowColor, skyFaced, CLOUD_VISUAL_SHADOW_SKY_SHARE)
    * mix(CLOUD_VISUAL_AMBIENT_BASE, CLOUD_VISUAL_AMBIENT_TOP, baseShade);
    vec3 lit      = max(light.sunRadiance, shade);

    vec3 twilight = light.twilightRadiance * mix(CLOUD_VISUAL_TWILIGHT_BASE, 1.0, baseShade);

    float rim = pow(1.0 - clamp(dot(normal, -rayDir), 0.0, 1.0), CLOUD_VISUAL_RIM_POWER)
    * clamp(dot(normal, light.sunDir) + CLOUD_VISUAL_RIM_WRAP, 0.0, 1.0);

    vec3 shaded = albedo * (mix(shade, lit, sunTone) + light.moonRadiance * moonTone + twilight)
    + light.sunRadiance * rim * CLOUD_VISUAL_RIM_STRENGTH
    + light.silverRadiance * translucency;

    return mix(shaded, light.hazeColor, resolveCloudHaze(horizontalDistance) * CLOUD_VISUAL_HAZE_STRENGTH);
}

#endif
