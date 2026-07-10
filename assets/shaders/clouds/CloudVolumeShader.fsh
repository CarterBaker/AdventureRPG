#version 330 core

in vec3  vWorldPos;
in vec3  vLocalPos;
in vec3  vNormal;
in vec2  vUV;
in float vRandomSeed;
in float vFadeAlpha;
in float vIntensity;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

#include "includes/CameraData.glsl"
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
* ============================================================================
 * STAGE 1 FIX — this pass now writes a COMPLETE G-buffer output (albedo,
 * normal, material) and real depth, exactly like StandardSurfaceShader.fsh
 * does for terrain, instead of a single un-located `fragColor` blended via
 * GL_SRC_ALPHA/GL_ONE_MINUS_SRC_ALPHA with depth writes disabled.
 *
 * That old setup is why no overhead clouds were ever actually visible:
 * - With no explicit output location, the lone `fragColor` bound to
 *   attachment 0 (gAlbedo) only. gNormal/gMaterial were never written, and
 *   with depth writes disabled the depth buffer kept its cleared value of
 *   1.0 wherever a cloud drew over open sky — i.e. almost everywhere a
 *   cloud actually is, since clouds sit far above terrain.
 * - LightingShader.fsh's very first line is `if (depth >= 1.0) { fragColor
 *   = vec4(0,0,0,0); return; }` — its shorthand for "nothing was drawn
 *   here." Since the cloud never wrote depth, every cloud-over-sky pixel
 *   hit that branch and was silently dropped from the final lit image.
 * - Anywhere a cloud DID happen to sit in front of real terrain, its own
 *   color blended into gAlbedo, but gNormal/gMaterial were still the
 *   terrain's, so the lighting pass relit "cloud color" using the
 *   terrain's own normal — never a coherent cloud look either way.
 *
 * The fix mirrors how every other opaque-ish surface in this deferred
 * renderer already behaves (see StandardSurfaceShader.fsh's own doc
 * comment): output alpha = 1.0 on every channel, every time, and do any
 * translucency blending manually against a known background BEFORE
 * writing, rather than leaning on the GPU's blend stage — the G-buffer
 * has no idea what "behind" a translucent fragment means once the
 * deferred lighting pass runs a frame later, so GL blending against
 * whatever the buffer happened to already hold (black, most of the time)
 * produced a dim, black-tinted cloud look even where it partially worked.
 * The known background used here is an approximation of the sky color
 * this shader can already sample (u_skyHorizonColor/u_skyZenithColor),
 * blended by the fragment's height within the cloud's own local cube, so
 * a soft edge fades toward "roughly sky-colored" instead of "toward
 * black." This is a stopgap for a card/cube proxy — Stage 3's volumetric
 * rewrite replaces it with a real raymarched silhouette.
 *
 * The puff/shape math below is otherwise UNCHANGED — still the legacy
 * card-circle look this whole effort exists to replace next. This pass
 * only exists to prove the render pipeline itself is fixed before that
 * rewrite lands.
 * ============================================================================
 *
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

    float alpha = clamp(shapeMask * u_cloudDensity * intensityFactor * vFadeAlpha, 0.0, 1.0);

    // Manual "blend" against an approximate sky background, height-blended
    // the same way the real sky pass blends horizon -> zenith — see the
    // STAGE 1 FIX note above for why this replaces GL_SRC_ALPHA blending
    // for a G-buffer write.
    vec3 approxSky = mix(u_skyHorizonColor, u_skyZenithColor, heightShade);
    vec3 blended = mix(approxSky, finalColor, alpha);

    vec3 normalView = normalize(mat3(u_view) * normalize(vNormal));

    // Cloud material packing mirrors StandardSurfaceShader's gMaterial
    // convention (r = fogT, g = specular, b = ao). Clouds don't apply the
    // terrain's distance-fog curve to themselves (r = 0), are non-shiny
    // (g = 0), and are never additionally ambient-occluded (b = 1).
    gAlbedo   = vec4(blended, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(0.0, 0.0, 1.0, 1.0);
}