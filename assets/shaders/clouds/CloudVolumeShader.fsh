#version 330 core

in vec3  vWorldPos;
in vec3  vLocalPos;
in vec3  vNormal;
in vec2  vUV;
in float vRandomSeed;
in float vFadeAlpha;
in float vIntensity;

out vec4 fragColor;

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"

// Baked once per material clone in CloudRenderSystem.resolveMaterial() —
// this archetype's own color/shape numbers, shared by every instance
// drawn through this material.
uniform vec3  u_cloudColor;
uniform float u_cloudDensity;
uniform float u_cloudEdgeSoftness;
uniform float u_cloudPuffJitter;

// Volumetric toon shading uniforms — baked per material clone alongside the
// legacy fields above (see CloudRenderSystem.resolveMaterial()), but not yet
// read by the shading logic below. This stage is pure data plumbing so the
// upcoming raymarched volumetric toon rework only has to change GLSL, not
// the Java baking path. See CloudData's own doc comment for what each of
// these drives once consumed.
uniform vec3  u_cloudTopColor;
uniform int   u_cloudToonBands;
uniform float u_cloudDensityNoiseScale;
uniform float u_cloudNoiseWarpStrength;
uniform float u_cloudCoverageBias;
uniform float u_cloudSilhouetteSoftness;
uniform vec3  u_cloudShadowColor;
uniform float u_cloudShadeStrength;
uniform float u_cloudRimLightStrength;
uniform float u_cloudAmbientOcclusionStrength;
uniform float u_cloudBrightnessMultiplier;

// Floor applied to vIntensity's density contribution (see main()) — a
// forming/weakening cloud reads as thin and wispy, never as fully
// invisible purely from intensity. True existence pop-in/pop-out is
// vFadeAlpha's job alone (see CloudVolumeShader.vsh's horizonFade and
// OverheadCellStruct.fadeAlpha) — intensity only ever thins density on
// top of whatever vFadeAlpha already decided about existence.
const float INTENSITY_ALPHA_FLOOR = 0.4;

/*
* Stamps a single soft, roughly circular "puff" onto the card's local XZ
 * footprint (vLocalPos ranges -0.5..0.5, so radialDist is 0 at the card's
 * center and ~1.4 at its corners — corners fall outside the cutoff and are
 * discarded, rounding the square card into a blob).
 *
 * vRandomSeed offsets the fbm domain per-instance so no two cards ever
 * warp identically — "shift and warp and are never the same", per the
 * design brief — and vWorldPos (rather than vLocalPos) drives that same
 * warp so a card's own internal shape stays spatially coherent as it
 * drifts with the wind, instead of swimming in place. u_time adds a slow
 * independent breathing motion on top, distinct from the wind-driven
 * translation already baked into vWorldPos upstream.
 *
 * This is still the legacy card/billboard shader — superseded by an actual
 * raymarched volumetric pass in the next stage. Left functionally
 * unchanged here except for vIntensity's density modulation; only the
 * uniform declarations above and that one addition are new.
 */
void main() {
    vec2 centered = vLocalPos.xz;
    float radialDist = length(centered) * 2.0;

    vec2 seedOffset = vec2(vRandomSeed * 173.13, vRandomSeed * 57.31);
    vec2 warpCoord = vWorldPos.xz * 0.05 + seedOffset + u_time * 0.01;
    float warp = fbmNoise2D(warpCoord) - 0.5;

    float jitteredDist = radialDist + warp * u_cloudPuffJitter;

    float core = 1.0 - smoothstep(0.35, 0.35 + u_cloudEdgeSoftness, jitteredDist);
    float shapeMask = 1.0 - smoothstep(0.75, 0.75 + u_cloudEdgeSoftness, jitteredDist);

    if (shapeMask <= 0.01)
    discard;

    vec3 ringColor = mix(u_skyHorizonColor, vec3(1.0), 0.75);
    vec3 coreColor = mix(u_cloudColor, vec3(1.0), 0.4);
    vec3 finalColor = mix(ringColor, coreColor, core);

    // Slightly darker toward the card's base than its top — a cheap stand-in
    // for the top-lit/bottom-shadowed look real clouds have, without a
    // second light pass.
    float heightShade = clamp(vLocalPos.y, 0.0, 1.0);
    finalColor = mix(finalColor * 0.85, finalColor, heightShade);

    // How strongly this cell's weather is currently expressed — see
    // WeatherBandStruct.getPrimaryIntensity() / OverheadManager.advanceIntensity().
    // Never lets density hit true zero on its own; a cell whose intensity
    // has genuinely bottomed out is already being retired through
    // vFadeAlpha instead.
    float intensityFactor = mix(INTENSITY_ALPHA_FLOOR, 1.0, clamp(vIntensity, 0.0, 1.0));

    float alpha = shapeMask * u_cloudDensity * intensityFactor * vFadeAlpha;

    fragColor = vec4(finalColor, alpha);
}