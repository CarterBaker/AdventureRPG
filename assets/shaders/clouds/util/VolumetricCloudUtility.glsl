#ifndef VOLUMETRIC_CLOUD_UTILITY_GLSL
#define VOLUMETRIC_CLOUD_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

/*
* Shared volumetric toon-cloud shape/shading, raymarched by both the
 * overhead cloud objects (CloudVolumeShader.fsh) and the sky dome's
 * distant weather preview (sky/util/Clouds.glsl) so the two layers can
 * never visually drift apart. Density sampling gates its expensive
 * cellular detail layer to samples actually near the cloud's own density
 * threshold — most of a raymarch through a cloud's bounding volume is
 * empty air well outside its silhouette.
 */

const float DETAIL_CONTRAST           = 0.5;
const float CLOUD_NOISE_BILLOW_CYCLES = 6.0;
const float DETAIL_GATE_MARGIN        = 0.12;

vec3 warpCloudDomain(vec3 p, float strength, vec3 seedOffset) {
    float wx = gradientNoise3D(p.yzx * 0.7 + seedOffset);
    float wz = gradientNoise3D(p.zxy * 0.7 + seedOffset + 11.3);
    return p + vec3(wx, 0.0, wz) * strength;
}

float heightGradient(float heightT, float coverageBias) {
    float baseCutoff = 0.06;
    float baseRamp = smoothstep(baseCutoff, baseCutoff + 0.14, heightT);
    float topStart = mix(0.50, 0.88, clamp(coverageBias, 0.0, 1.0));
    float topRamp = 1.0 - smoothstep(topStart, 1.0, heightT);
    return baseRamp * topRamp;
}

// Position within the shared rotated/height-stretched noise domain — the
// macro and detail layers both sample through this so they never drift
// apart. elongation (long axis / short axis) is derived from the box's
// own true half-extents rather than threaded as a separate parameter.
vec3 resolveNoiseCoord(
    vec3 worldPos,
    vec3 boxMin,
    vec3 boxMax,
    vec2 halfExtentXZ,
    float heightT,
    float noiseScale,
    vec2 rot,
    float seed,
    float timeSeconds,
    out vec3 seedOffset) {
    vec3 boxSize = max(boxMax - boxMin, vec3(0.0001));
    vec3 localPos = (worldPos - (boxMin + boxMax) * 0.5) / boxSize;

    vec2 rotatedXZ = vec2(
        localPos.x * rot.x - localPos.z * rot.y,
        localPos.x * rot.y + localPos.z * rot.x);

    float elongation = max(halfExtentXZ.x / max(halfExtentXZ.y, 0.0001), 1.0);
    float heightStretch = mix(1.0, 2.4, clamp(heightT, 0.0, 1.0));
    vec2 stretchedXZ = vec2(rotatedXZ.x / (heightStretch * elongation), rotatedXZ.y);

    seedOffset = vec3(seed * 173.13, seed * 57.31, seed * 91.7);
    vec3 stretchedPos = vec3(stretchedXZ.x, localPos.y, stretchedXZ.y);

    return stretchedPos * (noiseScale * CLOUD_NOISE_BILLOW_CYCLES)
    + seedOffset + vec3(0.0, 0.0, timeSeconds * 0.015);
}

// Elliptical silhouette taper — rotates into the box's own pre-rotation
// local space via the inverse of `rot` so an elongated instance reads as
// a genuine oriented oval rather than one flattened to its own AABB.
float silhouetteMask(vec3 worldPos, vec3 boxCenter, vec2 halfExtentXZ, vec2 rot, float silhouetteSoftness, float seed) {
    vec2 rel = worldPos.xz - boxCenter.xz;
    vec2 local = vec2(rel.x * rot.x + rel.y * rot.y, -rel.x * rot.y + rel.y * rot.x);
    vec2 localXZ = local / max(halfExtentXZ, vec2(0.0001));

    float angle = atan(localXZ.y, localXZ.x);
    float angularJitter = gradientNoise3D(vec3(cos(angle) * 2.0, sin(angle) * 2.0, seed * 41.7))
    * silhouetteSoftness * 1.5;

    float radial = length(localXZ) - angularJitter;
    float startRadius = clamp(1.0 - silhouetteSoftness * 2.2, 0.15, 0.95);

    return 1.0 - smoothstep(startRadius, 1.0, radial);
}

