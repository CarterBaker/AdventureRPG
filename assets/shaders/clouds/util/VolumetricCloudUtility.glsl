// VolumetricCloudUtility.glsl — clouds/util/VolumetricCloudUtility.glsl
#ifndef VOLUMETRIC_CLOUD_UTILITY_GLSL
#define VOLUMETRIC_CLOUD_UTILITY_GLSL

#include "includes/NoiseUtility.glsl"

/*
* Shape, density, and shading primitives for the overhead volumetric cloud
 * boxes. Every instance raymarches its own oriented box treated as an
 * inscribed ellipsoid, so silhouette, density, and lighting all agree in
 * the same local frame regardless of world position, rotation, or
 * elongation.
 */

const float CLOUD_NOISE_WORLD_SCALE = 1.0 / 26.0;
const float CLOUD_DETAIL_FREQUENCY  = 4.4;
const float CLOUD_EROSION_STRENGTH  = 0.55;
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

float remap(float value, float low1, float high1, float low2, float high2) {
    return low2 + (value - low1) * (high2 - low2) / max(high1 - low1, 0.0001);
}

float cloudHeightEnvelope(float heightT, float coverageBias) {
    float base = smoothstep(0.0, 0.22, heightT);
    float topStart = mix(0.45, 0.9, clamp(coverageBias, 0.0, 1.0));
    float top = 1.0 - smoothstep(topStart, 1.0, heightT);
    return base * top;
}

// Density \\

// worldPos/boxCenter/rot/halfExtent describe the instance's own oriented
// box. heightT is 0 at the box floor and 1 at the box ceiling. Returns 0
// outside the ellipsoid inscribed in that box.
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
    float seed,
    float timeSeconds) {
    vec3 rel = worldPos - boxCenter;
    vec2 localXZ = rotateWorldToLocal(rel.xz, rot);
    vec3 localPos = vec3(localXZ.x, rel.y, localXZ.y);

    vec3 normalizedPos = localPos / max(halfExtent, vec3(0.0001));
    float radius = length(normalizedPos);

    if (radius >= 1.0)
    return 0.0;

    float softness = clamp(silhouetteSoftness, CLOUD_MIN_SOFTNESS, 0.7);
    float envelope = (1.0 - smoothstep(1.0 - softness, 1.0, radius)) * cloudHeightEnvelope(heightT, coverageBias);

    if (envelope <= 0.002)
    return 0.0;

    // Noise is sampled at a fixed physical frequency regardless of how the
    // box has been stretched — dividing the elongated axis back out keeps
    // cloud detail reading as the same real-world size at any elongation.
    float elongation = max(halfExtent.x / max(halfExtent.z, 0.0001), 1.0);
    vec3 seedOffset = vec3(seed * 173.13, seed * 57.31, seed * 91.7);
    vec3 physicalPos = vec3(localXZ.x / elongation, rel.y, localXZ.y);
    vec3 coord = physicalPos * (noiseScale * CLOUD_NOISE_WORLD_SCALE)
    + seedOffset + vec3(0.0, 0.0, timeSeconds * 0.015);

    float warpA = gradientNoise3D(coord.yzx * 0.55 + seedOffset);
    float warpB = gradientNoise3D(coord.zxy * 0.55 + seedOffset + 19.1);
    vec3 warped = coord + vec3(warpA, warpA * 0.4, warpB) * warpStrength;

    float macro = fbmGradient3D(warped);
    float coverage = clamp(1.0 - coverageBias, 0.05, 0.95);
    float base = clamp(remap(macro, coverage, 1.0, 0.0, 1.0), 0.0, 1.0) * envelope;

    if (base <= 0.01)
    return 0.0;
    if (base > 0.98)
    return base;

    float bump = worleyFbm3D(warped * CLOUD_DETAIL_FREQUENCY + seedOffset.yzx);
    float erosionWeight = smoothstep(0.0, 0.7, 1.0 - base) * smoothstep(0.0, 0.3, base);
    float erosion = mix(1.0, bump, CLOUD_EROSION_STRENGTH * erosionWeight);

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
    float seed,
    float timeSeconds,
    float epsilon) {
    float boxHeight = max(halfExtent.y * 2.0, 0.0001);
    float baseY = boxCenter.y - halfExtent.y;

    float heightHere = clamp((p.y - baseY) / boxHeight, 0.0, 1.0);
    float heightUp    = clamp(((p.y + epsilon) - baseY) / boxHeight, 0.0, 1.0);
    float heightDown  = clamp(((p.y - epsilon) - baseY) / boxHeight, 0.0, 1.0);

    float dx = sampleCloudDensity(p + vec3(epsilon, 0.0, 0.0), boxCenter, rot, halfExtent, heightHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds)
    - sampleCloudDensity(p - vec3(epsilon, 0.0, 0.0), boxCenter, rot, halfExtent, heightHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds);

    float dy = sampleCloudDensity(p + vec3(0.0, epsilon, 0.0), boxCenter, rot, halfExtent, heightUp, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds)
    - sampleCloudDensity(p - vec3(0.0, epsilon, 0.0), boxCenter, rot, halfExtent, heightDown, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds);

    float dz = sampleCloudDensity(p + vec3(0.0, 0.0, epsilon), boxCenter, rot, halfExtent, heightHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds)
    - sampleCloudDensity(p - vec3(0.0, 0.0, epsilon), boxCenter, rot, halfExtent, heightHere, noiseScale, warpStrength, coverageBias, silhouetteSoftness, seed, timeSeconds);

    vec3 gradient = vec3(dx, dy, dz);
    float gradientLength = length(gradient);

    if (gradientLength < 0.0001)
    return vec3(0.0, 1.0, 0.0);

    return -gradient / gradientLength;
}

// Shading \\

float toonBand(float t, float bands) {
    float scaled = clamp(t, 0.0, 1.0) * bands;
    float index = floor(scaled);
    float frac = scaled - index;
    float edge = smoothstep(0.35, 0.65, frac);
    return (index + edge) / max(bands - 1.0, 1.0);
}

vec3 shadeCloudSample(
    vec3 baseColor,
    vec3 topColor,
    vec3 shadowColor,
    vec3 ambientColor,
    float heightT,
    float lightLift,
    float density,
    int toonBands,
    float shadeStrength,
    float ambientOcclusionStrength,
    float brightnessMultiplier,
    out float outAO) {
    float litAmount = clamp(heightT * 0.55 + lightLift * 0.45, 0.0, 1.0);
    float banded = toonBand(litAmount, max(float(toonBands), 2.0));

    vec3 lit = mix(baseColor, topColor, banded);
    vec3 shaded = mix(lit, shadowColor, (1.0 - banded) * shadeStrength);

    float ao = mix(1.0 - ambientOcclusionStrength, 1.0, banded);
    shaded *= ao;
    shaded = mix(mix(shadowColor, ambientColor, 0.4), shaded, clamp(density * 2.2, 0.0, 1.0));

    outAO = ao;
    return shaded * brightnessMultiplier;
}

#endif