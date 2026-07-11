#ifndef SKY_CLOUD_UTILITY_GLSL
#define SKY_CLOUD_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

/*
* Sky-dome-only volumetric toon-cloud primitives. Sole consumer:
 * sky/util/Clouds.glsl's own raymarch over an unbounded, view-ray-driven
 * layer. This is the sky's independent copy of the shape/shading idea
 * clouds/util/VolumetricCloudUtility.glsl implements for physical,
 * per-instance box-bounded cloud objects — see that file's own doc
 * comment for why the two were split apart rather than sharing one
 * function set (a bounded AABB raymarch and an unbounded direction-driven
 * one need genuinely different domain handling; forcing one function to
 * serve both meant neither could be tuned for what it actually needed).
 * The two share only NoiseUtility.glsl's low-level primitives and the
 * CPU-side CloudData/WeatherHandle values that feed them — never the same
 * shading function.
 *
 * This replaces the old shared sky/util/CloudShapeUtility.glsl include —
 * that file is now dead (nothing includes it any more) and should be
 * deleted.
 *
 * Unlike the physical version, there is no box to taper a silhouette
 * against — silhouetteSoftness here only softens the noise-threshold
 * crossing itself (see the edgeSoftness mapping in
 * sampleSkyCloudDensity()), driven by the ACTUAL resolved archetype's own
 * silhouetteSoftness field rather than one fixed sky-wide constant, so a
 * soft, hazy-edged Stratus genuinely reads differently from a
 * crisper-edged Nimbus.
 */

// Cheap two-axis domain warp — offsets a sample position using a second,
// decorrelated noise field scaled by `strength`. Only X/Z are warped —
// vertical warp buys little for a thin sky layer and costs a full extra
// noise evaluation per sample. Uses gradient noise so the warp itself is
// smooth — a grainy warp field shows up directly as grain in the result.
vec3 skyWarpDomain(vec3 p, float strength, vec3 seedOffset) {
    float wx = gradientNoise3D(p.yzx * 0.7 + seedOffset);
    float wz = gradientNoise3D(p.zxy * 0.7 + seedOffset + 11.3);
    return p + vec3(wx, 0.0, wz) * strength;
}

/*
* Vertical density gradient within the sky layer. heightT is the caller's
 * own elevation-angle proxy (see Clouds.glsl) — real clouds condense at a
 * fairly consistent altitude, giving a comparatively sharp flat base that
 * erodes gradually into clear air above, never the reverse. coverageBias
 * shifts the top-erosion point higher for denser/stormier resolved
 * weather, so a storm reads as a thicker, taller deck than a fair-weather
 * puff without the caller ever special-casing that difference.
 */
float skyHeightGradient(float heightT, float coverageBias) {
    float baseCutoff = 0.06;
    float baseRamp = smoothstep(baseCutoff, baseCutoff + 0.14, heightT);

    float topStart = mix(0.50, 0.88, clamp(coverageBias, 0.0, 1.0));
    float topRamp = 1.0 - smoothstep(topStart, 1.0, heightT);

    return baseRamp * topRamp;
}

/*
* Resolves a single sky-layer density sample in [0,1] at a world-scale
 * raymarch position. coverageBias is the CALLER-COMBINED bias (live
 * weather coverage blended with this direction's own resolved archetype
 * coverageBias — see Clouds.glsl's effectiveCoverageBias) — higher bias
 * means more of the noise field reads as cloud. silhouetteSoftness is
 * this direction's own resolved archetype value, mapped here into an
 * actual edge-softness span for the threshold smoothstep. seed
 * decorrelates this sample from every other direction sampling the same
 * nominal position, and timeSeconds drives a slow, near-imperceptible
 * internal drift so the sky isn't perfectly static even at zero wind.
 */