float sampleVolumetricCloudDensity(
    vec3 worldPos,
    vec3 boxMin,
    vec3 boxMax,
    vec2 halfExtentXZ,
    float heightT,
    float noiseScale,
    float warpStrength,
    vec2 rot,
    float coverageBias,
    float silhouetteSoftness,
    float seed,
    float timeSeconds) {
    vec3 seedOffset;
    vec3 coord = resolveNoiseCoord(worldPos, boxMin, boxMax, halfExtentXZ, heightT, noiseScale, rot, seed, timeSeconds, seedOffset);
    vec3 warped = warpCloudDomain(coord, warpStrength, seedOffset);

    float macro = fbmGradient3D(warped);
    float threshold = mix(0.74, 0.22, clamp(coverageBias, 0.0, 1.0));

    // Cellular detail (worleyFbm3D) is by far the most expensive part of
    // this function. A sample whose macro shape already sits well below
    // the density threshold can only ever end up as empty air once detail
    // is blended in, so skip it there entirely.
    float n = macro;
    if (macro > threshold - silhouetteSoftness - DETAIL_GATE_MARGIN) {
        float bump = worleyFbm3D(warped * 3.2 + seedOffset.yzx);
        n = clamp(mix(macro, bump, 0.35) + (macro - 0.5) * DETAIL_CONTRAST, 0.0, 1.0);
    }

    float shape = smoothstep(threshold - silhouetteSoftness, threshold + silhouetteSoftness, n);
    vec3 boxCenter = (boxMin + boxMax) * 0.5;
    float edge = silhouetteMask(worldPos, boxCenter, halfExtentXZ, rot, silhouetteSoftness, seed);

    return shape * heightGradient(heightT, coverageBias) * edge;
}

vec3 volumetricCloudGradientNormal(
    vec3 p,
    vec3 boxMin,
    vec3 boxMax,
    vec2 halfExtentXZ,
    float noiseScale,
    float warpStrength,
    vec2 rot,
    float coverageBias,
    float silhouetteSoftness,
    float seed,
    float timeSeconds,
    float epsilon) {
    float boxHeight = max(boxMax.y - boxMin.y, 0.0001);
    float heightTHere = clamp((p.y - boxMin.y) / boxHeight, 0.0, 1.0);
    float heightTUp = clamp(((p.y + epsilon) - boxMin.y) / boxHeight, 0.0, 1.0);
    float heightTDown = clamp(((p.y - epsilon) - boxMin.y) / boxHeight, 0.0, 1.0);

    float dx = sampleVolumetricCloudDensity(p + vec3(epsilon, 0.0, 0.0), boxMin, boxMax, halfExtentXZ, heightTHere, noiseScale, warpStrength, rot, coverageBias, silhouetteSoftness, seed, timeSeconds)
    - sampleVolumetricCloudDensity(p - vec3(epsilon, 0.0, 0.0), boxMin, boxMax, halfExtentXZ, heightTHere, noiseScale, warpStrength, rot, coverageBias, silhouetteSoftness, seed, timeSeconds);

    float dy = sampleVolumetricCloudDensity(p + vec3(0.0, epsilon, 0.0), boxMin, boxMax, halfExtentXZ, heightTUp, noiseScale, warpStrength, rot, coverageBias, silhouetteSoftness, seed, timeSeconds)
    - sampleVolumetricCloudDensity(p - vec3(0.0, epsilon, 0.0), boxMin, boxMax, halfExtentXZ, heightTDown, noiseScale, warpStrength, rot, coverageBias, silhouetteSoftness, seed, timeSeconds);

    float dz = sampleVolumetricCloudDensity(p + vec3(0.0, 0.0, epsilon), boxMin, boxMax, halfExtentXZ, heightTHere, noiseScale, warpStrength, rot, coverageBias, silhouetteSoftness, seed, timeSeconds)
    - sampleVolumetricCloudDensity(p - vec3(0.0, 0.0, epsilon), boxMin, boxMax, halfExtentXZ, heightTHere, noiseScale, warpStrength, rot, coverageBias, silhouetteSoftness, seed, timeSeconds);

    vec3 gradient = vec3(dx, dy, dz);
    float gradLen = length(gradient);

    if (gradLen < 0.0001)
    return vec3(0.0, 1.0, 0.0);

    return -gradient / gradLen;
}

/*
* Unlit shape-only shading for a deferred G-buffer writer (overhead cloud
 * objects) — Lighting.fsh applies real sun/moon lighting afterward.
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
* Direct-lit shading for a forward pass (the sky dome). Bakes real
 * sun/moon color and intensity, plus a rim term — must never be used by a
 * deferred G-buffer writer, since Lighting.fsh would then light it twice.
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

    float rim = (1.0 - clamp(density * 2.0, 0.0, 1.0)) * rimLightStrength;
    shaded += lightColor * lightIntensity * rim;

    shaded *= mix(vec3(1.0), lightColor, 0.35) * max(lightIntensity, 0.15);

    return shaded * brightnessMultiplier;
}

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