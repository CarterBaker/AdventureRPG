#ifndef VOLUMETRIC_CLOUD_UTILITY_GLSL
#define VOLUMETRIC_CLOUD_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

/*
* Procedural volumetric toon-cloud primitives for physical cloud objects
 * (clouds/CloudVolumeShader.fsh's per-instance raymarched box). Deliberately
 * independent from the sky dome's own copy (sky/util/SkyCloudUtility.glsl) —
 * the two raymarch different geometry (a bounded per-instance AABB here vs.
 * an unbounded view-ray layer there) and only share NoiseUtility.glsl's
 * primitives and the CPU-side weather/cloud data feeding them.
 *
 * Density shaping — silhouetteMask() tapers the raymarch to zero before the
 * true AABB boundary so a cloud reads as a rounded puff rather than its own
 * bounding box; sampleVolumetricCloudDensity() normalizes worldPos against
 * the instance's own box before sampling so noise frequency scales correctly
 * regardless of archetype size, and stretches only a per-instance rotated
 * axis (domainRotation) as height increases so tops read as directional
 * streaks rather than a uniformly-coarser blob.
 */

const float DETAIL_CONTRAST = 0.5;
const float CLOUD_NOISE_BILLOW_CYCLES = 6.0;

// Two-axis (X/Z) domain warp using a second decorrelated noise field.
vec3 warpCloudDomain(vec3 p, float strength, vec3 seedOffset) {
    float wx = gradientNoise3D(p.yzx * 0.7 + seedOffset);
    float wz = gradientNoise3D(p.zxy * 0.7 + seedOffset + 11.3);
    return p + vec3(wx, 0.0, wz) * strength;
}

// Vertical density falloff within the instance's own box — sharp flat base,
// soft eroding top. coverageBias raises the top-erosion point for denser
// weather so a storm reads taller/thicker than a fair-weather puff.
float heightGradient(float heightT, float coverageBias) {
    float baseCutoff = 0.06;
    float baseRamp = smoothstep(baseCutoff, baseCutoff + 0.14, heightT);
    float topStart = mix(0.50, 0.88, clamp(coverageBias, 0.0, 1.0));
    float topRamp = 1.0 - smoothstep(topStart, 1.0, heightT);
    return baseRamp * topRamp;
}

// Horizontal silhouette taper, normalized against the box's own XZ extents,
// with a jittered (non-circular) radius so the outline reads as an
// irregular puff rather than the box itself.
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

// Resolves density in [0,1] at a world-scale position for a raymarch bounded
// to boxMin/boxMax. domainRotation is a stable per-instance angle (baked at
// stream-in — see OverheadCellStruct) that elongates one rotated axis with
// height, so every instance streaks in its own direction.
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
    float timeSeconds,
    float domainRotation) {
    vec3 boxSize = max(boxMax - boxMin, vec3(0.0001));
    vec3 localPos = (worldPos - (boxMin + boxMax) * 0.5) / boxSize;

    float rotCos = cos(domainRotation);
    float rotSin = sin(domainRotation);
    vec2 rotatedXZ = vec2(
        localPos.x * rotCos - localPos.z * rotSin,
        localPos.x * rotSin + localPos.z * rotCos);

    float stretch = mix(1.0, 2.4, clamp(heightT, 0.0, 1.0));
    vec2 stretchedXZ = vec2(rotatedXZ.x / stretch, rotatedXZ.y);

    vec3 seedOffset = vec3(seed * 173.13, seed * 57.31, seed * 91.7);
    vec3 stretchedPos = vec3(stretchedXZ.x, localPos.y, stretchedXZ.y);
    vec3 coord = stretchedPos * (noiseScale * CLOUD_NOISE_BILLOW_CYCLES)
    + seedOffset + vec3(0.0, 0.0, timeSeconds * 0.015);

    vec3 warped = warpCloudDomain(coord, warpStrength, seedOffset);

    float macro = fbmGradient3D(warped);
    float bump = worleyFbm3D(warped * 3.2 + seedOffset.yzx);
    float n = clamp(mix(macro, bump, 0.35) + (macro - 0.5) * DETAIL_CONTRAST, 0.0, 1.0);

    float threshold = mix(0.74, 0.22, clamp(coverageBias, 0.0, 1.0));
    float shape = smoothstep(threshold - silhouetteSoftness, threshold + silhouetteSoftness, n);

    float edge = silhouetteMask(worldPos, boxMin, boxMax, silhouetteSoftness, seed);

    return shape * heightGradient(heightT, coverageBias) * edge;
}

/*
* Surface normal for lighting, derived from the density field's own
 * gradient rather than the instance's flat box geometry. Six extra density
 * taps (central differences per axis) — called once per fragment at the
 * raymarch's single most-visible sample (see CloudVolumeShader.fsh), never
 * once per step. This is what makes a cloud shade like a cloud: handing the
 * box's own flat face normal to the deferred lighting pass made every face
 * light as one uniform flat panel — six visibly distinct, hard-edged
 * rectangles instead of a rounded volume.
 */
vec3 volumetricCloudGradientNormal(
    vec3 p,
    vec3 boxMin,
    vec3 boxMax,
    float noiseScale,
    float warpStrength,
    float coverageBias,
    float silhouetteSoftness,
    float seed,
    float timeSeconds,
    float domainRotation,
    float epsilon) {
    float boxHeight = max(boxMax.y - boxMin.y, 0.0001);
    float heightTHere = clamp((p.y - boxMin.y) / boxHeight, 0.0, 1.0);
    float heightTUp = clamp(((p.y + epsilon) - boxMin.y) / boxHeight, 0.0, 1.0);
    float heightTDown = clamp(((p.y - epsilon) - boxMin.y) / boxHeight, 0.0, 1.0);

    float dx = sampleVolumetricCloudDensity(p + vec3(epsilon, 0.0, 0.0), boxMin, boxMax, heightTHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds, domainRotation)
    - sampleVolumetricCloudDensity(p - vec3(epsilon, 0.0, 0.0), boxMin, boxMax, heightTHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds, domainRotation);

    float dy = sampleVolumetricCloudDensity(p + vec3(0.0, epsilon, 0.0), boxMin, boxMax, heightTUp, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds, domainRotation)
    - sampleVolumetricCloudDensity(p - vec3(0.0, epsilon, 0.0), boxMin, boxMax, heightTDown, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds, domainRotation);

    float dz = sampleVolumetricCloudDensity(p + vec3(0.0, 0.0, epsilon), boxMin, boxMax, heightTHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds, domainRotation)
    - sampleVolumetricCloudDensity(p - vec3(0.0, 0.0, epsilon), boxMin, boxMax, heightTHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds, domainRotation);

    vec3 gradient = vec3(dx, dy, dz);
    float gradLen = length(gradient);

    if (gradLen < 0.0001)
    return vec3(0.0, 1.0, 0.0);

    return -gradient / gradLen;
}

/*
* Unlit (shape-only) toon shading for a physical cloud sample. Writes into
 * the same deferred G-buffer terrain uses (gAlbedo/gNormal/gMaterial) — the
 * shared Lighting.fsh pass applies real sun/moon/ambient lighting to it
 * exactly once, using gNormal for the directional terms and gMaterial.b for
 * ambient occlusion. Must never bake a light's own color/intensity into its
 * result, or the pixel gets lit twice.
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

// Ray/AABB slab intersection. Returns (tNear, tFar) along rayDir from
// rayOrigin — tFar < tNear means the ray misses the box entirely.
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