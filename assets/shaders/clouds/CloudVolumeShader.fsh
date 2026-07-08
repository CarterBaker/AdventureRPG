#version 330 core

in vec3  vWorldPos;
in vec3  vLocalPos;
in vec3  vNormal;
in vec2  vUV;
in float vRandomSeed;
in float vFadeAlpha;

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

    float alpha = shapeMask * u_cloudDensity * vFadeAlpha;

    fragColor = vec4(finalColor, alpha);
}