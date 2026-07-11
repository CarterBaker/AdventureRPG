#ifndef VOLUMETRIC_CLOUD_UTILITY_GLSL
#define VOLUMETRIC_CLOUD_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

/*
* Procedural volumetric toon-cloud primitives for PHYSICAL cloud objects
 * only — clouds/CloudVolumeShader.fsh's per-instance raymarched box.
 *
 * This used to be shared with the sky dome's own distant-weather preview
 * (sky/util/Clouds.glsl) via a single CloudShapeUtility.glsl, on the
 * theory that "the sky and the physical layer must always agree on what
 * a cloud looks like" required sharing the actual shading code. In
 * practice the two callers raymarch fundamentally different geometry —
 * an unbounded, view-ray-driven layer for the sky; a small, bounded,
 * per-instance AABB here — and forcing one density/shading function to
 * serve both meant neither could be tuned for what it actually needs:
 * the sky never had a real box to taper against, and this file never had
 * a reason to model a raymarch layer thickness. The two now share only
 * the same CPU-side weather data (WeatherHandle/CloudHandle) and the same
 * low-level noise primitives (NoiseUtility.glsl) — never the same shading
 * function. See sky/util/SkyCloudUtility.glsl (landing in the next stage)
 * for the sky dome's own, independent copy of this idea.
 *
 * Silhouette fix
 * --------------
 * The previous shared version raymarched a density field that was simply
 * CLIPPED by this instance's AABB — nothing in the density calculation
 * ever anticipated the box boundary, so a sufficiently dense/thick cloud
 * (high coverageBias, which pushes the noise threshold down until most of
 * the field reads as "inside") stayed just as dense right up against the
 * box wall as it was in the interior. The raymarch then simply stopped at
 * that wall, which is exactly what a hard-edged box looks like — this is
 * the root cause of the "clouds just look like boxes" complaint.
 * silhouetteMask() below fixes this by tapering density to zero well
 * before the true AABB boundary, in a jittered (not perfectly circular)
 * radius around the box's own horizontal center, so the visible silhouette
 * reads as a rounded, irregular puff sitting inside its bounding box
 * rather than the box itself. Vertical shape is untouched — heightGradient()
 * already gives clouds a flat-ish base and eroding top, which is correct
 * cloud behavior and was never the source of the boxy look.
 */

// Fixed internal contrast boost blended into the base fbm/worley mix.
// Replaces the old per-archetype "puffJitter" knob — see CloudData's own
// doc comment for why that field was retired rather than kept as a
// second detail-texture control alongside silhouetteSoftness.
const float DETAIL_CONTRAST = 0.5;

// Cheap two-axis domain warp — offsets a sample position using a second,
// decorrelated noise field scaled by `strength`. Only X/Z are warped —
// vertical warp buys little for a cloud whose vertical shape is already
// fully owned by heightGradient() below, and costs a full extra noise
// evaluation per sample. Uses gradient noise rather than value noise so
// the warp itself is smooth — a grainy warp field shows up directly as
// grain in the warped shape.
vec3 warpCloudDomain(vec3 p, float strength, vec3 seedOffset) {
    float wx = gradientNoise3D(p.yzx * 0.7 + seedOffset);
    float wz = gradientNoise3D(p.zxy * 0.7 + seedOffset + 11.3);
    return p + vec3(wx, 0.0, wz) * strength;
}

/*
* Vertical density gradient within this instance's own box. heightT is
 * [0, 1]: 0 at the box's own base, 1 at the top of its own vertical
 * extent. Real clouds condense at a fairly consistent altitude within a
 * given layer, giving them a comparatively sharp flat base, then erode
 * gradually into clear air above — never the reverse. coverageBias is the
 * same live-weather-derived value already driving the density threshold
 * below; a denser/stormier weather pushes the top-erosion point higher,
 * so a storm deck reads as thicker and taller than a fair-weather puff
 * without this function ever special-casing that difference.
 */
