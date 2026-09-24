#ifndef SKY_COLOR_GLSL
#define SKY_COLOR_GLSL

#include "includes/SkyColorData.glsl"

/*
 * The sky seen along one direction, built entirely from SkyColorData so the
 * sky pass and the clouds read the same colors. A horizon-to-zenith gradient
 * forms the dome; the sun-side glow hugs the horizon toward the sun; the
 * anti-solar belt lays a pink band low on the opposite side at twilight; and
 * a soft halo brightens the sky around the sun. gradientShift lets a caller
 * nudge the gradient (the sky pass feeds its noise in here); pass 0.0 for
 * the plain sky.
 */

const float SKY_COLOR_ZENITH_CURVE     = 0.45;
const float SKY_COLOR_GLOW_FOCUS       = 5.0;
const float SKY_COLOR_GLOW_HEIGHT      = 4.0;
const float SKY_COLOR_BELT_FOCUS       = 1.6;
const float SKY_COLOR_BELT_LOW         = 0.0;
const float SKY_COLOR_BELT_PEAK        = 0.14;
const float SKY_COLOR_BELT_HIGH        = 0.50;
const float SKY_COLOR_HALO_FOCUS       = 48.0;
const float SKY_COLOR_HALO_WEIGHT      = 0.22;

// ── Masks ──────────────────────────────────────────────────────────────────

// How much of the sun-side glow reaches a direction: tight toward the sun,
// fading with altitude so it stays a horizon band.
float resolveSkyGlowMask(vec3 dir, vec3 sunDir) {
    float sunSide = dot(dir, sunDir) * 0.5 + 0.5;
    return pow(sunSide, SKY_COLOR_GLOW_FOCUS) * exp(-max(dir.y, 0.0) * SKY_COLOR_GLOW_HEIGHT);
}

// How much of the anti-solar belt reaches a direction: broad across the
// half of the sky facing away from the sun, in a low band above the horizon.
float resolveSkyBeltMask(vec3 dir, vec3 sunDir) {
    float antiSide = 0.5 - dot(dir, sunDir) * 0.5;
    float band     = smoothstep(SKY_COLOR_BELT_LOW, SKY_COLOR_BELT_PEAK, dir.y)
                   * (1.0 - smoothstep(SKY_COLOR_BELT_PEAK, SKY_COLOR_BELT_HIGH, dir.y));
    return pow(antiSide, SKY_COLOR_BELT_FOCUS) * band;
}

// ── Sky ────────────────────────────────────────────────────────────────────

vec3 resolveSkyColor(vec3 dir, vec3 sunDir, float gradientShift) {
    float gradientT = clamp(pow(clamp(dir.y, 0.0, 1.0), SKY_COLOR_ZENITH_CURVE) + gradientShift, 0.0, 1.0);
    vec3  sky       = mix(u_skyHorizonColor, u_skyZenithColor, gradientT);

    sky = mix(sky, u_skyGlowColor, clamp(resolveSkyGlowMask(dir, sunDir) * u_skyBlend.x, 0.0, 1.0));
    sky = mix(sky, u_skyBeltColor, clamp(resolveSkyBeltMask(dir, sunDir) * u_skyBlend.y, 0.0, 1.0));

    float haloLight = max(u_skyBlend.z, clamp(u_skyBlend.x, 0.0, 1.0)) * (1.0 - u_skyBlend.w);
    float halo      = pow(max(dot(dir, sunDir), 0.0), SKY_COLOR_HALO_FOCUS) * SKY_COLOR_HALO_WEIGHT * haloLight;
    vec3  haloColor = mix(vec3(1.0), u_skyGlowColor, clamp(u_skyBlend.x, 0.0, 1.0));

    return sky + haloColor * halo;
}

#endif
