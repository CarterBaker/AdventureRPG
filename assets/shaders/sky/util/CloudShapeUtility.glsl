#ifndef CLOUD_SHAPE_UTILITY_GLSL
#define CLOUD_SHAPE_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

/*
* Shared procedural volumetric toon-cloud primitives. This is the single
 * "what does a cloud actually look like" function set, used by BOTH the
 * sky dome's distant weather preview (sky/util/Clouds.glsl) and the
 * physical near-field cloud objects (clouds/CloudVolumeShader.fsh).
 * Neither caller implements its own cloud shape or shading — they only
 * differ in what geometry they raymarch this density field through (an
 * unbounded view-ray-driven layer for the sky dome; a bounded per-instance
 * box for physical clouds). Keeping the actual density/shading math in
 * exactly one place is what guarantees the two layers can never visually
 * drift apart from each other, per the requirement that the sky and the
 * physical cloud layer always represent the same simulation.
 *
 * Every sample taken by either caller is a world-space (or world-scale)
 * position, so the field is genuinely 3D and shifts continuously with
 * true position — never a repeating 2D tile — which is what eliminates
 * the visible repetition a fixed-pattern grid would otherwise show.
 *
 * Stage 1 rework — height-shaped density + real lighting
 * -------------------------------------------------------
 * Two things were missing before this pass, both directly responsible for
 * the "cheap, flat, just noise" look reported against the sky dome:
 *
 * 1. sampleCloudDensity() now takes a `heightT` parameter — the caller's
 *    own normalized "how high up within this cloud/layer is this sample"
 *    value (a physical cloud's true position within its own box; the sky
 *    dome's elevation-angle proxy — see Clouds.glsl) — and uses it for
 *    TWO things: heightGradient() shapes density into a comparatively
 *    sharp, flat cloud base with a softer, eroding top (real clouds have
 *    a floor discontinuity but never the exact opposite — a hard top —
 *    so the top is deliberately the softer of the two edges), and
 *    stretch() widens the sampling domain horizontally the higher up a
 *    sample sits, so shapes read as long, thin streaks near the top of
 *    the layer and compact, rounded puffs near the bottom. Both effects
 *    are pure functions of heightT and the resolved weather's own
 *    coverageBias — nothing here is a fixed vertical band a caller has
 *    to impose from the outside; a dense, high-coverage storm naturally
 *    reads as a thicker, taller deck than a wisp of fair-weather cumulus,
 *    purely because coverageBias shifts where heightGradient's own
 *    top-erosion point sits.
 *
 * 2. shadeCloudLit() — already written for exactly this purpose but never
 *    actually called by anything — is now the sky dome's real shading
 *    path (see Clouds.glsl), so distant clouds react to the same sun/moon
 *    direction, color, and intensity the physical cloud objects already
 *    did, and are tinted using the sky's own horizon/zenith color rather
 *    than flat white/black toon bands.
 *
 * Macro shape now comes from fbmGradient3D() (gradient/Perlin noise)
 * blended with worleyFbm3D() (cellular noise) — see NoiseUtility.glsl's
 * own doc comment for why value noise alone read as smooth and grainy
 * rather than properly billowing and bumpy-edged.
 */

// Cheap two-axis domain warp — offsets a sample position using a second,
// decorrelated noise field scaled by `strength`. Only X/Z are warped
// (matching the technique the old sky-dome warp used) since vertical
// warp buys little for a thin cloud layer but costs a full extra noise
// evaluation per sample. Uses gradient noise rather than value noise so
// the warp itself is smooth — a grainy warp field shows up directly as
// grain in the warped shape.
vec3 warpCloudDomain(vec3 p, float strength, vec3 seedOffset) {
    float wx = gradientNoise3D(p.yzx * 0.7 + seedOffset);
    float wz = gradientNoise3D(p.zxy * 0.7 + seedOffset + 11.3);
    return p + vec3(wx, 0.0, wz) * strength;
}

/*
* Vertical density gradient within a cloud's own layer. heightT is
 * [0, 1]: 0 at the cloud's own base, 1 at the top of its own vertical
 * extent (a physical cloud's own box height, or the sky dome's visible-
 * elevation proxy). Real clouds condense at a fairly consistent altitude
 * within a given layer, giving them a comparatively sharp flat base, then
 * erode gradually into clear air above — never the reverse. coverageBias
 * is the same live weather value already driving sampleCloudDensity's
 * threshold; a denser/stormier weather pushes the top-erosion point
 * higher, so a storm deck reads as thicker and taller than a fair-weather
 * puff without either caller having to special-case that difference.
 */