float heightGradient(float heightT, float coverageBias) {
    float baseCutoff = 0.06;
    float baseRamp = smoothstep(baseCutoff, baseCutoff + 0.14, heightT);

    float topStart = mix(0.50, 0.88, clamp(coverageBias, 0.0, 1.0));
    float topRamp = 1.0 - smoothstep(topStart, 1.0, heightT);

    return baseRamp * topRamp;
}

/*
* Horizontal silhouette taper — see the class comment's "Silhouette fix"
 * section for why this exists. Computes this sample's position relative
 * to the instance's own box center, normalized by the box's own XZ half-
 * extents so the taper scales correctly with CloudData.scale regardless
 * of archetype. The taper's start radius and its angular jitter strength
 * both derive from silhouetteSoftness — a single archetype knob rather
 * than a second dedicated jitter parameter, since "how soft/irregular
 * this cloud's edge reads" is one creative decision, not two.
 */
float silhouetteMask(vec3 worldPos, vec3 boxMin, vec3 boxMax, float silhouetteSoftness, float seed) {
    vec3 boxCenter = (boxMin + boxMax) * 0.5;
    vec2 halfExtentXZ = max((boxMax - boxMin).xz * 0.5, vec2(0.0001));
    vec2 localXZ = (worldPos.xz - boxCenter.xz) / halfExtentXZ;

    float angle = atan(localXZ.y, localXZ.x);
    float angularJitter = gradientNoise3D(vec3(cos(angle) * 2.0, sin(angle) * 2.0, seed * 41.7))
    * silhouetteSoftness * 1.5;

    float radial = length(localXZ) - angularJitter;
    float startRadius = clamp(1.0 - silhouetteSoftness * 2.2, 0.15, 0.95);

    return 1.0 - smoothstep(startRadius, 1.0, radial);
}

/*
* Resolves a single density sample in [0,1] at a world-scale position,
 * for a raymarch bounded to boxMin/boxMax. coverageBias shifts the noise
 * threshold separating "inside" from "empty sky" — higher bias means more
 * of the noise field reads as cloud, and this is exactly the live,
 * per-region weather coverage value the caller already has on hand, so
 * density genuinely tracks the simulation rather than a fixed archetype
 * look. silhouetteSoftness softens both that threshold crossing and the
 * box-relative taper from silhouetteMask() — see that function's own doc
 * comment. seed decorrelates this sample from every other cloud sampling
 * the same nominal position, and timeSeconds drives a slow, near-
 * imperceptible internal drift so a stationary cloud still isn't
 * perfectly static. heightT drives both the horizontal stretch and the
 * vertical heightGradient() falloff — see heightGradient()'s own doc
 * comment.
 */
float sampleVolumetricCloudDensity(
    vec3 worldPos,
    vec3 boxMin,
    vec3 boxMax,
    float heightT,
    float noiseScale,
    float warpStrength,
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

    vec3 warped = warpCloudDomain(coord, warpStrength, seedOffset);

    // Macro billow (gradient FBM) blended with a cellular detail layer for
    // the bumpy, cauliflower-like edge real clouds show — see
    // NoiseUtility.glsl's own doc comment.
    float macro = fbmGradient3D(warped);
    float bump = worleyFbm3D(warped * 3.2 + seedOffset.yzx);
    float n = clamp(mix(macro, bump, 0.35) + (macro - 0.5) * DETAIL_CONTRAST, 0.0, 1.0);

    float threshold = mix(0.74, 0.22, clamp(coverageBias, 0.0, 1.0));
    float shape = smoothstep(threshold - silhouetteSoftness, threshold + silhouetteSoftness, n);

    float edge = silhouetteMask(worldPos, boxMin, boxMax, silhouetteSoftness, seed);

    return shape * heightGradient(heightT, coverageBias) * edge;
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
* Ray/AABB slab intersection — bounds this instance's own raymarch to its
 * box. Returns (tNear, tFar) along rayDir from rayOrigin — tFar < tNear
 * means the ray misses the box entirely, which the caller (marching from
 * a point already known to be on the box's own surface, since GL only
 * rasterizes a convex box's outer faces) should never actually see, but is
 * left for the caller to guard against via a simple max(tFar - tNear, 0.0)
 * length check.
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

#endif