// VolumetricCloudUtility.glsl — clouds/util/VolumetricCloudUtility.glsl
#ifndef VOLUMETRIC_CLOUD_UTILITY_GLSL
#define VOLUMETRIC_CLOUD_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

/*
* Shape and density primitives for the overhead volumetric cloud boxes.
 * Every instance raymarches its own oriented box, treated as an inscribed
 * ellipsoid so silhouette, density, and normals all agree in the same
 * local frame regardless of world position, rotation, or elongation.
 * Lighting is intentionally NOT computed here — see CloudVolumeShader.fsh
 * for why directional sun/moon response is left to the shared deferred
 * pass instead of being baked in twice.
 */

const float CLOUD_NOISE_WORLD_SCALE = 1.0 / 26.0;
const float CLOUD_DETAIL_FREQUENCY  = 3.2;
const float CLOUD_EROSION_STRENGTH  = 0.6;
const float CLOUD_MIN_SOFTNESS      = 0.12;

// Rotation \\

vec2 rotateWorldToLocal(vec2 relXZ, vec2 rot) {
    return vec2(relXZ.x * rot.x + relXZ.y * rot.y, -relXZ.x * rot.y + relXZ.y * rot.x);
}

// Intersection \\

vec2 intersectCloudBox(vec3 rayOrigin, vec3 rayDir, vec3 boxCenter, vec2 rot, vec3 halfExtent) {
    vec3 rel = rayOrigin - boxCenter;
    vec2 localOriginXZ = rotateWorldToLocal(rel.xz, rot);
    vec2 localDirXZ = rotateWorldToLocal(rayDir.xz, rot);

    vec3 localOrigin = vec3(localOriginXZ.x, rel.y, localOriginXZ.y);
    vec3 localDir = vec3(localDirXZ.x, rayDir.y, localDirXZ.y);

    vec3 invDir = 1.0 / localDir;
    vec3 t0 = (-halfExtent - localOrigin) * invDir;
    vec3 t1 = (halfExtent - localOrigin) * invDir;
    vec3 tMin = min(t0, t1);
    vec3 tMax = max(t0, t1);

    return vec2(max(max(tMin.x, tMin.y), tMin.z), min(min(tMax.x, tMax.y), tMax.z));
}

// Shape \\

float cloudHeightEnvelope(float heightT, float coverageBias) {
    float base = smoothstep(0.0, 0.16, heightT);
    float topStart = mix(0.35, 0.92, clamp(coverageBias, 0.0, 1.0));
    float top = 1.0 - smoothstep(topStart, 1.0, heightT);
    return base * top;
}

// Density \\

/*
* worldPos/boxCenter/rot/halfExtent describe the instance's own oriented
 * box. heightT is 0 at the box floor and 1 at the box ceiling. Returns 0
 * outside the ellipsoid inscribed in that box. detailFactor fades the
 * highest-frequency erosion detail out with distance so a raymarch step
 * that now covers many world units per pixel never aliases into visible
 * shimmer — the silhouette itself never changes, only how much cauliflower
 * texture rides on top of it.
 */