float heightGradient(float heightT, float coverageBias) {
    float baseCutoff = 0.06;
    float baseRamp = smoothstep(baseCutoff, baseCutoff + 0.14, heightT);

    float topStart = mix(0.50, 0.88, clamp(coverageBias, 0.0, 1.0));
    float topRamp = 1.0 - smoothstep(topStart, 1.0, heightT);

    return baseRamp * topRamp;
}

/*
* Resolves a single density sample in [0,1] at a world-scale position.
 * coverageBias shifts the noise threshold separating "inside" from
 * "empty sky" — higher bias means more of the noise field reads as
 * cloud, and this is exactly the live, per-region weather coverage value
 * each caller already has on hand, so density genuinely tracks the
 * simulation rather than a fixed archetype look. edgeSoftness controls
 * how gradual that threshold transition is. detailJitter layers a
 * smaller, higher-frequency wobble on top of the main domain warp for
 * fine cauliflower-like detail. seed decorrelates this sample from every
 * other cloud — or sky direction — sampling the same nominal position,
 * and timeSeconds drives a slow, near-imperceptible internal drift so a
 * stationary cloud still isn't perfectly static. heightT drives both the
 * horizontal stretch and the vertical heightGradient() falloff — see the
 * class comment above.
 */
float sampleCloudDensity(
    vec3 worldPos,
    float heightT,
    float noiseScale,
    float warpStrength,
    float detailJitter,
    float coverageBias,
    float edgeSoftness,
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

    vec3 warped = warpCloudDomain(coord, warpStrength, seedOffset);

    // Macro billow (gradient FBM) blended with a cellular detail layer for
    // the bumpy, cauliflower-like edge real clouds show — see
    // NoiseUtility.glsl's own doc comment.
    float macro = fbmGradient3D(warped);
    float bump = worleyFbm3D(warped * 3.2 + seedOffset.yzx);
    float n = clamp(mix(macro, bump, 0.35) + (macro - 0.5) * detailJitter, 0.0, 1.0);

    float threshold = mix(0.74, 0.22, clamp(coverageBias, 0.0, 1.0));
    float shape = smoothstep(threshold - edgeSoftness, threshold + edgeSoftness, n);

    return shape * heightGradient(heightT, coverageBias);
}

/*
* Height-driven toon shading. A cloud's underside sits in shadowColor,
 * its top catches topColor, and toonBands posterizes the blend into
 * discrete steps rather than a smooth gradient — the same "ring/core"
 * shading idea the old puff system used, just driven by a genuine
 * height measure instead of a 2D circle radius. heightT is [0,1] and is
 * whatever "how high within this cloud" value the caller has on hand — a
 * physical instance's own local Y, or a raymarch-depth proxy for the sky
 * dome. density gently pulls the thinnest, wispiest samples back toward
 * shadowColor so edges never read as flatly lit as the core.
 *
 * Retained for reference/fallback use — the sky dome now calls
 * shadeCloudLit() instead (see Clouds.glsl) so it reacts to real sun/moon
 * lighting rather than a fixed toon ramp.
 */
vec3 shadeCloudToon(
    vec3 baseColor,
    vec3 topColor,
    vec3 shadowColor,
    float heightT,
    float density,
    int toonBands,
    float shadeStrength,
    float ambientOcclusionStrength,
    float brightnessMultiplier) {
    float bands = max(float(toonBands), 1.0);
    float banded = floor(clamp(heightT, 0.0, 1.0) * bands) / max(bands - 1.0, 1.0);

    vec3 lit = mix(baseColor, topColor, banded);
    vec3 shaded = mix(lit, shadowColor, (1.0 - banded) * shadeStrength);

    float ao = mix(1.0 - ambientOcclusionStrength, 1.0, banded);
    shaded *= ao;
    shaded = mix(shadowColor, shaded, clamp(density * 2.0, 0.0, 1.0));

    return shaded * brightnessMultiplier;
}

