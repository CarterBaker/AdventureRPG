#ifndef PAINTED_LIGHTING_GLSL
#define PAINTED_LIGHTING_GLSL

#include "includes/SkyColorData.glsl"

// Distant macro terrain, marked painted in the surface target (GBufferSurface.glsl), is lit as a painted backdrop's
// hills and mountains are. The sun falls in three flat tones, full, half and shade, each seam soft over a narrow
// range, so a range breaks into a few broad planes of light; the lit tones are warmed a little and the shade keeps a
// share of the sun cooled to the blue of a painted mountain's shadow side rather than dropping to the sky alone; the
// moon falls in two tones. The further painted terrain stands, the more it fades into the sky's horizon color, so
// range behind range layers back into the haze.

const float PAINTED_SHADE_LOW  = 0.03;
const float PAINTED_SHADE_HIGH = 0.1;
const float PAINTED_HALF_LOW   = 0.42;
const float PAINTED_HALF_HIGH  = 0.5;
const float PAINTED_HALF_SHARE = 0.6;
const vec3  PAINTED_LIT_TINT   = vec3(1.05, 1.0, 0.9);
const vec3  PAINTED_SHADE_TINT = vec3(0.6, 0.74, 1.0);
const float PAINTED_SHADE_FILL = 0.42;
const float PAINTED_HAZE_START = 160.0;
const float PAINTED_HAZE_END   = 2400.0;
const float PAINTED_HAZE_SHARE = 0.55;
const float PAINTED_HAZE_LIGHT = 1.1;

// Three tones from a diffuse term: shade, half and full, each seam soft over a narrow range
float resolvePaintedBand(float diffuse) {
    return smoothstep(PAINTED_SHADE_LOW, PAINTED_SHADE_HIGH, diffuse) * PAINTED_HALF_SHARE
        + smoothstep(PAINTED_HALF_LOW, PAINTED_HALF_HIGH, diffuse) * (1.0 - PAINTED_HALF_SHARE);
}

vec3 resolvePaintedLight(
    vec3 albedo,
    float sunDiff,
    float sunVisibility,
    float moonDiff,
    float skyAmbient,
    vec3 sunLight,
    vec3 moonLight) {

    float lit     = resolvePaintedBand(sunDiff) * sunVisibility;
    float moonLit = smoothstep(PAINTED_SHADE_LOW, PAINTED_SHADE_HIGH, moonDiff);

    vec3 direct = sunLight * albedo * PAINTED_LIT_TINT * lit + moonLight * albedo * moonLit;
    vec3 shade  = sunLight * albedo * PAINTED_SHADE_TINT * PAINTED_SHADE_FILL * (1.0 - lit);

    return albedo * skyAmbient + direct + shade;
}

// A lit color faded into the horizon's haze by its distance, as far as painted is 1; nothing where it is 0
vec3 applyPaintedHaze(vec3 lit, float fragDistance, float painted) {
    float haze = smoothstep(PAINTED_HAZE_START, PAINTED_HAZE_END, fragDistance) * PAINTED_HAZE_SHARE * painted;

    return mix(lit, u_skyHorizonColor * PAINTED_HAZE_LIGHT, haze);
}

#endif
