#ifndef ATMOSPHERIC_FOG_GLSL
#define ATMOSPHERIC_FOG_GLSL

#include "includes/GridCoordinateData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SettingsData.glsl"

/*
* Canonical fog distance curve. Single source of truth — StandardSurface.fsh
 * calls computeFogAmount() directly (it has valid per-chunk distance data
 * during its own draw call) and packs the result into gMaterial.r so the
 * deferred Lighting.fsh pass can read it later.
 *
 * applyAtmosphericFog() is NOT called from Lighting.fsh. u_distanceFromCenter
 * is per-chunk data, valid only during that chunk's own draw call — by the
 * time the deferred lighting pass runs (one fullscreen draw, after every
 * chunk has already been rendered into the G-buffer), this UBO holds
 * whatever was last written by the final chunk drawn, not the correct value
 * for an arbitrary screen pixel. That's why fogT has to be computed here,
 * in the forward/per-chunk stage, and carried through the G-buffer instead.
 * applyAtmosphericFog() stays available for any future forward-shaded
 * material that computes lighting and distance in the same pass.
 */

// Raised from the original 0.25 / 0.15 — fog was barely visible at the old
// weights. Single source of truth for both call sites below.
const float FOG_NEAR_CURVE_WEIGHT = 0.45;
const float FOG_FAR_CURVE_WEIGHT  = 0.35;

float computeFogAmount() {
    float halfD       = u_renderDistance * 0.5 - 0.5;
    float trueMax      = halfD * halfD * 2.0;
    float fogDist       = clamp(u_distanceFromCenter / trueMax, 0.0, 1.0);
    float linearDist     = sqrt(fogDist);

    return smoothstep(0.0, 0.5, linearDist) * FOG_NEAR_CURVE_WEIGHT
    + smoothstep(0.5, 1.0, linearDist) * FOG_FAR_CURVE_WEIGHT;
}

vec3 applyAtmosphericFog(vec3 litColor) {
    float fogT = computeFogAmount();
    vec3 fogColor = u_skyHorizonColor + 0.04;
    return mix(litColor, fogColor, clamp(fogT, 0.0, 0.9));
}

#endif