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
//
// FOG_MAX_AMOUNT is a hard ceiling, independent of whatever the near/far
// weights below add up to. Whatever the deferred pass does with fogT
// (mix(litColor, fogColor, fogT) is the expected use), this guarantees the
// result is never more than FOG_MAX_AMOUNT fog color — at least
// (1.0 - FOG_MAX_AMOUNT) of the final pixel is always the surface's own
// lit/albedo color. That matters most right at the render-distance edge,
// where the far-ring "distant terrain rise" (see StandardSurface.tes) is
// biggest and any remaining geometry seams are most visible — at 0.80 the
// result reads as strongly sky-tinted without fully flattening into a
// featureless fogColor cutout, so distant terrain still reads as ground,
// not as a hole in the world.
const float FOG_NEAR_CURVE_WEIGHT = 0.45;
const float FOG_FAR_CURVE_WEIGHT  = 0.35;
const float FOG_MAX_AMOUNT        = 0.80;

float computeFogAmount() {
    float halfD       = u_renderDistance * 0.5 - 0.5;
    float trueMax      = halfD * halfD * 2.0;
    float fogDist       = clamp(u_distanceFromCenter / trueMax, 0.0, 1.0);
    float linearDist     = sqrt(fogDist);

    float rawFog = smoothstep(0.0, 0.5, linearDist) * FOG_NEAR_CURVE_WEIGHT
    + smoothstep(0.5, 1.0, linearDist) * FOG_FAR_CURVE_WEIGHT;

    // Explicit clamp rather than relying on the two weights above happening
    // to sum to FOG_MAX_AMOUNT — so retuning either weight later can never
    // silently push the total past the cap.
    return min(rawFog, FOG_MAX_AMOUNT);
}

vec3 applyAtmosphericFog(vec3 litColor) {
    float fogT = computeFogAmount();
    vec3 fogColor = u_skyHorizonColor + 0.04;
    return mix(litColor, fogColor, fogT);
}

#endif