float sampleCloudDensity(
    vec3 worldPos,
    vec3 boxCenter,
    vec2 rot,
    vec3 halfExtent,
    float heightT,
    float noiseScale,
    float warpStrength,
    float coverageBias,
    float silhouetteSoftness,
    float detailFactor,
    float seed,
    float timeSeconds) {
    vec3 rel = worldPos - boxCenter;
    vec2 localXZ = rotateWorldToLocal(rel.xz, rot);
    vec3 localPos = vec3(localXZ.x, rel.y, localXZ.y);

    vec3 normalizedPos = localPos / max(halfExtent, vec3(0.0001));
    float radius = length(normalizedPos);

    if (radius >= 1.0)
    return 0.0;

    float softness = clamp(silhouetteSoftness, CLOUD_MIN_SOFTNESS, 0.85);
    float silhouette = 1.0 - smoothstep(1.0 - softness, 1.0, radius);
    float envelope = silhouette * cloudHeightEnvelope(heightT, coverageBias);

    if (envelope <= 0.002)
    return 0.0;

    float elongation = max(halfExtent.x / max(halfExtent.z, 0.0001), 1.0);
    vec3 seedOffset = vec3(seed * 173.13, seed * 57.31, seed * 91.7);
    vec3 drift = vec3(timeSeconds * 0.006, 0.0, timeSeconds * 0.004);
    vec3 physicalPos = vec3(localXZ.x / elongation, rel.y, localXZ.y);
    vec3 coord = physicalPos * (noiseScale * CLOUD_NOISE_WORLD_SCALE) + seedOffset + drift;

    float warpA = gradientNoise3D(coord.yzx * 0.7 + seedOffset);
    float warpB = gradientNoise3D(coord.zxy * 0.7 + seedOffset - 11.3);
    vec3 warped = coord + vec3(warpA, warpA * 0.3, warpB) * warpStrength;

    float macro = perlinWorley3D(warped, 2.2);
    float coverage = clamp(1.0 - coverageBias, 0.05, 0.95);
    float shaped = clamp(remapClamped(macro, coverage, 1.0, 0.0, 1.0), 0.0, 1.0);
    float base = shaped * envelope;

    if (base <= 0.01)
    return 0.0;

    float detail = worleyFbm3D(warped * CLOUD_DETAIL_FREQUENCY + seedOffset.yzx, 2, 2.3, 0.5);
    float edgeWeight = smoothstep(0.0, 0.55, 1.0 - base) * smoothstep(0.0, 0.35, base);
    float erosionStrength = CLOUD_EROSION_STRENGTH * mix(0.25, 1.0, clamp(detailFactor, 0.0, 1.0));
    float erosion = mix(1.0, detail, erosionStrength * edgeWeight);

    return clamp(base * erosion, 0.0, 1.0);
}

// Normal \\

vec3 sampleCloudGradientNormal(
    vec3 p,
    vec3 boxCenter,
    vec2 rot,
    vec3 halfExtent,
    float noiseScale,
    float warpStrength,
    float coverageBias,
    float silhouetteSoftness,
    float detailFactor,
    float seed,
    float timeSeconds,
    float epsilon) {
    float boxHeight = max(halfExtent.y * 2.0, 0.0001);
    float baseY = boxCenter.y - halfExtent.y;

    float heightHere = clamp((p.y - baseY) / boxHeight, 0.0, 1.0);
    float heightUp    = clamp(((p.y + epsilon) - baseY) / boxHeight, 0.0, 1.0);
    float heightDown  = clamp(((p.y - epsilon) - baseY) / boxHeight, 0.0, 1.0);

    float dx = sampleCloudDensity(p + vec3(epsilon, 0.0, 0.0), boxCenter, rot, halfExtent, heightHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, detailFactor, seed, timeSeconds)
    - sampleCloudDensity(p - vec3(epsilon, 0.0, 0.0), boxCenter, rot, halfExtent, heightHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, detailFactor, seed, timeSeconds);

    float dy = sampleCloudDensity(p + vec3(0.0, epsilon, 0.0), boxCenter, rot, halfExtent, heightUp, noiseScale, warpStrength, coverageBias, silhouetteSoftness, detailFactor, seed, timeSeconds)
    - sampleCloudDensity(p - vec3(0.0, epsilon, 0.0), boxCenter, rot, halfExtent, heightDown, noiseScale, warpStrength, coverageBias, silhouetteSoftness, detailFactor, seed, timeSeconds);

    float dz = sampleCloudDensity(p + vec3(0.0, 0.0, epsilon), boxCenter, rot, halfExtent, heightHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, detailFactor, seed, timeSeconds)
    - sampleCloudDensity(p - vec3(0.0, 0.0, epsilon), boxCenter, rot, halfExtent, heightHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, detailFactor, seed, timeSeconds);

    vec3 gradient = vec3(dx, dy, dz);
    float gradientLength = length(gradient);

    if (gradientLength < 0.0001)
    return vec3(0.0, 1.0, 0.0);

    return -gradient / gradientLength;
}

#endif