float sampleSkyCloudDensity(
    vec3 worldPos,
    float heightT,
    float noiseScale,
    float warpStrength,
    float detailJitter,
    float coverageBias,
    float silhouetteSoftness,
    float seed,
    float timeSeconds) {
    // Stretch the horizontal sampling domain the higher up this sample
    // sits — a stretched (lower-frequency) domain reads as long, thin
    // streaks; a compact one reads as rounded puffs. Purely a function of
    // heightT, so this falls out naturally rather than needing a fixed
    // per-archetype "cloud type" switch.
    float stretch = mix(1.0, 2.4, clamp(heightT, 0.0, 1.0));

    vec3 seedOffset = vec3(seed * 173.13, seed * 57.31, seed * 91.7);
    vec3 stretchedPos = worldPos * vec3(1.0 / stretch, 1.0, 1.0 / stretch);
    vec3 coord = stretchedPos * noiseScale + seedOffset + vec3(0.0, 0.0, timeSeconds * 0.003);

    vec3 warped = skyWarpDomain(coord, warpStrength, seedOffset);

    // Macro billow (gradient FBM) blended with a cellular detail layer for
    // the bumpy, cauliflower-like edge real clouds show.
    float macro = fbmGradient3D(warped);
    float bump = worleyFbm3D(warped * 3.2 + seedOffset.yzx);
    float n = clamp(mix(macro, bump, 0.35) + (macro - 0.5) * detailJitter, 0.0, 1.0);

    float edgeSoftness = clamp(silhouetteSoftness * 1.5, 0.05, 0.35);
    float threshold = mix(0.74, 0.22, clamp(coverageBias, 0.0, 1.0));
    float shape = smoothstep(threshold - edgeSoftness, threshold + edgeSoftness, n);

    return shape * skyHeightGradient(heightT, coverageBias);
}

/*
* Real-light toon shading for a sky-layer raymarch sample. heightT is this
 * sample's height within the sky layer (0 = base, 1 = top). lightLift is
 * a cheap one-tap self-shadow term — density at this sample minus density
 * one small step toward the light, rescaled to [0,1] by the caller — near
 * 1 means this point faces the light with little material in the way,
 * near 0 means it's buried in shadow under more cloud. Both are blended
 * into one "how lit is this point" value, then posterized into toonBands,
 * using every shading parameter resolved for THIS fragment's own blended
 * cloud archetype (see Clouds.glsl) rather than a fixed constant — this is
 * what makes a Nimbus storm genuinely read as darker/flatter and a
 * Cumulus puff genuinely read as brighter/rounder, direction by direction.
 */
vec3 shadeSkyCloudLit(
    vec3 baseColor,
    vec3 topColor,
    vec3 shadowColor,
    vec3 lightColor,
    float lightIntensity,
    float heightT,
    float lightLift,
    float density,
    int toonBands,
    float shadeStrength,
    float rimLightStrength,
    float ambientOcclusionStrength,
    float brightnessMultiplier) {
    float litAmount = clamp(heightT * 0.5 + lightLift * 0.5, 0.0, 1.0);

    float bands = max(float(toonBands), 1.0);
    float banded = floor(litAmount * bands) / max(bands - 1.0, 1.0);

    vec3 lit = mix(baseColor, topColor, banded);
    vec3 shaded = mix(lit, shadowColor, (1.0 - banded) * shadeStrength);

    float ao = mix(1.0 - ambientOcclusionStrength, 1.0, banded);
    shaded *= ao;
    shaded = mix(shadowColor, shaded, clamp(density * 2.0, 0.0, 1.0));

    // Rim — thin, wispy edges (low density) catch extra light, the same
    // silhouette glow a real backlit cloud shows.
    float rim = (1.0 - clamp(density * 2.0, 0.0, 1.0)) * rimLightStrength;
    shaded += lightColor * lightIntensity * rim;

    // Tint the whole result toward the dominant light's own color/
    // intensity so a cloud under a warm sunset or a dim moon genuinely
    // reads differently, not just brighter/dimmer.
    shaded *= mix(vec3(1.0), lightColor, 0.35) * max(lightIntensity, 0.15);

    return shaded * brightnessMultiplier;
}

#endif