/*
* Unlit (shape-only) toon shading for a raymarched PHYSICAL cloud sample —
 * used by clouds/CloudVolumeShader.fsh, which writes into the same
 * deferred G-buffer every opaque terrain fragment writes into
 * (gAlbedo/gNormal/gMaterial). The shared Lighting.fsh pass reads gAlbedo
 * as raw unlit surface color and applies real sun/moon/ambient lighting
 * to it — exactly once, using gNormal for the directional terms and
 * gMaterial.b for ambient occlusion. This function must therefore never
 * bake a light's own color or intensity into its result, or that pixel
 * gets lit a second time by Lighting.fsh on top of whatever this function
 * already did.
 *
 * lightLift is still real light-DIRECTION shape information — a self-
 * shadow density tap toward wherever the sun/moon actually is (see the
 * call site) — not a radiance value, so folding it into the toon band
 * alongside heightT has no relighting risk, any more than baking a
 * vertex's own ambient occlusion into gMaterial.b does for terrain.
 * outAO returns that same banded occlusion term so the caller can
 * accumulate a real ambient-occlusion value into gMaterial.b instead of
 * a hardcoded "fully exposed" constant.
 */
vec3 shadeCloudUnlit(
    vec3 baseColor,
    vec3 topColor,
    vec3 shadowColor,
    float heightT,
    float lightLift,
    float density,
    int toonBands,
    float shadeStrength,
    float ambientOcclusionStrength,
    float brightnessMultiplier,
    out float outAO) {
    float litAmount = clamp(heightT * 0.5 + lightLift * 0.5, 0.0, 1.0);

    float bands = max(float(toonBands), 1.0);
    float banded = floor(litAmount * bands) / max(bands - 1.0, 1.0);

    vec3 lit = mix(baseColor, topColor, banded);
    vec3 shaded = mix(lit, shadowColor, (1.0 - banded) * shadeStrength);

    float ao = mix(1.0 - ambientOcclusionStrength, 1.0, banded);
    shaded *= ao;
    shaded = mix(shadowColor, shaded, clamp(density * 2.0, 0.0, 1.0));

    outAO = ao;
    return shaded * brightnessMultiplier;
}

/*
* Ray/AABB slab intersection, used by any raymarching caller that bounds
 * its march to a physical volume (currently: CloudVolumeShader.fsh's
 * per-instance box). Returns (tNear, tFar) along rayDir from rayOrigin —
 * tFar < tNear means the ray misses the box entirely, which callers
 * marching from a point already known to be on the box's own surface
 * (the common case here, since GL only rasterizes a convex box's outer
 * faces) should never actually see, but is left for the caller to guard
 * against via a simple max(tFar - tNear, 0.0) length check.
 */
vec2 intersectAABB(vec3 rayOrigin, vec3 rayDir, vec3 boxMin, vec3 boxMax) {
    vec3 invDir = 1.0 / rayDir;
    vec3 t0 = (boxMin - rayOrigin) * invDir;
    vec3 t1 = (boxMax - rayOrigin) * invDir;
    vec3 tSmall = min(t0, t1);
    vec3 tBig   = max(t0, t1);
    float tNear = max(max(tSmall.x, tSmall.y), tSmall.z);
    float tFar  = min(min(tBig.x, tBig.y), tBig.z);
    return vec2(tNear, tFar);
}

/*
* Real-light toon shading for a raymarched sample. This is now the sky
 * dome's own shading path (see Clouds.glsl) as well as being available
 * for any future forward-shaded/direct caller (anything that draws
 * straight to a final color target rather than through the deferred
 * G-buffer). NOT called by CloudVolumeShader.fsh — see shadeCloudUnlit()
 * above for why a G-buffer writer must never use this.
 *
 * - heightT: this sample's height within its own layer (0 = base,
 *   1 = top) — see heightGradient()'s own doc comment.
 * - lightLift: a cheap one-tap self-shadow term, expected pre-computed
 *   and pre-clamped to [0,1] by the caller (density at this sample minus
 *   density one small step toward the light, rescaled) — near 1 means
 *   this point sits on the side facing the light with little material in
 *   the way, near 0 means it's buried in shadow under more cloud.
 *
 * Both are blended into one "how lit is this point" value, then
 * posterized into toonBands exactly like shadeCloudToon()'s ring/core
 * look, so the two functions read as the same art style — just one
 * driven by fake progress through a march, the other by a real light.
 */
vec3 shadeCloudLit(
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